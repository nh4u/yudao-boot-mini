-- 修改 current_usage 字段类型
ALTER TABLE usage_cost
    MODIFY COLUMN current_usage DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT "当前用量";

-- 修改 total_usage 字段类型
ALTER TABLE usage_cost
    MODIFY COLUMN total_usage DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT "截至当前总用量";

-- 修改 current_usage 字段类型
ALTER TABLE usage_cost
    MODIFY COLUMN `cost` decimal(30, 10) NOT NULL DEFAULT "0.0" COMMENT "成本";

ALTER TABLE usage_cost
    MODIFY COLUMN `standard_coal_equivalent` decimal(30, 10) NOT NULL DEFAULT "0.0" COMMENT "折标煤";