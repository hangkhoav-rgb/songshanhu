package com.songshanhu.blog.controller;

import com.songshanhu.blog.entity.Article;
import com.songshanhu.blog.service.IArticleService;
import com.songshanhu.blog.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class SeederController {

    private final IArticleService articleService;

    @PostMapping("/articles")
    public Result<String> seedArticles() {
        List<Article> seedData = Arrays.asList(
            createArticle("探索松山湖：科技与自然的和谐共生", "松山湖高新区不仅是高新技术的聚集地，更是一个拥有优美自然景观的生态园。本文将带你领略华为欧洲小镇的风采...", "tech", "https://images.unsplash.com/photo-1518005020250-68594932387c?q=80&w=800", 1200, 45, 12, 113.883, 22.895),
            createArticle("松山湖骑行指南：最美环湖路线推荐", "周末不知道去哪？松山湖环湖路全程约20公里，沿途绿树成荫，湖水清澈。本指南为你总结了最佳的骑行路线...", "life", "https://images.unsplash.com/photo-1541625602330-2277a4c4b282?q=80&w=800", 850, 32, 8, 113.892, 22.912),
            createArticle("数据可视化在城市规划中的应用", "通过 ECharts 展示松山湖区域的人口流动与产业布局，我们可以更直观地理解这座城市的脉动。本文将分享相关的可视化实践...", "visual", "https://images.unsplash.com/photo-1551288049-bbbda536339a?q=80&w=800", 2100, 88, 25, 113.875, 22.903),
            createArticle("松山湖图书馆：静谧的阅读天堂", "位于湖畔的图书馆，以其独特的建筑风格和丰富的藏书吸引了无数读者。在这里，你可以一边阅读，一边欣赏湖光山色...", "life", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=800", 600, 15, 3, 113.881, 22.901),
            createArticle("华为欧洲小镇：松山湖的异域风情", "模仿欧洲经典建筑风格打造的华为溪流背坡村（欧洲小镇），已经成为松山湖的标志性名片。让我们一起走进这片童话般的办公区...", "tech", "https://images.unsplash.com/photo-1512100356132-db7f5c423136?q=80&w=800", 3500, 150, 42, 113.895, 22.888),
            createArticle("智能制造在东莞：松山湖机器人的崛起", "松山湖机器人产业园聚集了数百家机器人企业。从核心零部件到整机研发，这里正引领着中国智能制造的新篇章...", "tech", "https://images.unsplash.com/photo-1531746790731-6c087fecd05a?q=80&w=800", 1800, 65, 18, 113.868, 22.921),
            createArticle("可视化大屏设计：从零到一的实践", "分享如何利用 ECharts 和 Vue 3 构建一个响应式的松山湖区域监控大屏。涵盖了布局设计、数据对接及性能优化等核心要点...", "visual", "https://images.unsplash.com/photo-1504868584819-f8e90526354c?q=80&w=800", 1500, 54, 20, 113.905, 22.908),
            createArticle("松山湖的四季：摄影爱好者的天堂", "从春天的黄花风铃木到冬天的落羽杉，松山湖的每一季都有独特的色彩。本文收集了摄影师笔下的松山湖四季美景...", "life", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=800", 980, 44, 15, 113.888, 22.898)
        );

        seedData.forEach(article -> {
            article.setAuthorId(1L); // 默认管理员
            article.setAuthorName("asd"); // 默认作者名
            article.setCreateTime(LocalDateTime.now().minusDays((int)(Math.random() * 10)));
            articleService.publishArticle(article);
        });

        return Result.success("成功导入 " + seedData.size() + " 篇文章");
    }

    private Article createArticle(String title, String summary, String category, String coverImage, int views, int likes, int comments, Double lng, Double lat) {
        Article article = new Article();
        article.setTitle(title);
        article.setSummary(summary);
        article.setContent("<p>" + summary + "</p><div class=\"embedded-chart-container\" data-config='{\"title\":\"文章热度\",\"type\":\"bar\",\"data\":[{\"name\":\"阅读\",\"value\":" + views + "},{\"name\":\"点赞\",\"value\":" + likes + "},{\"name\":\"评论\",\"value\":" + comments + "}]}'></div>");
        article.setCategory(category);
        article.setCoverImage(coverImage);
        article.setViews(views);
        article.setLikes(likes);
        article.setComments(comments);
        article.setLongitude(lng);
        article.setLatitude(lat);
        article.setStatus(1);
        return article;
    }
}
