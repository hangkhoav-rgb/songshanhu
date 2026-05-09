-- 创建数据库
CREATE DATABASE IF NOT EXISTS songshanhu_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE songshanhu_blog;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `gender` varchar(10) DEFAULT 'secret' COMMENT '性别: male/female/secret',
  `bio` varchar(255) DEFAULT NULL COMMENT '个人简介',
  `extra` json DEFAULT NULL COMMENT '扩展字段(JSON)',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `role` varchar(20) DEFAULT 'ROLE_USER' COMMENT '角色',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_nickname` (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `content` longtext NOT NULL COMMENT '富文本内容',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图',
  `category` varchar(50) DEFAULT '全部' COMMENT '分类',
  `author_id` bigint(20) NOT NULL COMMENT '作者ID',
  `author_name` varchar(50) NOT NULL COMMENT '作者名',
  `views` int(11) DEFAULT '0' COMMENT '阅读量',
  `comments` int(11) DEFAULT '0' COMMENT '评论数',
  `likes` int(11) DEFAULT '0' COMMENT '点赞数',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态: 0-草稿, 1-待审核, 2-已发布, 3-驳回, 4-下架',
  `last_review_reason` varchar(500) DEFAULT NULL COMMENT '最近一次审核原因',
  `last_review_time` datetime DEFAULT NULL COMMENT '最近一次审核时间',
  `last_review_result` varchar(20) DEFAULT NULL COMMENT '最近一次审核结果(approve/reject/offline/submit)',
  `longitude` double DEFAULT NULL COMMENT '经度',
  `latitude` double DEFAULT NULL COMMENT '纬度',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint(20) NOT NULL COMMENT '文章ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `user_avatar` varchar(255) DEFAULT NULL COMMENT '用户头像',
  `content` text NOT NULL COMMENT '评论内容',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_article_id` (`article_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 点赞表（记录用户点赞时间）
CREATE TABLE IF NOT EXISTS `article_like` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint(20) NOT NULL COMMENT '文章ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_article_time` (`article_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- 收藏表（记录用户收藏时间）
CREATE TABLE IF NOT EXISTS `article_collect` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint(20) NOT NULL COMMENT '文章ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
  KEY `idx_user_time` (`user_id`, `create_time`),
  KEY `idx_article_time` (`article_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章收藏表';

-- 文章审核记录表
CREATE TABLE IF NOT EXISTS `biz_article_review` (
  `id` bigint(20) not null auto_increment,
  `article_id` bigint(20) not null,
  `reviewer` varchar(64) default '',
  `result` varchar(20) not null,
  `reason` varchar(500) default null,
  `hit_level` tinyint(1) default null,
  `hit_words` varchar(2000) default null,
  `create_time` datetime default current_timestamp,
  primary key (id),
  key idx_biz_article_review_article (article_id),
  key idx_biz_article_review_result (result),
  key idx_biz_article_review_time (create_time)
) engine=innodb default charset=utf8mb4 comment='文章审核记录';
