use acquisition;
-- 修改 total_usage 字段类型
ALTER TABLE usage_cost
    MODIFY COLUMN total_usage DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT "截至当前总用量";