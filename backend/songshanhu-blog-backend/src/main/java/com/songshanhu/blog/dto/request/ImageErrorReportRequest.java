package com.songshanhu.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageErrorReportRequest {
    @NotBlank
    private String url;
    private String page;
    private String userAgent;
    private String referrer;
    private String message;
}

