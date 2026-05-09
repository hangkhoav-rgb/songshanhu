-- 修复旧数据兼容：历史 status=1(发布) 迁移为 status=2(已发布)
-- V4 之后，status=1 的新语义是“待审核”；新提交会写 last_review_result='submit'
-- 因此这里仅迁移 last_review_result 为空的 status=1 记录，避免误把“待审核”迁移为“已发布”

SET @col_result := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE()
    AND table_name = 'article'
    AND column_name = 'last_review_result'
);

SET @sql_fix := IF(
  @col_result > 0,
  'UPDATE `article` SET `status` = 2 WHERE `status` = 1 AND (`last_review_result` IS NULL OR `last_review_result` = '''')',
  'UPDATE `article` SET `status` = 2 WHERE `status` = 1'
);

PREPARE stmt_fix FROM @sql_fix;
EXECUTE stmt_fix;
DEALLOCATE PREPARE stmt_fix;

