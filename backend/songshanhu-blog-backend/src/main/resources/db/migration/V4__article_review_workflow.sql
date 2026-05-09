-- 文章“先审后发”工作流：状态机扩展 + 旧数据迁移 + 审核记录

SET @has_published := (SELECT COUNT(*) FROM `article` WHERE `status` = 2);
SET @has_old_published := (SELECT COUNT(*) FROM `article` WHERE `status` = 1);

-- 仅当库里没有 status=2 且存在 status=1 时，认为是旧数据（status=1=发布），迁移为 status=2=已发布
SET @sql_migrate := IF(@has_published = 0 AND @has_old_published > 0,
  'UPDATE `article` SET `status` = 2 WHERE `status` = 1',
  'SELECT 1'
);
PREPARE stmt_migrate FROM @sql_migrate;
EXECUTE stmt_migrate;
DEALLOCATE PREPARE stmt_migrate;

-- 扩展状态字段定义（tinyint）
ALTER TABLE `article`
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-待审核, 2-已发布, 3-驳回, 4-下架';

-- 冗余字段：便于作者列表展示最近一次审核结论
SET @col_reason := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'article' AND column_name = 'last_review_reason');
SET @sql_reason := IF(@col_reason = 0,
  'ALTER TABLE `article` ADD COLUMN `last_review_reason` VARCHAR(500) DEFAULT NULL COMMENT ''最近一次审核原因''',
  'SELECT 1'
);
PREPARE stmt_reason FROM @sql_reason;
EXECUTE stmt_reason;
DEALLOCATE PREPARE stmt_reason;

SET @col_time := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'article' AND column_name = 'last_review_time');
SET @sql_time := IF(@col_time = 0,
  'ALTER TABLE `article` ADD COLUMN `last_review_time` DATETIME DEFAULT NULL COMMENT ''最近一次审核时间''',
  'SELECT 1'
);
PREPARE stmt_time FROM @sql_time;
EXECUTE stmt_time;
DEALLOCATE PREPARE stmt_time;

SET @col_result := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE table_schema = DATABASE() AND table_name = 'article' AND column_name = 'last_review_result');
SET @sql_result := IF(@col_result = 0,
  'ALTER TABLE `article` ADD COLUMN `last_review_result` VARCHAR(20) DEFAULT NULL COMMENT ''最近一次审核结果(approve/reject/offline/submit)''',
  'SELECT 1'
);
PREPARE stmt_result FROM @sql_result;
EXECUTE stmt_result;
DEALLOCATE PREPARE stmt_result;

-- 审核记录表
CREATE TABLE IF NOT EXISTS `biz_article_review` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `article_id` BIGINT(20) NOT NULL,
  `reviewer` VARCHAR(64) DEFAULT '',
  `result` VARCHAR(20) NOT NULL,
  `reason` VARCHAR(500) DEFAULT NULL,
  `hit_level` TINYINT(1) DEFAULT NULL,
  `hit_words` VARCHAR(2000) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_biz_article_review_article` (`article_id`),
  KEY `idx_biz_article_review_result` (`result`),
  KEY `idx_biz_article_review_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章审核记录';

