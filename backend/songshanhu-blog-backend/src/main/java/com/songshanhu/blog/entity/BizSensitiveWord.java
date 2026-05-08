package com.songshanhu.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_sensitive_word")
public class BizSensitiveWord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    private String category;

    private Integer level;

    private String matchType;

    private Integer status;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;
}

