## 1, starrocks 表中minute表需要更换为uniquekey
```sql
CREATE TABLE `minute_aggregate_data` (
  `aggregate_time` datetime NOT NULL COMMENT "聚合时间",
  `param_code` varchar(255) NOT NULL COMMENT "参数 code",
  `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数",
  `standingbook_id` bigint(20) NOT NULL COMMENT "台账id",
  `data_site` varchar(255) NULL COMMENT "OPC_DA:IO地址/",
  `full_value` decimal(30, 10) NULL COMMENT "全量（累积值）",
  `incremental_value` decimal(30, 10) NULL COMMENT "增量（累积值）"
) ENGINE=OLAP 
UNIQUE KEY(`aggregate_time`, `param_code`, `energy_flag`, `standingbook_id`)
COMMENT "聚合数据采集表（分钟级）"
PARTITION BY RANGE(`aggregate_time`)()
DISTRIBUTED BY HASH(`standingbook_id`) BUCKETS 16 
PROPERTIES (
"compression" = "LZ4",
"datacache.enable" = "true",
"dynamic_partition.enable" = "true",
"dynamic_partition.end" = "3",
"dynamic_partition.history_partition_num" = "0",
"dynamic_partition.prefix" = "p",
"dynamic_partition.start" = "-3650",
"dynamic_partition.time_unit" = "DAY",
"dynamic_partition.time_zone" = "Asia/Shanghai",
"enable_async_write_back" = "false",
"replication_num" = "3",
"storage_volume" = "builtin_storage_volume"
);;
```
## 2.更新普通角色的数据权限为全部数据范围
```
update system_role set data_scope = 1 where code = 'common';
```
## 3.更新金额和用量数据范围为decimal(30,10)

### acquisition 数据库
```sql
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

```
### ydme_ems 数据库
```sql
ALTER TABLE ems_additional_recording MODIFY COLUMN this_value DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '本次数值';
ALTER TABLE ems_additional_recording MODIFY COLUMN pre_value DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '上次采集值';
ALTER TABLE ems_price_detail MODIFY COLUMN `usage_min` DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '档位用量下限';
ALTER TABLE ems_price_detail MODIFY COLUMN `usage_max` DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '档位用量上限';
ALTER TABLE ems_price_detail MODIFY COLUMN `unit_price` DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '单价';
ALTER TABLE ems_voucher MODIFY COLUMN `price` DECIMAL ( 30, 10 ) DEFAULT '0.00000' COMMENT '金额';
ALTER TABLE ems_voucher MODIFY COLUMN `usage` DECIMAL ( 30, 10 ) DEFAULT '0.00000' COMMENT '用量';
```