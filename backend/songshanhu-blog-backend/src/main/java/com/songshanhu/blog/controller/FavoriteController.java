package com.songshanhu.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.songshanhu.blog.dto.request.BatchIdsRequest;
import com.songshanhu.blog.entity.ArticleCollect;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.mapper.ArticleCollectMapper;
import com.songshanhu.blog.service.IDashboardService;
import com.songshanhu.blog.service.IUserService;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorite")
@Tag(name = "收藏", description = "收藏相关接口")
@RequiredArgsConstructor
public class FavoriteController {
    private final IUserService userService;
    private final ArticleCollectMapper articleCollectMapper;
    private final StringRedisTemplate redisTemplate;
    private final IDashboardService dashboardService;

    @Operation(summary = "收藏/取消收藏（文章）")
    @PostMapping("/{id}")
    public Result<?> toggle(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        String key = "collect:article:" + id;
        boolean collected;

        long existed = articleCollectMapper.selectCount(new LambdaQueryWrapper<ArticleCollect>()
                .eq(ArticleCollect::getArticleId, id)
                .eq(ArticleCollect::getUserId, userId));
        if (existed > 0) {
            articleCollectMapper.delete(new LambdaQueryWrapper<ArticleCollect>()
                    .eq(ArticleCollect::getArticleId, id)
                    .eq(ArticleCollect::getUserId, userId));
            redisTemplate.opsForSet().remove(key, String.valueOf(userId));
            collected = false;
        } else {
            ArticleCollect collect = new ArticleCollect();
            collect.setArticleId(id);
            collect.setUserId(userId);
            articleCollectMapper.insert(collect);
            redisTemplate.opsForSet().add(key, String.valueOf(userId));
            collected = true;
        }
        dashboardService.invalidateUserSummaryCache(userId);
        long count = articleCollectMapper.selectCount(new LambdaQueryWrapper<ArticleCollect>().eq(ArticleCollect::getArticleId, id));
        return Result.success(java.util.Map.of("collected", collected, "collects", count));
    }

    @Operation(summary = "收藏状态与计数（文章）")
    @GetMapping("/{id}/status")
    public Result<?> status(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        long existed = articleCollectMapper.selectCount(new LambdaQueryWrapper<ArticleCollect>()
                .eq(ArticleCollect::getArticleId, id)
                .eq(ArticleCollect::getUserId, userId));
        boolean collected = existed > 0;
        long count = articleCollectMapper.selectCount(new LambdaQueryWrapper<ArticleCollect>().eq(ArticleCollect::getArticleId, id));
        return Result.success(java.util.Map.of("collected", collected, "collects", count));
    }

    @Operation(summary = "批量取消收藏（文章）")
    @PostMapping("/batch-delete")
    public Result<?> batchDelete(@Valid @RequestBody BatchIdsRequest req) {
        Long userId = getCurrentUserId();
        int deleted = articleCollectMapper.delete(new LambdaQueryWrapper<ArticleCollect>()
                .eq(ArticleCollect::getUserId, userId)
                .in(ArticleCollect::getArticleId, req.getArticleIds()));
        dashboardService.invalidateUserSummaryCache(userId);
        return Result.success(java.util.Map.of("deleted", deleted));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        User user = userService.findByUsername(username);
        return user.getId();
    }
}
