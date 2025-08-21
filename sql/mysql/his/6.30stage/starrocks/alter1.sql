use acquisition;
-- 修改 current_usage 字段类型
ALTER TABLE usage_cost
    MODIFY COLUMN current_usage DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT "当前用量";