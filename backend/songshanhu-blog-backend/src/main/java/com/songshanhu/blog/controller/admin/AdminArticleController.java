package com.songshanhu.blog.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.songshanhu.blog.entity.Article;
import com.songshanhu.blog.service.IArticleService;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/article")
@Tag(name = "管理员-文章审核", description = "先审后发：待审核队列与审核动作")
public class AdminArticleController {

    private final IArticleService articleService;

    public AdminArticleController(IArticleService articleService) {
        this.articleService = articleService;
    }

    @Operation(summary = "待审核列表")
    @GetMapping("/pending")
    public Result<Page<Article>> pending(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String authorName) {
        return Result.success(articleService.getAdminArticlePage(current, size, keyword, authorName, 1));
    }

    @Operation(summary = "文章列表（全状态）")
    @GetMapping("/list")
    public Result<Page<Article>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) Integer status) {
        return Result.success(articleService.getAdminArticlePage(current, size, keyword, authorName, status));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    public Result<?> approve(@PathVariable Long id, @Valid @RequestBody AdminReasonReq req, @RequestHeader(value = "X-Admin-User", required = false) String reviewer) {
        boolean ok = articleService.adminApprove(id, normalizeReviewer(reviewer), req.getReason());
        if (!ok) {
            return Result.error(404, "文章不存在");
        }
        return Result.success(null);
    }

    @Operation(summary = "审核驳回")
    @PostMapping("/{id}/reject")
    public Result<?> reject(@PathVariable Long id, @Valid @RequestBody AdminRejectReq req, @RequestHeader(value = "X-Admin-User", required = false) String reviewer) {
        boolean ok = articleService.adminReject(id, normalizeReviewer(reviewer), req.getReason());
        if (!ok) {
            return Result.error(404, "文章不存在");
        }
        return Result.success(null);
    }

    @Operation(summary = "下架")
    @PostMapping("/{id}/offline")
    public Result<?> offline(@PathVariable Long id, @Valid @RequestBody AdminOfflineReq req, @RequestHeader(value = "X-Admin-User", required = false) String reviewer) {
        boolean ok = articleService.adminOffline(id, normalizeReviewer(reviewer), req.getReason());
        if (!ok) {
            return Result.error(404, "文章不存在");
        }
        return Result.success(null);
    }

    private String normalizeReviewer(String reviewer) {
        return StringUtils.hasText(reviewer) ? reviewer : "admin";
    }

    @Data
    public static class AdminReasonReq {
        private String reason;
    }

    @Data
    public static class AdminRejectReq {
        @NotBlank
        private String reason;
    }

    @Data
    public static class AdminOfflineReq {
        @NotBlank
        private String reason;
    }
}

