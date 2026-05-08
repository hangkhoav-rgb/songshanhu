package com.songshanhu.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.songshanhu.blog.entity.Article;
import com.songshanhu.blog.entity.Comment;
import com.songshanhu.blog.mapper.CommentMapper;
import com.songshanhu.blog.service.IArticleService;
import com.songshanhu.blog.service.ICommentService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements ICommentService {

    private final IArticleService articleService;

    public CommentServiceImpl(@Lazy IArticleService articleService) {
        this.articleService = articleService;
    }

    @Override
    public List<Comment> getCommentsByArticleId(Long articleId) {
        return list(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .orderByDesc(Comment::getCreateTime));
    }

    @Override
    @Transactional
    public void addComment(Comment comment) {
        save(comment);
        // 更新文章评论数，增加空值检查
        Article article = articleService.getById(comment.getArticleId());
        if (article != null) {
            Integer currentComments = article.getComments();
            article.setComments(currentComments == null ? 1 : currentComments + 1);
            articleService.updateById(article);
        }
    }
}
