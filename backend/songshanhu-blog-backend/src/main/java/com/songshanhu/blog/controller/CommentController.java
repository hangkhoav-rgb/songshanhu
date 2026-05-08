package com.songshanhu.blog.controller;

import com.songshanhu.blog.dto.request.CommentRequest;
import com.songshanhu.blog.entity.Comment;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.service.ICommentService;
import com.songshanhu.blog.service.IUserService;
import com.songshanhu.blog.util.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comment")
@Tag(name = "评论管理", description = "文章评论接口")
public class CommentController {

    private final ICommentService commentService;
    private final IUserService userService;

    public CommentController(ICommentService commentService, IUserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @Operation(summary = "获取文章评论列表")
    @GetMapping("/list/{articleId}")
    public Result<List<Comment>> list(@PathVariable Long articleId) {
        return Result.success(commentService.getCommentsByArticleId(articleId));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/add")
    public Result<?> add(@RequestBody CommentRequest request) {
        // 获取当前登录用户
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = ((UserDetails) principal).getUsername();
        User user = userService.findByUsername(username);

        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setContent(request.getContent());
        comment.setUserId(user.getId());
        comment.setUsername(user.getNickname() != null ? user.getNickname() : user.getUsername());
        comment.setUserAvatar(user.getAvatar());

        commentService.addComment(comment);
        return Result.success(comment);
    }
}
