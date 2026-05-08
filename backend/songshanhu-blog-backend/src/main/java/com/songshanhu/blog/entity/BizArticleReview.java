package com.songshanhu.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_article_review")
public class BizArticleReview {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private String reviewer;

    private String result;

    private String reason;

    private Integer hitLevel;

    private String hitWords;

    private LocalDateTime createTime;
}

