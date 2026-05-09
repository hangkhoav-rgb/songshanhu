package com.songshanhu.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.songshanhu.blog.dto.response.DashboardStatsResponse;
import com.songshanhu.blog.entity.BizArticleReview;
import com.songshanhu.blog.entity.Article;
import com.songshanhu.blog.enums.ArticleStatus;
import com.songshanhu.blog.mapper.ArticleMapper;
import com.songshanhu.blog.mapper.BizArticleReviewMapper;
import com.songshanhu.blog.service.IArticleService;
import com.songshanhu.blog.util.ContentModerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements IArticleService {

    private final ContentModerator contentModerator;
    private final BizArticleReviewMapper bizArticleReviewMapper;

    @Override
    public DashboardStatsResponse getStatsByAuthor(Long authorId) {
        List<Article> articles = list(new LambdaQueryWrapper<Article>()
                .eq(Article::getAuthorId, authorId)
                .eq(Article::getStatus, ArticleStatus.PUBLISHED.getCode()));

        DashboardStatsResponse response = new DashboardStatsResponse();
        
        // 总计数据
        response.setTotalArticles((long) articles.size());
        response.setTotalViews(articles.stream().mapToLong(a -> a.getViews() != null ? a.getViews() : 0).sum());
        response.setTotalLikes(articles.stream().mapToLong(a -> a.getLikes() != null ? a.getLikes() : 0).sum());
        response.setTotalComments(articles.stream().mapToLong(a -> a.getComments() != null ? a.getComments() : 0).sum());

        // 分类分布
        Map<String, Long> categoryCount = articles.stream()
                .filter(a -> a.getCategory() != null)
                .collect(Collectors.groupingBy(Article::getCategory, Collectors.counting()));
        
        List<DashboardStatsResponse.CategoryStats> categoryStatsList = new ArrayList<>();
        categoryCount.forEach((k, v) -> {
            DashboardStatsResponse.CategoryStats stats = new DashboardStatsResponse.CategoryStats();
            stats.setName(k);
            stats.setValue(v);
            categoryStatsList.add(stats);
        });
        response.setCategoryDistribution(categoryStatsList);

        // 发布趋势 (近 7 天，补全缺失日期)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d");
        List<DashboardStatsResponse.DailyStats> dailyStatsList = new ArrayList<>();
        
        for (int i = 6; i >= 0; i--) {
            String date = LocalDateTime.now().minusDays(i).format(formatter);
            long count = articles.stream()
                    .filter(a -> a.getCreateTime() != null && a.getCreateTime().format(formatter).equals(date))
                    .count();
            
            DashboardStatsResponse.DailyStats ds = new DashboardStatsResponse.DailyStats();
            ds.setDate(date);
            ds.setCount(count);
            dailyStatsList.add(ds);
        }
        response.setDailyTrend(dailyStatsList);
        
        // 地理坐标点
        List<DashboardStatsResponse.LocationStats> locationPoints = articles.stream()
                .filter(a -> a.getLongitude() != null && a.getLatitude() != null)
                .map(a -> {
                    DashboardStatsResponse.LocationStats ls = new DashboardStatsResponse.LocationStats();
                    ls.setLongitude(a.getLongitude());
                    ls.setLatitude(a.getLatitude());
                    ls.setTitle(a.getTitle());
                    return ls;
                })
                .collect(Collectors.toList());
        response.setLocationPoints(locationPoints);
        
        return response;
    }

    @Override
    public Page<Article> getArticlePage(int current, int size, String category, String keyword, Long authorId, Integer statusFilter) {
        Page<Article> page = new Page<>(current, size);
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<Article>()
                .orderByDesc(Article::getCreateTime);

        if (authorId != null) {
            queryWrapper.eq(Article::getAuthorId, authorId);
        }

        if (statusFilter != null) {
            queryWrapper.eq(Article::getStatus, statusFilter);
        } else if (authorId == null) {
            queryWrapper.eq(Article::getStatus, ArticleStatus.PUBLISHED.getCode());
        }

        if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category)) {
            queryWrapper.eq(Article::getCategory, category);
        }

        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(w -> w.like(Article::getTitle, keyword)
                    .or().like(Article::getSummary, keyword));
        }

        return page(page, queryWrapper);
    }

    @Override
    public Article getArticleById(Long id) {
        Article article = getById(id);
        return article;
    }

    @Override
    public void publishArticle(Article article) {
        boolean isDraft = article.getStatus() != null && article.getStatus() == ArticleStatus.DRAFT.getCode();
        int hitLevel = 0;
        List<String> hitWords = new ArrayList<>();

        if (!isDraft) {
            ContentModerator.ModerationResult t = contentModerator.moderate(article.getTitle());
            ContentModerator.ModerationResult c = contentModerator.moderate(article.getContent());
            hitLevel = Math.max(t.getHitLevel(), c.getHitLevel());
            hitWords.addAll(t.getHitWords());
            hitWords.addAll(c.getHitWords());
            if (hitLevel >= 3) {
                throw new RuntimeException("内容包含违规词汇，请修改后重试");
            }
        }

        if (!StringUtils.hasText(article.getSummary()) && StringUtils.hasText(article.getContent())) {
            // 简单截取正文作为摘要 (去除 HTML 标签)
            String plainText = article.getContent().replaceAll("<[^>]*>", "");
            article.setSummary(plainText.substring(0, Math.min(plainText.length(), 150)) + "...");
        }

        if (!isDraft) {
            article.setStatus(ArticleStatus.PENDING.getCode());
            article.setLastReviewResult("submit");
            article.setLastReviewTime(LocalDateTime.now());
            if (hitLevel == 2) {
                article.setLastReviewReason("敏感词命中，待审核");
            } else {
                article.setLastReviewReason("已提交审核");
            }
        }
        save(article);

        if (!isDraft && article.getId() != null) {
            BizArticleReview r = new BizArticleReview();
            r.setArticleId(article.getId());
            r.setReviewer(StringUtils.hasText(article.getAuthorName()) ? article.getAuthorName() : "author");
            r.setResult("SUBMIT");
            r.setReason(article.getLastReviewReason());
            r.setHitLevel(hitLevel == 0 ? null : hitLevel);
            r.setHitWords(hitWords.isEmpty() ? null : String.join(",", hitWords));
            bizArticleReviewMapper.insert(r);
        }
    }

    @Override
    public boolean resubmitArticle(Long articleId, Long authorId) {
        Article article = getById(articleId);
        if (article == null) {
            return false;
        }
        if (authorId == null || article.getAuthorId() == null || !article.getAuthorId().equals(authorId)) {
            return false;
        }
        article.setStatus(ArticleStatus.PENDING.getCode());
        article.setLastReviewResult("submit");
        article.setLastReviewReason("已提交审核");
        article.setLastReviewTime(LocalDateTime.now());
        boolean ok = updateById(article);
        if (!ok) {
            return false;
        }
        BizArticleReview r = new BizArticleReview();
        r.setArticleId(articleId);
        r.setReviewer(StringUtils.hasText(article.getAuthorName()) ? article.getAuthorName() : "author");
        r.setResult("SUBMIT");
        r.setReason("重新提交审核");
        bizArticleReviewMapper.insert(r);
        return true;
    }

    @Override
    public Page<Article> getAdminArticlePage(int current, int size, String keyword, String authorName, Integer status) {
        Page<Article> page = new Page<>(current, size);
        LambdaQueryWrapper<Article> qw = new LambdaQueryWrapper<Article>().orderByDesc(Article::getCreateTime);
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Article::getTitle, keyword).or().like(Article::getSummary, keyword));
        }
        if (StringUtils.hasText(authorName)) {
            qw.like(Article::getAuthorName, authorName);
        }
        if (status != null) {
            qw.eq(Article::getStatus, status);
        }
        return page(page, qw);
    }

    @Override
    public boolean adminApprove(Long articleId, String reviewer, String reason) {
        String r = StringUtils.hasText(reason) ? reason : "通过";
        return adminUpdateStatus(articleId, reviewer, ArticleStatus.PUBLISHED.getCode(), "APPROVE", r, "approve");
    }

    @Override
    public boolean adminReject(Long articleId, String reviewer, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("驳回原因必填");
        }
        return adminUpdateStatus(articleId, reviewer, ArticleStatus.REJECTED.getCode(), "REJECT", reason, "reject");
    }

    @Override
    public boolean adminOffline(Long articleId, String reviewer, String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("下架原因必填");
        }
        return adminUpdateStatus(articleId, reviewer, ArticleStatus.OFFLINE.getCode(), "OFFLINE", reason, "offline");
    }

    private boolean adminUpdateStatus(Long articleId, String reviewer, int newStatus, String reviewResult, String reason, String lastResult) {
        Article article = getById(articleId);
        if (article == null) {
            return false;
        }
        article.setStatus(newStatus);
        article.setLastReviewResult(lastResult);
        article.setLastReviewReason(reason);
        article.setLastReviewTime(LocalDateTime.now());
        boolean ok = updateById(article);
        if (!ok) {
            return false;
        }
        BizArticleReview r = new BizArticleReview();
        r.setArticleId(articleId);
        r.setReviewer(StringUtils.hasText(reviewer) ? reviewer : "admin");
        r.setResult(reviewResult);
        r.setReason(reason);
        bizArticleReviewMapper.insert(r);
        return true;
    }
}
