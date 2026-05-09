package com.songshanhu.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.songshanhu.blog.dto.response.DashboardStatsResponse;
import com.songshanhu.blog.entity.Article;

public interface IArticleService extends IService<Article> {
    Page<Article> getArticlePage(int current, int size, String category, String keyword, Long authorId, Integer statusFilter);
    Article getArticleById(Long id);
    void publishArticle(Article article);
    boolean resubmitArticle(Long articleId, Long authorId);
    Page<Article> getAdminArticlePage(int current, int size, String keyword, String authorName, Integer status);
    boolean adminApprove(Long articleId, String reviewer, String reason);
    boolean adminReject(Long articleId, String reviewer, String reason);
    boolean adminOffline(Long articleId, String reviewer, String reason);
    DashboardStatsResponse getStatsByAuthor(Long authorId);
}
