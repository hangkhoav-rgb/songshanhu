package com.songshanhu.blog.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DashboardSummaryResponse {
    private long likeCount;
    private long collectCount;
    private LocalDateTime lastActionTime;
}

