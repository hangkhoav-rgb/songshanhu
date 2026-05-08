package com.songshanhu.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.songshanhu.blog.dto.response.DashboardActionVO;
import com.songshanhu.blog.dto.response.DashboardInitResponse;
import com.songshanhu.blog.dto.response.DashboardStatsResponse;
import com.songshanhu.blog.dto.response.DashboardSummaryResponse;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.service.IArticleService;
import com.songshanhu.blog.service.IDashboardService;
import com.songshanhu.blog.service.IUserService;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "数据看板", description = "用户互动与内容数据看板")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;
    private final IUserService userService;
    private final IArticleService articleService;

    @Operation(summary = "看板初始数据：KPI + 互动汇总 + 最近互动列表")
    @GetMapping
    public Result<DashboardInitResponse> init(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "30") int days) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        Long userId = user.getId();
        DashboardInitResponse resp = new DashboardInitResponse();
        DashboardStatsResponse stats = articleService.getStatsByAuthor(userId);
        resp.setStats(stats);
        resp.setSummary(dashboardService.getSummary(userId));
        resp.setActions(dashboardService.getRecentActions(userId, current, size, actionType, days));
        return Result.success(resp);
    }

    @Operation(summary = "看板汇总：点赞数、收藏数、最近互动时间")
    @GetMapping("/summary")
    public Result<DashboardSummaryResponse> summary() {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        Long userId = user.getId();
        return Result.success(dashboardService.getSummary(userId));
    }

    @Operation(summary = "看板互动列表：最近 30 天点赞/收藏记录")
    @GetMapping("/actions")
    public Result<Page<DashboardActionVO>> actions(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(defaultValue = "30") int days) {
        User user = getCurrentUser();
        if (user == null) {
            return Result.error(401, "请先登录");
        }
        Long userId = user.getId();
        return Result.success(dashboardService.getRecentActions(userId, current, size, actionType, days));
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof UserDetails)) return null;
        String username = ((UserDetails) principal).getUsername();
        return userService.findByUsername(username);
    }
}
