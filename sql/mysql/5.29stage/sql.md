## starrocks 密码：f6158df6cd0b81d228e315d568f20b7f
1. **starrocks建库**
```bash
mysql -h 192.168.91.61 -P 9030 -u root

CREATE DATABASE acuisition
CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```
2. **starrocks建表**
```sql
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
  `usage` int(11) NULL COMMENT "用量1，非用量0"
) ENGINE=OLAP
DUPLICATE KEY(`sync_time`, `standingbook_id`)
COMMENT "实时数据采集表"
PARTITION BY RANGE (`sync_time`) ()
DISTRIBUTED BY HASH(`sync_time`) BUCKETS 16
PROPERTIES (
  "replication_num" = "3",
  "storage_type" = "COLUMN",
  "dynamic_partition.enable" = "true",
  "dynamic_partition.time_unit" = "DAY",
  "dynamic_partition.time_zone" = "Asia/Shanghai",
  "dynamic_partition.start" = "-15",
  "dynamic_partition.end" = "3",
  "dynamic_partition.prefix" = "p"
);

CREATE TABLE `minute_aggregate_data` (
`aggregate_time` datetime NOT NULL COMMENT "聚合时间",
  `param_code` varchar(255) NOT NULL COMMENT "参数 code",
  `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数",
  `standingbook_id` bigint(20) NOT NULL COMMENT "台账id",
  `data_site` varchar(255) NULL COMMENT "OPC_DA:IO地址/",
  `full_value` decimal(20, 10) NULL COMMENT "全量（累积值）",
  `incremental_value` decimal(20, 10) NULL COMMENT "增量（累积值）"
) ENGINE=OLAP
DUPLICATE KEY(`aggregate_time`, `param_code`, `energy_flag`, `standingbook_id`)
COMMENT "聚合数据采集表（分钟级）"
PARTITION BY RANGE (`aggregate_time`) ()
DISTRIBUTED BY HASH(`standingbook_id`) BUCKETS 16
PROPERTIES (
  "replication_num" = "3",
  "storage_type" = "COLUMN",
  "dynamic_partition.enable" = "true",
  "dynamic_partition.time_unit" = "DAY",
  "dynamic_partition.time_zone" = "Asia/Shanghai",
  "dynamic_partition.start" = "-3650",
  "dynamic_partition.end" = "3",
  "dynamic_partition.prefix" = "p"
);

CREATE TABLE `usage_cost` (
  `standingbook_id` bigint(20) NOT NULL COMMENT "台账ID",
  `aggregate_time` datetime NOT NULL COMMENT "生成时间",
  `current_usage` decimal(20, 10) NOT NULL DEFAULT "0.0" COMMENT "当前用量",
  `total_usage` decimal(20, 10) NOT NULL DEFAULT "0.0" COMMENT "截至当前总用量",
  `cost` decimal(20, 10) NOT NULL DEFAULT "0.0" COMMENT "成本",
  `standard_coal_equivalent` decimal(20, 10) NOT NULL DEFAULT "0.0" COMMENT "折标煤",
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
"replication_num" = "3",
"storage_volume" = "builtin_storage_volume"
);;
```

3. **mysql建库**

链接信息：192.168.91.65:3306  用户名：root  密码：f6158df6cd0b81d228e315d568f20b7f

```bash
CREATE DATABASE ydme_ems
CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
CREATE DATABASE ydme_ems_acquisition
CHARACTER SET utf8mb4
COLLATE utf8mb4_general_ci;
```

4. **选择ydme_ems_acquisition数据库，执行acquisition.sql**

5. **选择ydme_ems数据库，执行init.sql**

   以下为重点内置数据，1.邮箱配置2.minio桶名accesskey 3.管理员信息配置

```sql
-- 重点内置数据，1.邮箱配置2.minio桶名accesskey 3.管理员信息配置
INSERT INTO `system_mail_account` (`id`, `mail`, `username`, `password`, `host`, `port`, `ssl_enable`, `starttls_enable`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1, 'lrx123581321@163.com', 'lrx123581321@163.com', 'KTfrN45U6yq9xccX', 'smtp.163.com', 465, b'1', b'0', '1', '2023-01-25 17:39:52', '1', '2025-04-08 10:18:43', b'0');
-- ----------------------------
-- Records of infra_file_config minio需要开启公有域
-- ----------------------------
INSERT INTO `infra_file_config` VALUES (1, 'minio', 20, '', b'1', '{\"@class\":\"cn.bitlinks.ems.module.infra.framework.file.core.client.s3.S3FileClientConfig\",\"endpoint\":\"http://192.168.91.64:9000\",\"domain\":\"http://192.168.91.64:9000/ydems\",\"bucket\":\"ydems\",\"accessKey\":\"24ec408394b774c8qsde\",\"accessSecret\":\"41385f1624ec408394b774c88364f457j9ye73nh\"}', '1', '2024-01-13 22:11:12', '1', '2025-05-22 10:12:17', b'0');
-- ----------------------------
-- Records of system_users
-- ----------------------------
INSERT INTO `system_users` VALUES (1, 'admin', '$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm', 'admin', '管理员', 1, '[]', 'xx@qq.com', '', 2, '', 0, '127.0.0.1', '2025-05-28 11:21:43', 'admin', '2021-01-05 17:03:47', NULL, '2025-05-28 19:11:39', b'0', 1);

```

