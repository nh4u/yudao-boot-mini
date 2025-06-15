CREATE TABLE usage_cost (
    standingbook_id BIGINT NOT NULL COMMENT '台账ID',
    aggregate_time DATETIME NOT NULL COMMENT '生成时间',
    current_usage DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT '当前用量',
    total_usage DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT '截至当前总用量',
    cost DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT '成本',
    standard_coal_equivalent DECIMAL(30, 10) NOT NULL DEFAULT '0.0' COMMENT '折标煤',
    energy_id BIGINT NOT NULL COMMENT '能源类型ID'
)
 UNIQUE KEY (standingbook_id, aggregate_time)
    COMMENT "用量成本计算结果"
PARTITION BY RANGE (aggregate_time)()
DISTRIBUTED BY HASH (standingbook_id) BUCKETS 32
PROPERTIES (
    "replication_num" = "3",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "DAY",  -- 按年
    "dynamic_partition.start" = "-3650",  -- 向前7年（比如2019年）
    "dynamic_partition.end" = "3",  -- 向后5年（比如2025年）
    "dynamic_partition.prefix" = "p",
    "dynamic_partition.buckets" = "32",
    "dynamic_partition.time_zone" = "Asia/Shanghai",
    "compression" = "lz4"
);
