package com.songshanhu.blog.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardActionVO {
    private Long articleId;
    private String title;
    private String actionType;
    private LocalDateTime actionTime;
}

