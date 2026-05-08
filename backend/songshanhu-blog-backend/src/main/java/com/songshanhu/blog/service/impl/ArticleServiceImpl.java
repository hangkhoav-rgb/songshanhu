package com.songshanhu.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.songshanhu.blog.dto.response.DashboardStatsResponse;
import com.songshanhu.blog.entity.BizArticleReview;
import com.songshanhu.blog.entity.Article;
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
                .eq(Article::getStatus, 1));

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
            queryWrapper.eq(Article::getStatus, 1);
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
        if (article != null) {
            // 简单模拟阅读量增加，增加空值检查
            Integer currentViews = article.getViews();
            article.setViews(currentViews == null ? 1 : currentViews + 1);
            updateById(article);
        }
        return article;
    }

    @Override
    public void publishArticle(Article article) {
        ContentModerator.ModerationResult t = contentModerator.moderate(article.getTitle());
        ContentModerator.ModerationResult c = contentModerator.moderate(article.getContent());
        int hitLevel = Math.max(t.getHitLevel(), c.getHitLevel());
        List<String> hitWords = new ArrayList<>();
        hitWords.addAll(t.getHitWords());
        hitWords.addAll(c.getHitWords());
        if (hitLevel >= 3) {
            throw new RuntimeException("内容包含违规词汇，请修改后重试");
        }

        if (!StringUtils.hasText(article.getSummary()) && StringUtils.hasText(article.getContent())) {
            // 简单截取正文作为摘要 (去除 HTML 标签)
            String plainText = article.getContent().replaceAll("<[^>]*>", "");
            article.setSummary(plainText.substring(0, Math.min(plainText.length(), 150)) + "...");
        }
        save(article);

        if (hitLevel == 2 && article.getId() != null) {
            BizArticleReview r = new BizArticleReview();
            r.setArticleId(article.getId());
            r.setReviewer("system");
            r.setResult("PENDING");
            r.setReason("敏感词命中，待审核");
            r.setHitLevel(hitLevel);
            r.setHitWords(String.join(",", hitWords));
            bizArticleReviewMapper.insert(r);
        }
    }
}
