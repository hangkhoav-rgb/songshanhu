package com.songshanhu.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String content;
    
    private String summary;
    
    private String coverImage;
    
    private String category;
    
    private Long authorId;
    
    private String authorName;
    
    private Integer views;
    
    private Integer comments;
    
    private Integer likes;
    
    private Integer status;

    private String lastReviewReason;

    private LocalDateTime lastReviewTime;

    private String lastReviewResult;
    
    private Double longitude;
    
    private Double latitude;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
