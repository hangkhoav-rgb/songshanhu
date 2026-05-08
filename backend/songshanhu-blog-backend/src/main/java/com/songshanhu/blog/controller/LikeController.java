package com.songshanhu.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.songshanhu.blog.dto.request.BatchIdsRequest;
import com.songshanhu.blog.entity.Article;
import com.songshanhu.blog.entity.ArticleLike;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.mapper.ArticleLikeMapper;
import com.songshanhu.blog.service.IArticleService;
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
@RequestMapping("/like")
@Tag(name = "点赞", description = "点赞相关接口")
@RequiredArgsConstructor
public class LikeController {
    private final IUserService userService;
    private final IArticleService articleService;
    private final ArticleLikeMapper articleLikeMapper;
    private final StringRedisTemplate redisTemplate;
    private final IDashboardService dashboardService;

    @Operation(summary = "点赞/取消点赞（文章）")
    @PostMapping("/{id}")
    public Result<?> toggle(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        String key = "like:article:" + id;
        Article a = articleService.getById(id);
        if (a == null) {
            return Result.error("文章不存在");
        }
        int likes = a.getLikes() == null ? 0 : a.getLikes();

        long existed = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, id)
                .eq(ArticleLike::getUserId, userId));
        boolean liked;
        if (existed > 0) {
            articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id)
                    .eq(ArticleLike::getUserId, userId));
            redisTemplate.opsForSet().remove(key, String.valueOf(userId));
            likes = Math.max(0, likes - 1);
            liked = false;
        } else {
            ArticleLike like = new ArticleLike();
            like.setArticleId(id);
            like.setUserId(userId);
            articleLikeMapper.insert(like);
            redisTemplate.opsForSet().add(key, String.valueOf(userId));
            likes = likes + 1;
            liked = true;
        }
        dashboardService.invalidateUserSummaryCache(userId);
        a.setLikes(likes);
        articleService.updateById(a);
        return Result.success(java.util.Map.of("liked", liked, "likes", likes));
    }

    @Operation(summary = "点赞状态与计数（文章）")
    @GetMapping("/{id}/status")
    public Result<?> status(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        Article a = articleService.getById(id);
        if (a == null) {
            return Result.error("文章不存在");
        }
        int likes = a.getLikes() == null ? 0 : a.getLikes();
        long existed = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, id)
                .eq(ArticleLike::getUserId, userId));
        boolean liked = existed > 0;
        return Result.success(java.util.Map.of("liked", liked, "likes", likes));
    }

    @Operation(summary = "批量取消点赞（文章）")
    @PostMapping("/batch-delete")
    public Result<?> batchDelete(@Valid @RequestBody BatchIdsRequest req) {
        Long userId = getCurrentUserId();
        int deleted = articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .in(ArticleLike::getArticleId, req.getArticleIds()));
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
