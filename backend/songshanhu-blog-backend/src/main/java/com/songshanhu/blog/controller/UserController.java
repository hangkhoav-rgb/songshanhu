package com.songshanhu.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.songshanhu.blog.dto.request.ProfileUpdateRequest;
import com.songshanhu.blog.dto.request.ChangePasswordRequest;
import com.songshanhu.blog.dto.response.UserProfileResponse;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.service.IUserService;
import com.songshanhu.blog.util.ContentModerator;
import com.songshanhu.blog.util.IdempotencyService;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@Tag(name = "用户资料", description = "个人资料维护与头像上传")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final ContentModerator contentModerator;
    private final IdempotencyService idempotencyService;
    private final PasswordEncoder passwordEncoder;

    @Value("${upload.avatar-dir:./uploads/avatars/}")
    private String avatarDir;

    @Value("${upload.avatar-signing-secret:${jwt.secret}}")
    private String avatarSigningSecret;

    @Value("${upload.avatar-url-exp-seconds:3600}")
    private long avatarUrlExpSeconds;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    @GetMapping("/profile")
    @Operation(summary = "获取当前用户资料")
    public Result<UserProfileResponse> getProfile() {
        User user = getCurrentUser();
        UserProfileResponse resp = toResp(user);
        return Result.success(resp);
    }

    @PutMapping("/profile")
    @Operation(summary = "更新当前用户资料（昵称唯一+敏感词校验）")
    public Result<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest req,
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        if (!idempotencyService.tryAcquire(idemKey, Duration.ofSeconds(10))) {
            return Result.error(429, "重复提交，请稍后重试");
        }

        User user = getCurrentUser();

        if (StringUtils.hasText(req.getNickname())) {
            // 敏感词过滤
            if (!contentModerator.isCompliant(req.getNickname())) {
                return Result.error("昵称含有敏感词");
            }
            // 唯一性校验
            long cnt = userService.count(new LambdaQueryWrapper<User>()
                    .eq(User::getNickname, req.getNickname())
                    .ne(User::getId, user.getId()));
            if (cnt > 0) {
                return Result.error("昵称已被占用");
            }
            user.setNickname(req.getNickname());
        }
        if (StringUtils.hasText(req.getGender())) {
            user.setGender(req.getGender());
        }
        if (StringUtils.hasText(req.getBio())) {
            user.setBio(req.getBio());
        }
        if (StringUtils.hasText(req.getExtra())) {
            user.setExtra(req.getExtra());
        }

        userService.updateById(user);
        return Result.success(null);
    }

    @PutMapping("/password")
    @Operation(summary = "修改密码（需要旧密码校验）")
    public Result<?> changePassword(@RequestBody ChangePasswordRequest req) {
        if (!StringUtils.hasText(req.getOldPassword()) || !StringUtils.hasText(req.getNewPassword())) {
            return Result.error(400, "旧密码和新密码不能为空");
        }
        if (req.getNewPassword().length() < 6) {
            return Result.error(400, "新密码长度至少 6 位");
        }
        User user = getCurrentUser();
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            return Result.error(401, "旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userService.updateById(user);
        return Result.success(null);
    }

    @PostMapping(value = "/avatar", consumes = {"multipart/form-data"})
    @Operation(summary = "上传头像（限制类型/大小，多分辨率存储，返回签名 URL）")
    public Result<Map<String, String>> uploadAvatar(@RequestPart("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return Result.error("不支持的图片类型");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            return Result.error("文件大小超过 2MB 限制");
        }

        User user = getCurrentUser();
        String baseKey = user.getId() + "_" + UUID.randomUUID().toString().replace("-", "");
        File dir = new File(avatarDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        BufferedImage src;
        try (InputStream in = file.getInputStream()) {
            src = ImageIO.read(in);
        }
        if (src == null) {
            return Result.error("图片解析失败");
        }

        BufferedImage square = centerCropSquare(src);

        File origin = new File(dir, baseKey + "_orig.jpg");
        writeJpeg(square, origin, 0.9f);

        File sm = new File(dir, baseKey + "_64.jpg");
        File md = new File(dir, baseKey + "_128.jpg");
        File lg = new File(dir, baseKey + "_256.jpg");
        writeJpeg(resize(square, 64, 64), sm, 0.85f);
        writeJpeg(resize(square, 128, 128), md, 0.85f);
        writeJpeg(resize(square, 256, 256), lg, 0.85f);

        Map<String, String> map = new HashMap<>();
        map.put("orig", signUrl("/user/avatar/" + urlEncode(origin.getName()), avatarUrlExpSeconds));
        map.put("sm", signUrl("/user/avatar/" + urlEncode(sm.getName()), avatarUrlExpSeconds));
        map.put("md", signUrl("/user/avatar/" + urlEncode(md.getName()), avatarUrlExpSeconds));
        map.put("lg", signUrl("/user/avatar/" + urlEncode(lg.getName()), avatarUrlExpSeconds));

        user.setAvatar(baseKey);
        userService.updateById(user);

        return Result.success(map);
    }

    @GetMapping("/avatar/{filename}")
    @Operation(summary = "获取签名头像（服务端签名校验+文件流）")
    public org.springframework.http.ResponseEntity<byte[]> getAvatar(
            @PathVariable String filename,
            @RequestParam("exp") long exp,
            @RequestParam("sig") String sig) throws Exception {
        if (!isSafeFilename(filename)) {
            return org.springframework.http.ResponseEntity.status(400).build();
        }
        String path = "/user/avatar/" + urlEncode(filename);
        if (!verifySignature(path, exp, sig)) {
            return org.springframework.http.ResponseEntity.status(403).build();
        }
        File file = new File(avatarDir, filename);
        if (!file.exists()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, contentTypeByName(filename));
        headers.add("Cache-Control", "public, max-age=3600");
        return org.springframework.http.ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String contentTypeByName(String name) {
        return "image/jpeg";
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        return userService.findByUsername(username);
    }

    private UserProfileResponse toResp(User user) {
        UserProfileResponse resp = new UserProfileResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        resp.setGender(user.getGender());
        resp.setBio(user.getBio());
        resp.setExtra(user.getExtra());
        if (StringUtils.hasText(user.getAvatar())) {
            Map<String, String> m = new HashMap<>();
            String key = user.getAvatar();
            m.put("sm", signUrl("/user/avatar/" + urlEncode(key + "_64.jpg"), avatarUrlExpSeconds));
            m.put("md", signUrl("/user/avatar/" + urlEncode(key + "_128.jpg"), avatarUrlExpSeconds));
            m.put("lg", signUrl("/user/avatar/" + urlEncode(key + "_256.jpg"), avatarUrlExpSeconds));
            m.put("orig", signUrl("/user/avatar/" + urlEncode(key + "_orig.jpg"), avatarUrlExpSeconds));
            resp.setAvatarUrls(m);
        }
        return resp;
    }

    private String signUrl(String path, long ttlSeconds) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = path + "|" + exp;
        String sig = hmacSha256Hex(avatarSigningSecret, payload);
        return contextPath + path + "?exp=" + exp + "&sig=" + sig;
    }

    private boolean verifySignature(String path, long exp, String sig) {
        if (Instant.now().getEpochSecond() > exp) {
            return false;
        }
        String payload = path + "|" + exp;
        String expect = hmacSha256Hex(avatarSigningSecret, payload);
        return constantTimeEquals(expect, sig);
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    private boolean isSafeFilename(String name) {
        return name != null && !name.contains("..") && !name.contains("/") && !name.contains("\\");
    }

    private String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private BufferedImage centerCropSquare(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2;
        int y = (h - side) / 2;
        BufferedImage cropped = src.getSubimage(x, y, side, side);
        BufferedImage out = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(cropped, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage resize(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private void writeJpeg(BufferedImage img, File out, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }
}
