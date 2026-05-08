package com.songshanhu.blog.controller;

import com.songshanhu.blog.dto.request.LoginRequest;
import com.songshanhu.blog.dto.request.RegisterRequest;
import com.songshanhu.blog.dto.request.FirstResetPasswordRequest;
import com.songshanhu.blog.dto.response.AuthResponse;
import com.songshanhu.blog.entity.BizUserPunishLog;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.mapper.BizUserPunishLogMapper;
import com.songshanhu.blog.service.IUserService;
import com.songshanhu.blog.util.AvatarUrlSigner;
import com.songshanhu.blog.util.JwtUtil;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户登录与注册接口")
public class AuthController {

    private final IUserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final AvatarUrlSigner avatarUrlSigner;
    private final StringRedisTemplate redisTemplate;
    private final BizUserPunishLogMapper bizUserPunishLogMapper;

    private static final int MAX_FAILS = 5;
    private static final Duration LOCK_TTL = Duration.ofMinutes(15);

    public AuthController(IUserService userService,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager,
                          AvatarUrlSigner avatarUrlSigner,
                          StringRedisTemplate redisTemplate,
                          BizUserPunishLogMapper bizUserPunishLogMapper) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.avatarUrlSigner = avatarUrlSigner;
        this.redisTemplate = redisTemplate;
        this.bizUserPunishLogMapper = bizUserPunishLogMapper;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername();
        String email = request.getEmail();
        if (!StringUtils.hasText(username)) {
            return Result.error(400, "用户名不能为空");
        }


        log.info("用户注册请求: {}", username);

        if (userService.findByUsername(username) != null) {
            return Result.error("用户名已存在");
        }

        if (StringUtils.hasText(email) && userService.findByEmail(email) != null) {
            return Result.error("邮箱已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(request.getPassword());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : username);
        user.setEmail(email);
        userService.register(user);

        String token = jwtUtil.generateToken(user.getUsername());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .mustResetPassword(false)
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(avatarUrlSigner.signUserAvatarLg(user.getAvatar()))
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
        return Result.success(response);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) {
        String identifier = request.getUsername();
        if (!StringUtils.hasText(identifier) || !StringUtils.hasText(request.getPassword())) {
            return Result.error(400, "账号或密码不能为空");
        }

        String key = "auth:fail:" + identifier.toLowerCase();
        String lockKey = "auth:lock:" + identifier.toLowerCase();
        String locked = redisTemplate.opsForValue().get(lockKey);
        if (StringUtils.hasText(locked)) {
            Long ttl = redisTemplate.getExpire(lockKey);
            long seconds = ttl == null || ttl < 0 ? LOCK_TTL.getSeconds() : ttl;
            return Result.error(423, "账号已锁定，请稍后再试（约 " + (seconds / 60 + 1) + " 分钟）");
        }

        log.info("用户登录请求: {}", identifier);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );
            if (!authentication.isAuthenticated()) {
                return Result.error(401, "认证失败");
            }
        } catch (AuthenticationException e) {
            Long cnt = redisTemplate.opsForValue().increment(key);
            if (cnt != null && cnt == 1) {
                redisTemplate.expire(key, LOCK_TTL);
            }
            long c = cnt == null ? 1 : cnt;
            if (c >= MAX_FAILS) {
                redisTemplate.opsForValue().set(lockKey, "1", LOCK_TTL);
                return Result.error(423, "密码错误次数过多，账号已锁定 15 分钟");
            }
            return Result.error(401, "账号或密码错误（剩余尝试次数：" + (MAX_FAILS - c) + "）");
        }

        redisTemplate.delete(key);
        redisTemplate.delete(lockKey);

        User user = userService.findByUsernameOrEmail(identifier);
        if (user == null) {
            return Result.error(401, "认证失败");
        }

        if (isLegacyAndWeakPassword(user.getPassword())) {
            String resetToken = jwtUtil.generateFirstResetToken(user.getUsername());
            AuthResponse response = AuthResponse.builder()
                    .mustResetPassword(true)
                    .resetToken(resetToken)
                    .user(AuthResponse.UserResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .nickname(user.getNickname())
                            .avatar(avatarUrlSigner.signUserAvatarLg(user.getAvatar()))
                            .email(user.getEmail())
                            .role(user.getRole())
                            .build())
                    .build();
            return Result.success(response);
        }

        if (isBanned(user.getId())) {
            return Result.error(403, "账号已被封禁");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .mustResetPassword(false)
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(avatarUrlSigner.signUserAvatarLg(user.getAvatar()))
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();

        return Result.success(response);
    }

    @PostMapping("/first-reset")
    @Operation(summary = "首次重置密码（兼容历史弱密码账号）")
    public Result<AuthResponse> firstReset(@Valid @RequestBody FirstResetPasswordRequest req) {
        String purpose = jwtUtil.extractPurpose(req.getResetToken());
        if (!"first_reset".equals(purpose)) {
            return Result.error(401, "无效的重置令牌");
        }
        String username = jwtUtil.extractUsername(req.getResetToken());
        User user = userService.findByUsername(username);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        if (!isLegacyAndWeakPassword(user.getPassword())) {
            return Result.error(400, "该账号无需首次重置密码");
        }

        userService.updatePassword(user.getId(), req.getNewPassword());

        String token = jwtUtil.generateToken(user.getUsername());
        AuthResponse response = AuthResponse.builder()
                .token(token)
                .mustResetPassword(false)
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .avatar(avatarUrlSigner.signUserAvatarLg(user.getAvatar()))
                        .email(user.getEmail())
                        .role(user.getRole())
                        .build())
                .build();
        return Result.success(response);
    }

    private boolean isLegacyAndWeakPassword(String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
            return false;
        }
        boolean bcrypt = storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$");
        if (bcrypt) {
            return false;
        }
        return storedPassword.length() < 6;
    }

    private boolean isBanned(Long userId) {
        if (userId == null) {
            return false;
        }
        BizUserPunishLog last;
        try {
            last = bizUserPunishLogMapper.selectOne(new LambdaQueryWrapper<BizUserPunishLog>()
                    .eq(BizUserPunishLog::getUserId, userId)
                    .orderByDesc(BizUserPunishLog::getId)
                    .last("limit 1"));
        } catch (Exception e) {
            return false;
        }
        if (last == null) {
            return false;
        }
        if (!"BAN".equalsIgnoreCase(last.getAction())) {
            return false;
        }
        if (last.getBanEndTime() == null) {
            return true;
        }
        return last.getBanEndTime().isAfter(LocalDateTime.now());
    }
}
