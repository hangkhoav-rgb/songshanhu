package com.songshanhu.blog.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.songshanhu.blog.dto.request.ArticleRequest;
import com.songshanhu.blog.dto.request.BatchIdsRequest;
import com.songshanhu.blog.dto.response.ArticleActionItem;
import com.songshanhu.blog.entity.ArticleCollect;
import com.songshanhu.blog.entity.ArticleLike;
import com.songshanhu.blog.entity.Article;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.enums.ArticleStatus;
import com.songshanhu.blog.mapper.ArticleActionMapper;
import com.songshanhu.blog.mapper.ArticleCollectMapper;
import com.songshanhu.blog.mapper.ArticleLikeMapper;
import com.songshanhu.blog.service.IArticleService;
import com.songshanhu.blog.service.IDashboardService;
import com.songshanhu.blog.service.IUserService;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;


@Slf4j
@RestController
@RequestMapping("/article")
@Tag(name = "文章管理", description = "文章列表、详情与发布接口")
public class ArticleController {

    private final IArticleService articleService;
    private final IUserService userService;
    private final StringRedisTemplate redisTemplate;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleCollectMapper articleCollectMapper;
    private final ArticleActionMapper articleActionMapper;
    private final IDashboardService dashboardService;

    public ArticleController(IArticleService articleService,
                             IUserService userService,
                             StringRedisTemplate redisTemplate,
                             ArticleLikeMapper articleLikeMapper,
                             ArticleCollectMapper articleCollectMapper,
                             ArticleActionMapper articleActionMapper,
                             IDashboardService dashboardService) {
        this.articleService = articleService;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.articleLikeMapper = articleLikeMapper;
        this.articleCollectMapper = articleCollectMapper;
        this.articleActionMapper = articleActionMapper;
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "获取文章列表")
    @GetMapping("/list")
    public Result<Page<Article>> list(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "false") boolean mine) {
        Long authorId = null;
        if (mine) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Result.error("未登录");
            }
            Object principal = auth.getPrincipal();
            String username = ((UserDetails) principal).getUsername();
            User user = userService.findByUsername(username);
            authorId = user.getId();
        } else {
            status = ArticleStatus.PUBLISHED.getCode();
        }
        return Result.success(articleService.getArticlePage(current, size, category, keyword, authorId, status));
    }

    @Operation(summary = "获取文章详情")
    @GetMapping("/{id}")
    public Result<Article> detail(@PathVariable Long id) {
        Article article = articleService.getArticleById(id);
        if (article == null) {
            return Result.error(404, "文章不存在");
        }

        Long viewerId = getOptionalCurrentUserId();
        boolean isAuthor = viewerId != null && article.getAuthorId() != null && article.getAuthorId().equals(viewerId);
        if (!isAuthor && (article.getStatus() == null || article.getStatus() != ArticleStatus.PUBLISHED.getCode())) {
            return Result.error(403, "文章未发布或已下架");
        }

        if (!isAuthor && article.getStatus() != null && article.getStatus() == ArticleStatus.PUBLISHED.getCode()) {
            Integer currentViews = article.getViews();
            article.setViews(currentViews == null ? 1 : currentViews + 1);
            articleService.updateById(article);
        }
        return Result.success(article);
    }

    @Operation(summary = "提交文章（草稿/提交审核）")
    @PostMapping("/publish")
    public Result<?> publish(@RequestBody ArticleRequest request) {
        log.info("用户发布文章: {}", request.getTitle());
        
        // 获取当前登录用户
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        User user = userService.findByUsername(username);

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setSummary(request.getSummary());
        article.setCategory(request.getCategory());
        article.setCoverImage(request.getCoverImage());
        article.setLongitude(request.getLongitude());
        article.setLatitude(request.getLatitude());
        article.setStatus(request.getStatus() != null && request.getStatus() == ArticleStatus.DRAFT.getCode()
                ? ArticleStatus.DRAFT.getCode()
                : ArticleStatus.PENDING.getCode());
        
        article.setAuthorId(user.getId());
        article.setAuthorName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        article.setViews(0);
        article.setComments(0);
        article.setLikes(0);

        articleService.publishArticle(article);
        return Result.success(article.getId());
    }

    @Operation(summary = "重新提交审核")
    @PostMapping("/{id}/resubmit")
    public Result<?> resubmit(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean ok = articleService.resubmitArticle(id, userId);
        if (!ok) {
            return Result.error(403, "无权限或文章不存在");
        }
        return Result.success(null);
    }

    @Operation(summary = "获取我的文章列表（支持草稿/已发布）")
    @GetMapping("/mine")
    public Result<Page<Article>> mine(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        User user = userService.findByUsername(username);
        return Result.success(articleService.getArticlePage(current, size, null, keyword, user.getId(), status));
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/{id}/like")
    public Result<?> toggleLike(@PathVariable Long id) {
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

    @Operation(summary = "获取点赞状态")
    @GetMapping("/{id}/like/status")
    public Result<?> likeStatus(@PathVariable Long id) {
        Article a = articleService.getById(id);
        int likes = a == null || a.getLikes() == null ? 0 : a.getLikes();
        boolean liked = false;
        try {
            Long userId = getCurrentUserId();
            liked = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id)
                    .eq(ArticleLike::getUserId, userId)) > 0;
        } catch (Exception ignored) {
        }
        return Result.success(java.util.Map.of("liked", liked, "likes", likes));
    }

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/{id}/collect")
    public Result<?> toggleCollect(@PathVariable Long id) {
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

    @Operation(summary = "批量取消点赞")
    @PostMapping("/like/batch-delete")
    public Result<?> batchUnlike(@Valid @RequestBody BatchIdsRequest req) {
        Long userId = getCurrentUserId();
        int deleted = articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getUserId, userId)
                .in(ArticleLike::getArticleId, req.getArticleIds()));
        dashboardService.invalidateUserSummaryCache(userId);
        return Result.success(java.util.Map.of("deleted", deleted));
    }

    @Operation(summary = "批量取消收藏")
    @PostMapping("/collect/batch-delete")
    public Result<?> batchUncollect(@Valid @RequestBody BatchIdsRequest req) {
        Long userId = getCurrentUserId();
        int deleted = articleCollectMapper.delete(new LambdaQueryWrapper<ArticleCollect>()
                .eq(ArticleCollect::getUserId, userId)
                .in(ArticleCollect::getArticleId, req.getArticleIds()));
        dashboardService.invalidateUserSummaryCache(userId);
        return Result.success(java.util.Map.of("deleted", deleted));
    }

    @Operation(summary = "获取收藏状态")
    @GetMapping("/{id}/collect/status")
    public Result<?> collectStatus(@PathVariable Long id) {
        boolean collected = false;
        long count = articleCollectMapper.selectCount(new LambdaQueryWrapper<ArticleCollect>().eq(ArticleCollect::getArticleId, id));
        try {
            Long userId = getCurrentUserId();
            collected = articleCollectMapper.selectCount(new LambdaQueryWrapper<ArticleCollect>()
                    .eq(ArticleCollect::getArticleId, id)
                    .eq(ArticleCollect::getUserId, userId)) > 0;
        } catch (Exception ignored) {
        }
        return Result.success(java.util.Map.of("collected", collected, "collects", count));
    }

    @Operation(summary = "获取我的收藏/点赞列表")
    @GetMapping("/actions/mine")
    public Result<Page<ArticleActionItem>> myActions(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean liked,
            @RequestParam(defaultValue = "true") boolean collected) {
        if (!liked && !collected) {
            return Result.success(new Page<>(current, size));
        }
        Long userId = getCurrentUserId();
        Page<ArticleActionItem> page = new Page<>(current, size);
        return Result.success(articleActionMapper.selectUserActions(page, userId, liked, collected));
    }

    private Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        User user = userService.findByUsername(username);
        return user.getId();
    }

    private Long getOptionalCurrentUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            Object principal = auth.getPrincipal();
            if (!(principal instanceof UserDetails)) {
                return null;
            }
            String username = ((UserDetails) principal).getUsername();
            User user = userService.findByUsername(username);
            return user == null ? null : user.getId();
        } catch (Exception e) {
            return null;
        }
    }
}
