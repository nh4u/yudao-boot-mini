CREATE TABLE `collect_raw_data` (
    `sync_time` datetime NOT NULL COMMENT "数据同步时间",
    `standingbook_id` bigint(20) NOT NULL COMMENT "台账id",
    `param_code` varchar(255) NOT NULL COMMENT "参数 code",
    `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数",
    `data_site` varchar(255) NULL COMMENT "OPC_DA:IO地址/",
    `calc_value` varchar(255) NULL COMMENT "公式计算值",
    `raw_value` varchar(255) NULL COMMENT "采集值（原始）",
    `collect_time` datetime NULL COMMENT "数据采集时间（原始）",
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间",
    `usage` int(11) NULL COMMENT "用量1，非用量0",
    `full_increment` int(11) NULL COMMENT "全量/增量（0：全量；1增量。）",
    `data_feature` int(11) NULL COMMENT "数据特征 1累计值2稳态值3状态值",
    `data_type` int(11) NULL COMMENT "数据类型 1数字2文本"
) ENGINE=OLAP
DUPLICATE KEY(`sync_time`, `standingbook_id`)
COMMENT "实时数据采集表"
PARTITION BY RANGE(`sync_time`) ()
DISTRIBUTED BY HASH(`sync_time`, `standingbook_id`) BUCKETS 16
PROPERTIES (
"compression" = "LZ4",
"datacache.enable" = "true",
"dynamic_partition.enable" = "true",
"dynamic_partition.end" = "3",
"dynamic_partition.history_partition_num" = "0",
"dynamic_partition.prefix" = "p",
"dynamic_partition.start" = "-15",
"dynamic_partition.time_unit" = "DAY",
"dynamic_partition.time_zone" = "Asia/Shanghai",
"enable_async_write_back" = "false",
"replication_num" = "6",
"storage_volume" = "builtin_storage_volume"
);;



CREATE TABLE `cop_hour_aggregate_data` (
   `aggregate_time` datetime NOT NULL COMMENT "聚合时间",
   `cop_type` varchar(40) NOT NULL COMMENT "低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS",
   `cop_value` decimal(30, 10) NULL COMMENT "公式计算值"
) ENGINE=OLAP
UNIQUE KEY(`aggregate_time`, `cop_type`)
COMMENT "cop根据公式聚合表（小时级）"
PARTITION BY RANGE(`aggregate_time`)()
DISTRIBUTED BY HASH(`aggregate_time`, `cop_type`) BUCKETS 16
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
"replication_num" = "6",
"storage_volume" = "builtin_storage_volume"
);;


CREATE TABLE `minute_aggregate_data` (
 `aggregate_time` datetime NOT NULL COMMENT "聚合时间",
 `param_code` varchar(255) NOT NULL COMMENT "参数 code",
 `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数",
 `standingbook_id` bigint(20) NOT NULL COMMENT "台账id",
 `data_site` varchar(255) NULL COMMENT "OPC_DA:IO地址/",
 `full_value` decimal(30, 10) NULL COMMENT "全量（累积值）",
 `incremental_value` decimal(30, 10) NULL COMMENT "增量（累积值）",
 `usage` int(11) NULL COMMENT "用量1，非用量0",
 `full_increment` int(11) NULL COMMENT "全量/增量（0：全量；1增量。）",
 `data_feature` int(11) NULL COMMENT "数据特征 1累计值2稳态值3状态值",
 `data_type` int(11) NULL COMMENT "数据类型 1数字2文本",
 `acq_flag` int(11) NULL COMMENT "业务点1，不是业务点0"
) ENGINE=OLAP
UNIQUE KEY(`aggregate_time`, `param_code`, `energy_flag`, `standingbook_id`)
COMMENT "聚合数据采集表（分钟级）"
PARTITION BY RANGE(`aggregate_time`)()
DISTRIBUTED BY HASH(`standingbook_id`) BUCKETS 16
PROPERTIES (
"bloom_filter_columns" = "param_code",
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
"replication_num" = "6",
"storage_volume" = "builtin_storage_volume"
);;

CREATE TABLE `usage_cost` (
  `standingbook_id` bigint(20) NOT NULL COMMENT "台账ID",
  `aggregate_time` datetime NOT NULL COMMENT "生成时间",
  `current_usage` decimal(30, 10) NOT NULL DEFAULT "0.0" COMMENT "当前用量",
  `total_usage` decimal(30, 10) NOT NULL DEFAULT "0.0" COMMENT "截至当前总用量",
  `cost` decimal(30, 10) NOT NULL DEFAULT "0.0" COMMENT "成本",
  `standard_coal_equivalent` decimal(30, 10) NOT NULL DEFAULT "0.0" COMMENT "折标煤",
  `energy_id` bigint(20) NOT NULL COMMENT "能源类型ID"
) ENGINE=OLAP
UNIQUE KEY(`standingbook_id`, `aggregate_time`)
COMMENT "用量成本计算结果"
PARTITION BY RANGE(`aggregate_time`)()
DISTRIBUTED BY HASH(`standingbook_id`) BUCKETS 32
PROPERTIES (
"compression" = "LZ4",
"datacache.enable" = "true",
"dynamic_partition.buckets" = "32",
"dynamic_partition.enable" = "true",
"dynamic_partition.end" = "3",
"dynamic_partition.history_partition_num" = "0",
"dynamic_partition.prefix" = "p",
"dynamic_partition.start" = "-3650",
"dynamic_partition.time_unit" = "DAY",
"dynamic_partition.time_zone" = "Asia/Shanghai",
"enable_async_write_back" = "false",
"replication_num" = "6",
"storage_volume" = "builtin_storage_volume"
);;