package com.songshanhu.blog.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleActionItem {
    private Long id;
    private String title;
    private String summary;
    private String coverImage;
    private String category;
    private Long authorId;
    private String authorName;
    private Integer views;
    private Integer comments;
    private Integer likes;

    private LocalDateTime likedAt;
    private LocalDateTime collectedAt;
    private LocalDateTime actionTime;
}

