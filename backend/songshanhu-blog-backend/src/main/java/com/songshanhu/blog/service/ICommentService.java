package com.songshanhu.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.songshanhu.blog.entity.Comment;
import java.util.List;

public interface ICommentService extends IService<Comment> {
    List<Comment> getCommentsByArticleId(Long articleId);
    void addComment(Comment comment);
}
