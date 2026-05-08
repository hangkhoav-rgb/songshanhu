package com.songshanhu.blog.controller;

import com.songshanhu.blog.dto.response.DashboardStatsResponse;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.service.IArticleService;
import com.songshanhu.blog.service.IUserService;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
@Tag(name = "统计分析", description = "内容影响力分析相关接口")
@RequiredArgsConstructor
public class StatsController {

    private final IArticleService articleService;
    private final IUserService userService;

    @Operation(summary = "获取当前用户数据看板统计")
    @GetMapping("/dashboard")
    public Result<DashboardStatsResponse> getDashboardStats() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return Result.error("请先登录以查看统计数据");
        }
        String username = ((UserDetails) principal).getUsername();
        User user = userService.findByUsername(username);
        
        if (user == null) {
            return Result.error("用户不存在");
        }
        
        return Result.success(articleService.getStatsByAuthor(user.getId()));
    }
}
