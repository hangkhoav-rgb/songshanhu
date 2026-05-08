package com.songshanhu.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class EventReportRequest {
    @NotBlank
    private String event;

    private String page;

    private Map<String, Object> props;
}

