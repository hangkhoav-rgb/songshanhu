package com.songshanhu.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_user_punish_log")
public class BizUserPunishLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String action;

    private String reason;

    private LocalDateTime banEndTime;

    private String createBy;

    private LocalDateTime createTime;
}

