CREATE TABLE usage_cost (
    standingbook_id BIGINT NOT NULL COMMENT '台账ID',
    aggregate_time DATETIME NOT NULL COMMENT '生成时间',
    current_usage DECIMAL(20, 10) NOT NULL DEFAULT '0.0' COMMENT '当前用量',
    total_usage DECIMAL(20, 10) NOT NULL DEFAULT '0.0' COMMENT '截至当前总用量',
    cost DECIMAL(20, 10) NOT NULL DEFAULT '0.0' COMMENT '成本',
    discount DECIMAL(20, 10) NOT NULL DEFAULT '0.0' COMMENT '折价',
    energy_id BIGINT NOT NULL COMMENT '能源类型ID'
)
    UNIQUE KEY (standingbook_id, aggregate_time)
PARTITION BY RANGE (aggregate_time)
(
    PARTITION p2018 VALUES LESS THAN ('2019-01-01'),
    PARTITION p2019 VALUES LESS THAN ('2020-01-01'),
    PARTITION p2020 VALUES LESS THAN ('2021-01-01'),
    PARTITION p2021 VALUES LESS THAN ('2022-01-01'),
    PARTITION p2022 VALUES LESS THAN ('2023-01-01'),
    PARTITION p2023 VALUES LESS THAN ('2024-01-01'),
    PARTITION p2024 VALUES LESS THAN ('2025-01-01')
)
DISTRIBUTED BY HASH (standingbook_id) BUCKETS 32
PROPERTIES (
    "replication_num" = "3",
    "dynamic_partition.enable" = "true",
    "dynamic_partition.time_unit" = "YEAR",  -- 按年
    "dynamic_partition.start" = "-7",  -- 向前7年（比如2019年）
    "dynamic_partition.end" = "5",  -- 向后5年（比如2025年）
    "dynamic_partition.prefix" = "p",
    "dynamic_partition.buckets" = "32",
    "dynamic_partition.time_zone" = "Asia/Shanghai",
    "compression" = "lz4"
);
