package com.songshanhu.blog.controller;

import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@Tag(name = "上传与转存", description = "图片外链转存、静态访问")
public class UploadController {

    @Value("${upload.image-dir:./uploads/images/}")
    private String imageDir;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Operation(summary = "外链图片转存（校验可访问与类型，返回可访问路径）")
    @PostMapping("/fetch-image")
    public Result<Map<String, String>> fetchImage(@RequestParam("url") String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "ssl-blog-image-fetch/1.0");
            int code = conn.getResponseCode();
            if (code != 200) {
                return Result.error("外链不可访问，状态码: " + code);
            }
            String ct = conn.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                return Result.error("外链不是图片类型");
            }
            String ext = "jpg";
            if (ct.contains("png")) ext = "png";
            else if (ct.contains("webp")) ext = "webp";
            else if (ct.contains("jpeg") || ct.contains("jpg")) ext = "jpg";

            byte[] data;
            try (InputStream in = conn.getInputStream()) {
                data = in.readAllBytes();
            }
            String md5 = DigestUtils.md5DigestAsHex(data);
            File dir = new File(imageDir);
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, md5 + "." + ext);
            if (!out.exists()) {
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(data);
                }
            }
            String filename = out.getName();
            return Result.success(Map.of(
                    "url", contextPath + "/upload/image/" + filename,
                    "md5", md5,
                    "filename", filename
            ));
        } catch (Exception e) {
            return Result.error("转存失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取转存图片")
    @GetMapping("/image/{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
        try {
            if (filename.contains("..")) return ResponseEntity.badRequest().build();
            File file = new File(imageDir, filename);
            if (!file.exists()) return ResponseEntity.notFound().build();
            byte[] bytes = Files.readAllBytes(file.toPath());
            String ext = FilenameUtils.getExtension(filename).toLowerCase();
            String ct = switch (ext) {
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                default -> "image/jpeg";
            };
            HttpHeaders h = new HttpHeaders();
            h.add(HttpHeaders.CONTENT_TYPE, ct);
            h.add("Cache-Control", "public, max-age=86400");
            return ResponseEntity.ok().headers(h).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "图片代理（仅允许 images.unsplash.com）")
    @GetMapping("/proxy-image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String urlStr) {
        try {
            String decoded = URLDecoder.decode(urlStr, StandardCharsets.UTF_8);
            URL url = new URL(decoded);
            String protocol = url.getProtocol();
            String host = url.getHost();
            if (!"https".equalsIgnoreCase(protocol) || !"images.unsplash.com".equalsIgnoreCase(host)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Referer", "https://unsplash.com/");

            int code = conn.getResponseCode();
            if (code != 200) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
            }
            String ct = conn.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
            }
            int len = conn.getContentLength();
            if (len > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }

            byte[] data;
            try (InputStream in = conn.getInputStream()) {
                data = in.readAllBytes();
            }
            if (data.length > 5 * 1024 * 1024) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(ct));
            headers.add("Cache-Control", "public, max-age=86400");
            return ResponseEntity.ok().headers(headers).body(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @Operation(summary = "本地上传图片（jpg/png/webp，≤2MB）")
    @PostMapping(value = "/image", consumes = {"multipart/form-data"})
    public Result<Map<String, String>> uploadImage(@RequestPart("file") MultipartFile file) {
        try {
            if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024) {
                return Result.error("文件为空或超过 2MB");
            }
            String ct = file.getContentType();
            if (ct == null || !ct.startsWith("image/")) {
                return Result.error("仅支持图片类型");
            }
            byte[] data = file.getBytes();
            String md5 = DigestUtils.md5DigestAsHex(data);
            String ext = "jpg";
            if (ct.contains("png")) ext = "png";
            else if (ct.contains("webp")) ext = "webp";
            File dir = new File(imageDir);
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, md5 + "." + ext);
            if (!out.exists()) {
                try (FileOutputStream fos = new FileOutputStream(out)) {
                    fos.write(data);
                }
            }
            return Result.success(Map.of(
                    "url", contextPath + "/upload/image/" + out.getName(),
                    "md5", md5,
                    "filename", out.getName()
            ));
        } catch (Exception e) {
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}
