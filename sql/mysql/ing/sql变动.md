## 1, starrocks 表中minute表需要更换为uniquekey
```sql
CREATE TABLE `minute_aggregate_data` (
  `aggregate_time` datetime NOT NULL COMMENT "聚合时间",
  `param_code` varchar(255) NOT NULL COMMENT "参数 code",
  `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数",
  `standingbook_id` bigint(20) NOT NULL COMMENT "台账id",
  `data_site` varchar(255) NULL COMMENT "OPC_DA:IO地址/",
  `full_value` decimal(20, 10) NULL COMMENT "全量（累积值）",
  `incremental_value` decimal(20, 10) NULL COMMENT "增量（累积值）"
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