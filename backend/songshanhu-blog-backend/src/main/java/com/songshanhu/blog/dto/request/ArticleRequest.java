package com.songshanhu.blog.dto.request;

import lombok.Data;

@Data
public class ArticleRequest {
    private String title;
    private String content;
    private String summary;
    private String category;
    private String coverImage;
    private Double longitude;
    private Double latitude;
    private Integer status;
}
