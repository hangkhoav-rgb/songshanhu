package com.songshanhu.blog.security;

import com.songshanhu.blog.util.JwtUtil;
import com.songshanhu.blog.entity.BizUserPunishLog;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.mapper.BizUserPunishLogMapper;
import com.songshanhu.blog.service.IUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final IUserService userService;
    private final BizUserPunishLogMapper bizUserPunishLogMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   UserDetailsService userDetailsService,
                                   IUserService userService,
                                   BizUserPunishLogMapper bizUserPunishLogMapper) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.bizUserPunishLogMapper = bizUserPunishLogMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            jwt = authHeader.substring(7);
            username = jwtUtil.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtUtil.validateToken(jwt, userDetails.getUsername())) {
                    User u = userService.findByUsername(username);
                    if (u != null && isBanned(u.getId())) {
                        response.setStatus(403);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":403,\"message\":\"账号已被封禁\",\"data\":null}");
                        return;
                    }
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // JWT 解析失败（如 Token 过期、非法等），不抛出异常，直接让请求继续
            // AnonymousAuthenticationFilter 会处理未授权的情况
            logger.warn("JWT 认证失败: " + e.getMessage());
        }
        chain.doFilter(request, response);
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
