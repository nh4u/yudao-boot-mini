## 1, starrocks 表中minute表需要更换为uniquekey
```sql
CREATE TABLE `minute_aggregate_data` (
  `aggregate_time` datetime NOT NULL COMMENT "聚合时间",
  `param_code` varchar(255) NOT NULL COMMENT "参数 code",
  `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数",
  `standingbook_id` bigint(20) NOT NULL COMMENT "台账id",
  `data_site` varchar(255) NULL COMMENT "OPC_DA:IO地址/",
  `full_value` decimal(30, 10) NULL COMMENT "全量（累积值）",
  `incremental_value` decimal(30, 10) NULL COMMENT "增量（累积值）",
  `data_feature` int(11) NULL COMMENT "数据特征",
  `data_type` int(11) NULL COMMENT "数据类型",
  `usage` int(11) NULL COMMENT "用量1，非用量0"
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

## 4.补录添加字段
```sql
ALTER TABLE collect_raw_data ADD COLUMN `full_increment` INT ( 4 ) DEFAULT NULL COMMENT '全量/增量（0：全量；1增量。）';
ALTER TABLE collect_raw_data ADD COLUMN `data_feature` INT ( 1 ) DEFAULT NULL COMMENT '数据特征 1累计值2稳态值3状态值';
ALTER TABLE collect_raw_data ADD COLUMN `data_type` INT ( 1 ) DEFAULT NULL COMMENT '数据类型 1数字2文本';

ALTER TABLE minute_aggregate_data ADD COLUMN `full_increment` INT ( 4 ) DEFAULT NULL COMMENT '全量/增量（0：全量；1增量。）';
ALTER TABLE minute_aggregate_data ADD COLUMN `data_feature` INT ( 1 ) DEFAULT NULL COMMENT '数据特征 1累计值2稳态值3状态值';
ALTER TABLE minute_aggregate_data ADD COLUMN `data_type` INT ( 1 ) DEFAULT NULL COMMENT '数据类型 1数字2文本';
ALTER TABLE minute_aggregate_data ADD COLUMN `usage` int(11) NULL COMMENT "用量1，非用量0";
```
### 5. cop
#### starrocks库
```sql
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
"replication_num" = "3",
"storage_volume" = "builtin_storage_volume"
);;
```
#### ydme_ems库
```sql
CREATE TABLE `power_cop_settings` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `cop_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS',
  `data_feature` int(4) NOT NULL COMMENT '数据特征 1累计值2稳态值3状态值',
  `param` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公式参数',
  `param_cn_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公式参数对应能源参数中文名',
  `standingbook_id` bigint(20) DEFAULT NULL COMMENT '台账id',
  `creator` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_standingbook_id` (`standingbook_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=69 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='cop报表公式参数配置';
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 1, 'm1', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 1, 'm2', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 1, 'm3', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 1, 'm4', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 1, 'm5', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 1, 'm6', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 1, 'm7', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't1', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't2', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't3', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't4', '瞬时温度',  1);

INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTC', 1, 'W1', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTC', 1, 'W2', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTC', 1, 'W3', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm1', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm2', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm3', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm4', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm5', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm6', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm7', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't1', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't2', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't3', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't4', '瞬时温度',  1);

INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'W1', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'W2', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'W3', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'W4', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'W5', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'W6', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'm1', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'm2', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'm3', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'm4', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'm5', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'm6', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'm7', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 2, 't1', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 2, 't2', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'W1', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'W2', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'W3', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'W4', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTC', 1, 'W5', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'm1', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'm2', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'm3', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'm4', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'm5', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'm6', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'm7', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 2, 't1', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 2, 't2', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W1', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W2', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W3', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W4', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W5', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W6', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W7', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W8', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W9', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W10', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W11', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W12', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W13', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W14', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W15', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W16', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'MTS', 1, 'W17', '正向有功电能',  1);
CREATE TABLE `power_cop_formula` (
 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
 `cop_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS',
 `formula` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公式',
 `creator` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `updater` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
 `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
 `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
 PRIMARY KEY (`id`) USING BTREE,
 KEY `idx_cop_type` (`cop_type`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=DYNAMIC COMMENT='cop报表公式';
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `tenant_id` )
VALUES
   ( 'LTC', '4.2*(m1+m2+m3+m4)*(min(t1,t2,t3,t4)-t5)/((W1+W2+W3)*3.6)', 1 );
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `tenant_id` )
VALUES
   ( 'LTS', '4.2*(m1+m2+m3+m4)*(min(t1,t2,t3,t4)-t5)/((W1+W2+W3+W4+W5+W6)*3.6)', 1 );
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `tenant_id` )
VALUES
   ( 'MTC', '4.2*(m1+m2+m3+m4+m5+m6+m7)*(t1-t2)/((W1+W2+W3+W4+W5)*3.6)', 1 );
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `tenant_id` )
VALUES
   ( 'MTS', '4.2*(m1+m2+m3+m4+m5+m6+m7)*(t1-t2)/((W1+W2+W3+W4+W5+W6+W7+W8+W9+W10+W11+W12+W13+W14+W15+W16+W17)*3.6)', 1 );

INSERT INTO `system_dict_type` ( `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES ('COP系统类型', 'cop_type', 0, 'COP系统类型', '1', '2025-05-09 13:40:13', '1', '2025-06-22 16:27:56', b'0', '2025-05-12 15:34:02');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 1, '低温冷机', 'LTC', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:28:41', '1', '2025-06-22 16:28:41', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 2, '低温系统', 'LTS', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:28:52', '1', '2025-06-22 16:28:52', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 3, '中温冷机', 'MTC', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:29:11', '1', '2025-06-22 16:29:11', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (4, '中温系统', 'MTS', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:29:23', '1', '2025-06-22 16:29:23', b'0');

```
### 6.补录
#### 采集点

```sql
ALTER TABLE minute_aggregate_data ADD COLUMN `acq_flag` INT ( 11 ) NULL COMMENT "业务点1，不是业务点0";

```

### 6.表格header和系统code映射关系表
#### 建表语句

```sql
-- ----------------------------
-- Table structure for ems_header_code_mapping
-- ----------------------------
DROP TABLE IF EXISTS `ems_header_code_mapping`;
CREATE TABLE `ems_header_code_mapping`  (
                                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                            `header_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表头code',
                                            `header` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '表头',
                                            `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统台账code',
                                            `type` int(4) NOT NULL DEFAULT 1 COMMENT '类型0：去空串完全匹配；1：去空串首部匹配；2：去空串尾部匹配；5：去尾部-完全匹配；6：去尾部-首部匹配；7：去尾部-尾部匹配；8：未匹配到。',
                                            `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                            `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 426 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ems_header_code_mapping
-- ----------------------------
INSERT INTO `ems_header_code_mapping` VALUES (3, 'L1N3UPS-1A-2', 'L1N3UPS-1A-2 备用 仪表 正向Ep', 'L1N3UPS-1A-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (4, 'L1N2UPS-1A', 'L1N2UPS-1A BUS-L1-U2-F2-01工艺母线 仪表 正向Ep', 'L1N2UPS-1A', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (5, 'L1N22EA', 'L1N22EA BUS-L2E-N2-F2-01工艺母线 仪表 正向Ep', 'L1N22EA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (6, 'L1N21FA', 'L1N21FA BUS-L1F-N2-F2-01工艺母线 仪表 正向Ep', 'L1N21FA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (7, 'L1N22FB-1', 'L1N22FB-1 备用 仪表 正向Ep', 'L1N22FB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (8, 'L1N22FB-2', 'L1N22FB-2 备用 仪表 正向Ep', 'L1N22FB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (9, 'L1N22FA', 'L1N22FA BUS-L2F-N2-F2-01工艺母线 仪表 正向Ep', 'L1N22FA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (10, 'L1N3UPS-1A-1', 'L1N3UPS-1A-1 BUS-L1-U3-F2-01工艺母线 仪表 正向Ep', 'L1N3UPS-1A-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (11, 'L1N21EA', 'L1N21EA BUS-L1E-N2-F2-01工艺母线 仪表 正向Ep', 'L1N21EA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (12, 'L1N22EB-1', 'L1N22EB-1 备用 仪表 正向Ep', 'L1N22EB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (13, 'L1N22EB-2', 'L1N22EB-2 备用 仪表 正向Ep', 'L1N22EB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (14, 'R1N21CA', 'R1N21CA BUS-R1C-N2-F2-01工艺母线 仪表 正向Ep', 'R1N21CA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (15, 'R1N22CB-1', 'R1N22CB-1 备用 仪表 正向Ep', 'R1N22CB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (16, 'R1N22CB-2', 'R1N22CB-2 备用 仪表 正向Ep', 'R1N22CB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (17, 'R1N22CA', 'R1N22CA BUS-R2C-N2-F2-01工艺母线 仪表 正向Ep', 'R1N22CA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (18, 'R1N22DA', 'R1N22DA BUS-R1D-N2-F2-01工艺母线 仪表 正向Ep', 'R1N22DA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (19, 'R1N22DB-1', 'R1N22DB-1 备用 仪表 正向Ep', 'R1N22DB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (20, 'R1N22DB-2', 'R1N22DB-2 备用 仪表 正向Ep', 'R1N22DB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (21, 'R1N21DA', 'R1N21DA BUS-R1D-N2-F2-01工艺母线 仪表 正向Ep', 'R1N21DA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (22, 'R1N31AA-1', 'R1N31AA-1 BUS-R1A-N3-F1-01工艺母线 仪表 正向Ep', 'R1N31AA-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (23, 'R1N31AA-2', 'R1N31AA-2 备用 仪表 正向Ep', 'R1N31AA-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (24, 'R1N31AB-1', 'R1N31AB-1 R1N3UPS-1UPS 仪表 正向Ep', 'R1N31AB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (25, 'R1N31AB-2', 'R1N31AB-2 L1N3UPS-1UPS 仪表 正向Ep', 'R1N31AB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (26, 'R1N32AB-1', 'R1N32AB-1 R1N3UPS-1UPS静态旁路 仪表 正向Ep', 'R1N32AB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (27, 'R1N32AB-2', 'R1N32AB-2 L1N3UPS-1UPS静态旁路 仪表 正向Ep', 'R1N32AB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (28, 'R1N32AA', 'R1N32AA BUS-R2A-N3-F2-01工艺母线 仪表 正向Ep', 'R1N32AA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (29, 'R1N31BB-2', 'R1N31BB-2 备用 仪表 正向Ep', 'R1N31BB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (30, 'R1N31BA-1', 'R1N31BA-1 BUS-R1B-N3-F2-02工艺母线 仪表 正向Ep', 'R1N31BA-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (31, 'R1N31BA-2', 'R1N31BA-2 备用 仪表 正向Ep', 'R1N31BA-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (32, 'R1E32GA-1', 'R1E32GA-1 备用 仪表 正向Ep', 'R1E32GA-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (33, 'R1E32GA-2', 'R1E32GA-2 备用 仪表 正向Ep', 'R1E32GA-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (34, 'R1E32GB-1', 'R1E32GB-1 R1E3UPS-1UPS主进线 仪表 正向Ep', 'R1E32GB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (35, 'R1E32GB-2', 'R1E32GB-2 L-1-SEX-E-1 SEX配电总箱 (E) 仪表 正向Ep', 'R1E32GB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (36, 'R1E32GB-3', 'R1E32GB-3 R-1-FFU-E-1 FFU (E) 仪表 正向Ep', 'R1E32GB-3', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (37, 'R1N31GC-1', 'R1N31GC-1 自带N电纯水系统 仪表 正向Ep', 'R1N31GC-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (38, 'R1N31GC-2', 'R1N31GC-2 自带N电特气系统 仪表 正向Ep', 'R1N31GC-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (39, 'R1N31GC-3', 'R1N31GC-3 R-1-FFU-N-1 FFU (N) 仪表 正向Ep', 'R1N31GC-3', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (40, 'R1N31GC-4', 'R1N31GC-4 L-1-PPB-N-1杂项配电总箱(N)仪表 正向Ep', 'R1N31GC-4', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (41, 'R1N31GB-1', 'R1N31GB-1 R1N3UPS-2 UPS检修旁路 仪表 正向Ep', 'R1N31GB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (42, 'R1N32BA', 'R1N32BA BUS-R2B-N3-F2-01工艺母线 仪表 正向Ep', 'R1N32BA', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (43, 'R1N32BB-1', 'R1N32BB-1 备用 仪表 正向Ep', 'R1N32BB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (44, 'R1N32BB-2', 'R1N32BB-2 备用 仪表 正向Ep', 'R1N32BB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (45, 'R1N31BB-1', 'R1N31BB-1 BUS-R1B-N3-F2-01工艺母线 仪表 正向Ep', 'R1N31BB-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (46, 'R1N31GB-2', 'R1N31GB-2 L-1-AEX-N-1 AEX配电总箱(N)仪表 正向Ep', 'R1N31GB-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (47, 'R1N31GB-3', 'R1N31GB-3 备用 仪表 正向Ep', 'R1N31GB-3', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (48, 'R1N31GA-1', 'R1N31GA-1 备用 仪表 正向Ep', 'R1N31GA-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (49, 'R1N31GA-2', 'R1N31GA-2 备用 仪表 正向Ep', 'R1N31GA-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (50, 'R1N31GA-3', 'R1N31GA-3 自带N电废水系统 仪表 正向Ep', 'R1N31GA-3', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (51, 'R1N31GA-4', 'R1N31GA-4 备用 仪表 正向Ep', 'R1N31GA-4', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (52, 'R1N3UPS-1A-1', 'R1N3UPS-1A-1 BUS-R1-U3-F2-01工艺母线 仪表 正向Ep', 'R1N3UPS-1A-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (53, 'R1N3UPS-1A-2', 'R1N3UPS-1A-2 备用 仪表 正向Ep', 'R1N3UPS-1A-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (54, 'R1N2UPS-1A', 'R1N2UPS-1A BUS-R1-U2-F2-01工艺母线 仪表 正向Ep', 'R1N2UPS-1A', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (55, 'R1E3UPS-1B-1', 'R1E3UPS-1B-1 自带U电纯水系统 仪表 正向Ep', 'R1E3UPS-1B-1', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (56, 'R1E3UPS-1B-2', 'R1E3UPS-1B-2 R-1-PPB-U-1 U电总箱 仪表 正向Ep', 'R1E3UPS-1B-2', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (57, 'R1E3UPS-1B-3', 'R1E3UPS-1B-3 自带U电废水系统 仪表 正向Ep', 'R1E3UPS-1B-3', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (58, 'R1E3UPS-1B-4', 'R1E3UPS-1B-4 备用 仪表 正向Ep', 'R1E3UPS-1B-4', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (59, 'R1E3UPS-1B-5', 'R1E3UPS-1B-5 备用 仪表 正向Ep', 'R1E3UPS-1B-5', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (60, 'R1E3UPS-1B-6', 'R1E3UPS-1B-6 备用 仪表 正向Ep', 'R1E3UPS-1B-6', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (61, 'R1E3UPS-1A', 'R1E3UPS-1A 备用 仪表 正向Ep', 'R1E3UPS-1A', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (62, 'L301F2-a', 'L301F2-a UPS 201-5# 正向有功电能', 'L301F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (63, 'L301F2-b', 'L301F2-b UPS 202旁路 正向有功电能', 'L301F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (64, 'L301F3-a', 'L301F3-a UPS 203-1# 正向有功电能', 'L301F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (65, 'L301F3-b', 'L301F3-b 备用 正向有功电能', 'L301F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (66, 'L301F4-a', 'L301F4-a UPS 205-11# 正向有功电能', 'L301F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (67, 'L301F4-b', 'L301F4-b UPS 204旁路 正向有功电能', 'L301F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (68, 'L302F2-a', 'L302F2-a UPS 202-2# 正向有功电能', 'L302F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (69, 'L302F2-b', 'L302F2-b UPS 203旁路 正向有功电能', 'L302F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (70, 'L302F3-a', 'L302F3-a UPS 204-13# 正向有功电能', 'L302F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (71, 'L302F3-b', 'L302F3-b UPS 205旁路 正向有功电能', 'L302F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (72, 'L302F4-a', 'L302F4-a UPS 201旁路 正向有功电能', 'L302F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (73, 'L302F4-b', 'L302F4-b 备用 正向有功电能', 'L302F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (74, 'R401F1-a', 'R401F1-a 空调设备 正向有功电能', 'R401F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (75, 'R401F1-b', 'R401F1-b 有源滤波 正向有功电能', 'R401F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (76, 'R401F2-a', 'R401F2-a 大宗气体 正向有功电能', 'R401F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (77, 'R401F2-b', 'R401F2-b UPS403主进 正向有功电能', 'R401F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (78, 'R401F3-a', 'R401F3-a 8#硅烷站 正向有功电能', 'R401F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (79, 'R401F3-b', 'R401F3-b 变电站站用箱 正向有功电能', 'R401F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (80, 'R401F3-c', 'R401F3-c 有机水设备 正向有功电能', 'R401F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (81, 'R401F3-d', 'R401F3-d 废水设备 正向有功电能', 'R401F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (82, 'R401F3-e', 'R401F3-e 空调设备 正向有功电能', 'R401F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (83, 'R401F3-f', 'R401F3-f VOC设备 正向有功电能', 'R401F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (84, 'R401F4-a', 'R401F4-a 空调设备 正向有功电能', 'R401F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (85, 'R401F4-b', 'R401F4-b 6#化学品库 正向有功电能', 'R401F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (86, 'R401F4-c', 'R401F4-c 空调设备 正向有功电能', 'R401F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (87, 'R401F4-d', 'R401F4-d 备用 正向有功电能', 'R401F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (88, 'R401F4-e', 'R401F4-e 空调设备 正向有功电能', 'R401F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (89, 'R401F4-f', 'R401F4-f 空调设备 正向有功电能', 'R401F4-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (90, 'R401F4-g', 'R401F4-g UPS402主进 正向有功电能', 'R401F4-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (91, 'R401F5-a', 'R401F5-a 8#硅烷站 正向有功电能', 'R401F5-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (92, 'R401F5-b', 'R401F5-b 直流屏电源 正向有功电能', 'R401F5-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (93, 'R401F5-c', 'R401F5-c 备用 正向有功电能', 'R401F5-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (94, 'R401F5-d', 'R401F5-d 空调设备 正向有功电能', 'R401F5-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (95, 'R401F5-e', 'R401F5-e 空调设备 正向有功电能', 'R401F5-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (96, 'R401F5-f', 'R401F5-f 真空间增加柜 正向有功电能', 'R401F5-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (97, 'R401F5-g', 'R401F5-g 空调设备 正向有功电能', 'R401F5-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (98, 'R401F5-h', 'R401F5-h 8#硅烷站 正向有功电能', 'R401F5-h', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (99, 'R401F6-a', 'R401F6-a 备用 正向有功电能', 'R401F6-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (100, 'R401F6-b', 'R401F6-b 7#危险品库 正向有功电能', 'R401F6-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (101, 'R401F6-c', 'R401F6-c 备用 正向有功电能', 'R401F6-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (102, 'R401F6-d', 'R401F6-d 空调设备 正向有功电能', 'R401F6-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (103, 'R401F6-e', 'R401F6-e 备用 正向有功电能', 'R401F6-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (104, 'R401F6-f', 'R401F6-f 空调设备 正向有功电能', 'R401F6-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (105, 'R401F6-g', 'R401F6-g 室外充电桩 正向有功电能', 'R401F6-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (106, 'R402F1-a', 'R402F1-a 空调设备 正向有功电能', 'R402F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (107, 'R402F1-b', 'R402F1-b 空调设备 正向有功电能', 'R402F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (108, 'R402F2-a', 'R402F2-a 空调设备 正向有功电能', 'R402F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (109, 'R402F2-b', 'R402F2-b UPS403旁路进 正向有功电能', 'R402F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (110, 'R402F3-a', 'R402F3-a 空调设备 正向有功电能', 'R402F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (111, 'R402F3-b', 'R402F3-b 7#危险品库 正向有功电能', 'R402F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (112, 'R402F3-c', 'R402F3-c 插座总箱 正向有功电能', 'R402F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (113, 'R402F3-d', 'R402F3-d UPS402旁路 正向有功电能', 'R402F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (114, 'R402F3-e', 'R402F3-e 备用 正向有功电能', 'R402F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (115, 'R402F3-f', 'R402F3-f 有源滤波 正向有功电能', 'R402F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (116, 'R402F4-a', 'R402F4-a 8#硅烷站 正向有功电能', 'R402F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (117, 'R402F4-b', 'R402F4-b 空调设备 正向有功电能', 'R402F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (118, 'R402F4-c', 'R402F4-c 空调设备 正向有功电能', 'R402F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (119, 'R402F4-d', 'R402F4-d 空调设备 正向有功电能', 'R402F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (120, 'R402F4-e', 'R402F4-e 气动设备 正向有功电能', 'R402F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (121, 'R402F4-f', 'R402F4-f 空调设备 正向有功电能', 'R402F4-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (122, 'R402F4-g', 'R402F4-g 备用 正向有功电能', 'R402F4-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (123, 'R402F5-a', 'R402F5-a 空调设备 正向有功电能', 'R402F5-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (124, 'R402F5-b', 'R402F5-b 6#化学品库 正向有功电能', 'R402F5-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (125, 'R402F5-c', 'R402F5-c 空调设备 正向有功电能', 'R402F5-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (126, 'R402F5-d', 'R402F5-d 直流屏电源 正向有功电能', 'R402F5-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (127, 'R402F5-e', 'R402F5-e 空调设备 正向有功电能', 'R402F5-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (128, 'R402F5-f', 'R402F5-f 空调设备 正向有功电能', 'R402F5-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (129, 'R402F5-g', 'R402F5-g 空调设备 正向有功电能', 'R402F5-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (130, 'R402F6-a', 'R402F6-a 变电站站用箱 正向有功电能', 'R402F6-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (131, 'R402F6-b', 'R402F6-b 空调设备 正向有功电能', 'R402F6-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (132, 'R402F6-c', 'R402F6-c 传送电梯 正向有功电能', 'R402F6-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (133, 'R402F6-d', 'R402F6-d 空调设备 正向有功电能', 'R402F6-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (134, 'R402F6-e', 'R402F6-e 空调设备 正向有功电能', 'R402F6-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (135, 'R402F6-f', 'R402F6-f 空调设备 正向有功电能', 'R402F6-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (136, 'R402F6-g', 'R402F6-g 纯水系统 正向有功电能', 'R402F6-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (137, '5102F1-a', '5102F1-a UPS501主进 正向有功电能', '5102F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (138, '5102F1-b', '5102F1-b 有源滤波 正向有功电能', '5102F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (139, '5102F2-a', '5102F2-a 消防水泵 正向有功电能', '5102F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (140, '5102F2-b', '5102F2-b 水泵 正向有功电能', '5102F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (141, '5102F2-c', '5102F2-c 水泵 正向有功电能', '5102F2-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (142, '5102F2-d', '5102F2-d 消防配电箱 正向有功电能', '5102F2-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (143, '5102F2-e', '5102F2-e 分界小室 正向有功电能', '5102F2-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (144, '5102F3-a', '5102F3-a 水泵 正向有功电能', '5102F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (145, '5102F3-b', '5102F3-b 水泵 正向有功电能', '5102F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (146, '5102F3-c', '5102F3-c 备用 正向有功电能', '5102F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (147, '5102F3-d', '5102F3-d 3#生产测试楼 正向有功电能', '5102F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (148, '5102F3-e', '5102F3-e 备用 正向有功电能', '5102F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (149, '5102F4-a', '5102F4-a 应急照明 正向有功电能', '5102F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (150, '5102F4-b', '5102F4-b 直流屏电源 正向有功电能', '5102F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (151, '5102F4-c', '5102F4-c 电梯配电箱 正向有功电能', '5102F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (152, '5102F4-d', '5102F4-d 10#门卫2 正向有功电能', '5102F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (153, '5102F4-e', '5102F4-e 备用 正向有功电能', '5102F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (154, '5102F4-f', '5102F4-f 消防水泵 正向有功电能', '5102F4-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (155, '5102F4-g', '5102F4-g 4#宿舍楼 正向有功电能', '5102F4-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (156, '5102F5-a', '5102F5-a 变电站站用箱 正向有功电能', '5102F5-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (157, '5102F5-b', '5102F5-b 10#门卫2 正向有功电能', '5102F5-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (158, '5102F5-c', '5102F5-c 2#厂房 正向有功电能', '5102F5-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (159, '5102F5-d', '5102F5-d 4#宿舍楼 正向有功电能', '5102F5-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (160, '5102F5-e', '5102F5-e 备用 正向有功电能', '5102F5-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (161, '5102F5-f', '5102F5-f 4#宿舍楼 正向有功电能', '5102F5-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (162, '5102F5-g', '5102F5-g 2#厂房 正向有功电能', '5102F5-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (163, '5103F1-a', '5103F1-a 水泵 正向有功电能', '5103F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (164, '5103F1-b', '5103F1-b UPS501旁路进 正向有功电能', '5103F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (165, '5103F2-a', '5103F2-a 水泵 正向有功电能', '5103F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (166, '5103F2-b', '5103F2-b 水泵 正向有功电能', '5103F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (167, '5103F3-a', '5103F3-a 备用 正向有功电能', '5103F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (168, '5103F3-b', '5103F3-b 排风风机 正向有功电能', '5103F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (169, '5103F3-c', '5103F3-c 水泵 正向有功电能', '5103F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (170, '5103F3-d', '5103F3-d 水泵 正向有功电能', '5103F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (171, '5103F3-e', '5103F3-e 5-3APF1 正向有功电能', '5103F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (172, '5103F4-a', '5103F4-a 热水泵 正向有功电能', '5103F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (173, '5103F4-b', '5103F4-b 备用 正向有功电能', '5103F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (174, '5103F4-c', '5103F4-c 消防水泵 正向有功电能', '5103F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (175, '5103F4-d', '5103F4-d 水泵 正向有功电能', '5103F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (176, '5103F4-e', '5103F4-e 备用 正向有功电能', '5103F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (177, '5103F5-a', '5103F5-a 应急照明 正向有功电能', '5103F5-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (178, '5103F5-b', '5103F5-b 变电站站用箱 正向有功电能', '5103F5-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (179, '5103F5-c', '5103F5-c 水泵 正向有功电能', '5103F5-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (180, '5103F5-d', '5103F5-d 备用 正向有功电能', '5103F5-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (181, '5103F5-e', '5103F5-e 有源滤波 正向有功电能', '5103F5-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (182, '5103F5-f', '5103F5-f 消防水泵 正向有功电能', '5103F5-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (183, '5103F5-g', '5103F5-g 备用 正向有功电能', '5103F5-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (184, '5104F1-a', '5104F1-a 水泵 正向有功电能', '5104F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (185, '5104F1-b', '5104F1-b 水泵 正向有功电能', '5104F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (186, '5104F2-a', '5104F2-a UPS502旁路进 正向有功电能', '5104F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (187, '5104F2-b', '5104F2-b 废水站 正向有功电能', '5104F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (188, '5104F3-a', '5104F3-a 冷却塔 正向有功电能', '5104F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (189, '5104F3-b', '5104F3-b 室外充电桩 正向有功电能', '5104F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (190, '5104F3-c', '5104F3-c 水泵 正向有功电能', '5104F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (191, '5104F3-d', '5104F3-d 水泵 正向有功电能', '5104F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (192, '5104F3-e', '5104F3-e 备用 正向有功电能', '5104F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (193, '5104F3-f', '5104F3-f 备用 正向有功电能', '5104F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (194, '5104F3-g', '5104F3-g 备用 正向有功电能', '5104F3-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (195, '5104F4-a', '5104F4-a 水泵 正向有功电能', '5104F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (196, '5104F4-b', '5104F4-b 水泵 正向有功电能', '5104F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (197, '5104F4-c', '5104F4-c 备用 正向有功电能', '5104F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (198, '5104F4-d', '5104F4-d 有源滤波 正向有功电能', '5104F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (199, '5104F4-e', '5104F4-e 照明插座 正向有功电能', '5104F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (200, '5105F1-a', '5105F1-a 水泵 正向有功电能', '5105F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (201, '5105F1-b', '5105F1-b UPS502主进 正向有功电能', '5105F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (202, '5105F2-a', '5105F2-a 水泵 正向有功电能', '5105F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (203, '5105F2-b', '5105F2-b 有源滤波 正向有功电能', '5105F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (204, '5105F3-a', '5105F3-a 事故风机 正向有功电能', '5105F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (205, '5105F3-b', '5105F3-b 纯废水E电柜 正向有功电能', '5105F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (206, '5105F3-c', '5105F3-c 事故风机 正向有功电能', '5105F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (207, '5105F3-d', '5105F3-d 备用 正向有功电能', '5105F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (208, '5105F3-e', '5105F3-e 备用 正向有功电能', '5105F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (209, '5105F3-f', '5105F3-f 备用 正向有功电能', '5105F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (210, '5105F4-a', '5105F4-a 事故风机 正向有功电能', '5105F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (211, '5105F4-b', '5105F4-b 备用 正向有功电能', '5105F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (212, '5105F4-c', '5105F4-c 事故风机 正向有功电能', '5105F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (213, '5105F4-d', '5105F4-d 冷却塔 正向有功电能', '5105F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (214, '5105F4-e', '5105F4-e 冰机 正向有功电能', '5105F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (215, '5106F1-a', '5106F1-a 水泵 正向有功电能', '5106F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (216, '5106F1-b', '5106F1-b 水泵 正向有功电能', '5106F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (217, '5106F2-a', '5106F2-a 冷却塔 正向有功电能', '5106F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (218, '5106F2-b', '5106F2-b 水泵 正向有功电能', '5106F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (219, '5106F3-a', '5106F3-a 备用 正向有功电能', '5106F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (220, '5106F3-b', '5106F3-b 纯水站 正向有功电能', '5106F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (221, '5106F4-a', '5106F4-a 有源滤波 正向有功电能', '5106F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (222, '5106F4-b', '5106F4-b 水泵 正向有功电能', '5106F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (223, '5107F1-a', '5107F1-a 冷却塔 正向有功电能', '5107F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (224, '5107F1-b', '5107F1-b 水泵 正向有功电能', '5107F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (225, '5107F2-a', '5107F2-a 水泵 正向有功电能', '5107F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (226, '5107F2-b', '5107F2-b 备用 正向有功电能', '5107F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (227, '5107F3-a', '5107F3-a 有源滤波 正向有功电能', '5107F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (228, '5107F3-b', '5107F3-b 纯水站 正向有功电能', '5107F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (229, '5107F4-a', '5107F4-a 备用 正向有功电能', '5107F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (230, '5107F4-b', '5107F4-b 水泵 正向有功电能', '5107F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (231, '5202F3-a', '5202F3-a 9#门卫1 正向有功电能', '5202F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (232, '5202F3-b', '5202F3-b 备用 正向有功电能', '5202F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (233, '5202F3-c', '5202F3-c 备用 正向有功电能', '5202F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (234, '5202F3-d', '5202F3-d 室外充电桩 正向有功电能', '5202F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (235, '5202F3-e', '5202F3-e 备用 正向有功电能', '5202F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (236, '5202F3-f', '5202F3-f 生产测试楼 正向有功电能', '5202F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (237, '5202F4-a', '5202F4-a 备用 正向有功电能', '5202F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (238, '5202F4-b', '5202F4-b FAB2人防 正向有功电能', '5202F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (239, '5202F4-c', '5202F4-c 生产测试楼 正向有功电能', '5202F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (240, '5202F4-d', '5202F4-d 有源滤波 正向有功电能', '5202F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (241, '5202F4-e', '5202F4-e 生产测试楼 正向有功电能', '5202F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (242, '5203F3-a', '5203F3-a 11#门卫3 正向有功电能', '5203F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (243, '5203F3-b', '5203F3-b 备用 正向有功电能', '5203F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (244, '5203F3-c', '5203F3-c FAB2人防 正向有功电能', '5203F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (245, '5203F3-d', '5203F3-d 生产测试楼 正向有功电能', '5203F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (246, '5203F3-e', '5203F3-e 室外充电桩 正向有功电能', '5203F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (247, '5203F3-f', '5203F3-f 生产测试楼 正向有功电能', '5203F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (248, '5203F4-a', '5203F4-a FAB2 正向有功电能', '5203F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (249, '5203F4-b', '5203F4-b 备用 正向有功电能', '5203F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (250, '5203F4-c', '5203F4-c 生产测试楼 正向有功电能', '5203F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (251, '5203F4-d', '5203F4-d 有源滤波 正向有功电能', '5203F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (252, '5203F4-e', '5203F4-e FAB2 正向有功电能', '5203F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (253, '5204F4-b', '5204F4-b 雨水调节池 正向有功电能', '5204F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (254, '5204F4-c', '5204F4-c 生产测试楼 正向有功电能', '5204F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (255, '5204F4-d', '5204F4-d 备用 正向有功电能', '5204F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (256, '5204F4-e', '5204F4-e 有源滤波 正向有功电能', '5204F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (257, '5204F3-a', '5204F3-a 10#门卫2 正向有功电能', '5204F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (258, '5204F3-b', '5204F3-b 备用 正向有功电能', '5204F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (259, '5204F3-c', '5204F3-c 备用 正向有功电能', '5204F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (260, '5204F3-d', '5204F3-d 生产测试楼 正向有功电能', '5204F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (261, '5204F3-e', '5204F3-e 备用 正向有功电能', '5204F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (262, '5204F3-f', '5204F3-f 生产测试楼 正向有功电能', '5204F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (263, '5204F4-a', '5204F4-a 备用 正向有功电能', '5204F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (264, '5205F3-b', '5205F3-b 10#门卫2 正向有功电能', '5205F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (265, '5205F3-c', '5205F3-c 分界小室 正向有功电能', '5205F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (266, '5205F3-d', '5205F3-d 10#门卫2 正向有功电能', '5205F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (267, '5205F3-e', '5205F3-e 备用 正向有功电能', '5205F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (268, '5205F3-f', '5205F3-f 生产测试楼 正向有功电能', '5205F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (269, '5205F4-a', '5205F4-a 宿舍楼 正向有功电能', '5205F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (270, '5205F4-b', '5205F4-b 备用 正向有功电能', '5205F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (271, '5205F3-a', '5205F3-a 备用 正向有功电能', '5205F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (272, '5205F4-c', '5205F4-c 生产测试楼 正向有功电能', '5205F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (273, '5205F4-d', '5205F4-d 备用 正向有功电能', '5205F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (274, '5205F4-e', '5205F4-e 有源滤波 正向有功电能', '5205F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (275, 'R1AH01A', 'R1AH01A 馈线 仪表 正向Ep', 'R1AH01A', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (276, 'R1AH01B', 'R1AH01B 馈线 仪表 正向Ep', 'R1AH01B', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (277, 'R1AH01C', 'R1AH01C 馈线 仪表 正向Ep', 'R1AH01C', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (278, 'R1AH01D', 'R1AH01D 馈线 仪表 正向Ep', 'R1AH01D', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (279, 'R1AH01E', 'R1AH01E 馈线 仪表 正向Ep', 'R1AH01E', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (280, 'R1AH01F', 'R1AH01F 馈线 仪表 正向Ep', 'R1AH01F', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (281, 'R1AH01G', 'R1AH01G 馈线 仪表 正向Ep', 'R1AH01G', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (282, 'R1AH02F', 'R1AH02F 馈线 仪表 正向Ep', 'R1AH02F', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (283, 'R1AH02E', 'R1AH02E 馈线 仪表 正向Ep', 'R1AH02E', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (284, 'R1AH02D', 'R1AH02D 馈线 仪表 正向Ep', 'R1AH02D', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (285, 'R1AH02C', 'R1AH02C 馈线 仪表 正向Ep', 'R1AH02C', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (286, 'R1AH02B', 'R1AH02B 馈线 仪表 正向Ep', 'R1AH02B', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (287, 'R1AH02A', 'R1AH02A 馈线 仪表 正向Ep', 'R1AH02A', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (288, 'R1AH01M', 'R1AH01M 进线 电能质量仪表 Ep-TOTAL', 'R1AH01M', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (289, 'R1AH03M', 'R1AH03M 进线 电能质量仪表 Ep-TOTAL', 'R1AH03M', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (290, 'R1AH02M', 'R1AH02M 进线 电能质量仪表 Ep-TOTAL', 'R1AH02M', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (291, 'L401F4-a', 'L401F4-a 空调设备 正向有功电能', 'L401F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (292, 'L401F4-b', 'L401F4-b 空调设备 正向有功电能', 'L401F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (293, 'L401F4-c', 'L401F4-c 空调设备 正向有功电能', 'L401F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (294, 'L401F4-d', 'L401F4-d 16#大宗气站 正向有功电能', 'L401F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (295, 'L401F4-e', 'L401F4-e 变电站站用箱 正向有功电能', 'L401F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (296, 'L401F4-f', 'L401F4-f 照明总箱 正向有功电能', 'L401F4-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (297, 'L401F4-g', 'L401F4-g 空调设备 正向有功电能', 'L401F4-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (298, 'L401F5-a', 'L401F5-a 客梯 正向有功电能', 'L401F5-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (299, 'L401F5-b', 'L401F5-b 客梯 正向有功电能', 'L401F5-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (300, 'L401F5-c', 'L401F5-c 空调设备 正向有功电能', 'L401F5-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (301, 'L401F5-d', 'L401F5-d 空调设备 正向有功电能', 'L401F5-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (302, 'L401F5-e', 'L401F5-e 备用 正向有功电能', 'L401F5-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (303, 'L401F5-f', 'L401F5-f 空调设备 正向有功电能', 'L401F5-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (304, 'L401F5-g', 'L401F5-g 消防设备 正向有功电能', 'L401F5-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (305, 'L401F6-a', 'L401F6-a 客梯 正向有功电能', 'L401F6-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (306, 'L401F6-b', 'L401F6-b 备用 正向有功电能', 'L401F6-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (307, 'L401F6-c', 'L401F6-c 空调设备 正向有功电能', 'L401F6-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (308, 'L401F6-d', 'L401F6-d 空调设备 正向有功电能', 'L401F6-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (309, 'L401F6-e', 'L401F6-e 货梯 正向有功电能', 'L401F6-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (310, 'L401F6-f', 'L401F6-f 应急照明总箱A 正向有功电能', 'L401F6-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (311, 'L401F6-g', 'L401F6-g 消防设备 正向有功电能', 'L401F6-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (312, 'L401F1-a', 'L401F1-a UPS401旁路进 正向有功电能', 'L401F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (313, 'L401F1-b', 'L401F1-b 空调设备 正向有功电能', 'L401F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (314, 'L401F2-a', 'L401F2-a 空调设备 正向有功电能', 'L401F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (315, 'L401F2-b', 'L401F2-b 备用 正向有功电能', 'L401F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (316, 'L401F3-a', 'L401F3-a 空调设备 正向有功电能', 'L401F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (317, 'L401F3-b', 'L401F3-b 纯水设备 正向有功电能', 'L401F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (318, 'L402F1-a', 'L402F1-a 空调设备 正向有功电能', 'L402F1-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (319, 'L402F1-b', 'L402F1-b UPS401主进 正向有功电能', 'L402F1-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (320, 'L402F2-a', 'L402F2-a 特气 正向有功电能', 'L402F2-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (321, 'L402F2-b', 'L402F2-b 纯水E电柜 正向有功电能', 'L402F2-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (322, 'L402F3-a', 'L402F3-a 备用 正向有功电能', 'L402F3-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (323, 'L402F3-b', 'L402F3-b 16#大宗气站 正向有功电能', 'L402F3-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (324, 'L402F3-c', 'L402F3-c 空调设备 正向有功电能', 'L402F3-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (325, 'L402F3-d', 'L402F3-d 备用 正向有功电能', 'L402F3-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (326, 'L402F3-e', 'L402F3-e 空调设备 正向有功电能', 'L402F3-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (327, 'L402F3-f', 'L402F3-f 空调设备 正向有功电能', 'L402F3-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (328, 'L402F4-a', 'L402F4-a 变电站站用箱 正向有功电能', 'L402F4-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (329, 'L402F4-b', 'L402F4-b 备用 正向有功电能', 'L402F4-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (330, 'L402F4-c', 'L402F4-c 空调设备 正向有功电能', 'L402F4-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (331, 'L402F4-d', 'L402F4-d 备用 正向有功电能', 'L402F4-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (332, 'L402F4-e', 'L402F4-e 空调设备 正向有功电能', 'L402F4-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (333, 'L402F4-f', 'L402F4-f 空调设备 正向有功电能', 'L402F4-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (334, 'L402F4-g', 'L402F4-g 消防设备 正向有功电能', 'L402F4-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (335, 'L402F5-a', 'L402F5-a 备用 正向有功电能', 'L402F5-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (336, 'L402F5-b', 'L402F5-b 空调设备 正向有功电能', 'L402F5-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (337, 'L402F5-c', 'L402F5-c 空调设备 正向有功电能', 'L402F5-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (338, 'L402F5-d', 'L402F5-d 空调设备 正向有功电能', 'L402F5-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (339, 'L402F5-e', 'L402F5-e 备用 正向有功电能', 'L402F5-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (340, 'L402F5-f', 'L402F5-f 空调设备 正向有功电能', 'L402F5-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (341, 'L402F5-g', 'L402F5-g 消防设备 正向有功电能', 'L402F5-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (342, 'L402F6-a', 'L402F6-a 空调设备 正向有功电能', 'L402F6-a', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (343, 'L402F6-b', 'L402F6-b 空调设备 正向有功电能', 'L402F6-b', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (344, 'L402F6-c', 'L402F6-c 纯水设备 正向有功电能', 'L402F6-c', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (345, 'L402F6-d', 'L402F6-d 空调设备 正向有功电能', 'L402F6-d', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (346, 'L402F6-e', 'L402F6-e 备用 正向有功电能', 'L402F6-e', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (347, 'L402F6-f', 'L402F6-f 空调设备 正向有功电能', 'L402F6-f', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (348, 'L402F6-g', 'L402F6-g 应急照明总箱B 正向有功电能', 'L402F6-g', 0, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (349, '1AH05', '1AH05 TR-5103变压器柜 正向有功电能', 'CUB-1AH05', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (350, '2AH05', '2AH05 TR-5102变压器柜 正向有功电能', 'CUB-2AH05', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (351, '3AH05', '3AH05 中温冷水机组CH-1 正向有功电能', 'CUB-3AH05', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (352, '3AH06', '3AH06 中温冷水机组CH-2 正向有功电能', 'CUB-3AH06', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (353, '3AH07', '3AH07 中温冷水机组CH-3 正向有功电能', 'CUB-3AH07', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (354, '3AH12', '3AH12 3#计量柜 正向有功电能', 'CUB-3AH12', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (355, '1AH16', '1AH16 TR-L401变压器进线柜 6AH01 正向有功电能', 'FAB-8-1AH16', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (356, '1AH12', '1AH12 TR-L101变压器进线柜 3AH01 正向有功电能', 'FAB-8-1AH12', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (357, '1AH11', '1AH11 TR-R401变压器柜 正向有功电能', 'FAB-8-1AH11', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (358, '6AH02', '6AH02 TR-L401变压器柜 正向有功电能', 'FAB-8-6AH02', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (359, '6AH03', '6AH03 TR-L402变压器柜 正向有功电能', 'FAB-8-6AH03', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (360, '2AH23', '2AH23 3#计量柜 正向有功电能', 'FAB-8-2AH23', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (361, '2AH14', '2AH14 TR-L302变压器进线柜5AH04 正向有功电能', 'FAB-8-2AH14', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (362, '2AH24', '2AH24 3#进线柜 Ep_imp', 'FAB-8-2AH24', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (363, '4AH03', '4AH03 TR-L202变压器柜 正向有功电能', 'FAB-8-4AH03', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (364, '4AH02', '4AH02 TR-L201变压器柜 正向有功电能', 'FAB-8-4AH02', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (365, '3AH02', '3AH02 TR-L101变压器柜 正向有功电能', 'FAB-8-3AH02', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (366, '5AH02', '5AH02 TR-L301变压器柜 正向有功电能', 'FAB-8-5AH02', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (367, '5AH03', '5AH03 TR-L302变压器柜 正向有功电能', 'FAB-8-5AH03', 2, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (368, 'L101F1', 'L101F1 5000A密集母线-19# 正向有功电能', 'L101F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (369, 'L101F2', 'L101F2 5000A密集母线-4# 正向有功电能', 'L101F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (370, 'L102F1', 'L102F1 5000A密集母线-8# 正向有功电能', 'L102F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (371, 'L201F1', 'L201F1 5000A密集母线-9# 正向有功电能', 'L201F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (372, 'L201F2', 'L201F2 5000A密集母线-3# 正向有功电能', 'L201F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (373, 'L202F1', 'L202F1 5000A密集母线-7# 正向有功电能', 'L202F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (374, 'R101F1', 'R101F1 5000A密集母线 正向有功电能', 'R101F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (375, 'R101F2', 'R101F2 5000A密集母线 正向有功电能', 'R101F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (376, 'R102F1', 'R102F1 5000A密集母线 正向有功电能', 'R102F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (377, 'R201F1', 'R201F1 5000A密集母线 正向有功电能', 'R201F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (378, 'R201F2', 'R201F2 5000A密集母线 正向有功电能', 'R201F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (379, 'R202F1', 'R202F1 5000A密集母线 正向有功电能', 'R202F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (380, 'R301F1', 'R301F1 5000A密集母线 正向有功电能', 'R301F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (381, 'R301F2', 'R301F2 有源滤波 正向有功电能', 'R301F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (382, 'R302F1', 'R302F1 5000A密集母线 正向有功电能', 'R302F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (383, 'R302F2', 'R302F2 有源滤波 正向有功电能', 'R302F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (384, 'L301F1', 'L301F1 1250A密集母线-20# 正向有功电能', 'L301F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (385, 'L302F1', 'L302F1 1250A密集母线-6# 正向有功电能', 'L302F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (386, '5202F1', '5202F1 2#密集型母线槽6(FAB2) 正向有功电能', '5202F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (387, '5202F2', '5202F2 2#密集型母线槽1(FAB2) 正向有功电能', '5202F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (388, '5203F1', '5203F1 2#密集型母线槽7(FAB2) 正向有功电能', '5203F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (389, '5203F2', '5203F2 2#密集型母线槽2(FAB2) 正向有功电能', '5203F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (390, '5204F1', '5204F1 2#密集型母线槽4(FAB2) 正向有功电能', '5204F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (391, '5204F2', '5204F2 2#密集型母线槽5(FAB2) 正向有功电能', '5204F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (392, '5205F1', '5205F1 2#密集型母线槽8(FAB2) 正向有功电能', '5205F1-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (393, '5205F2', '5205F2 2#密集型母线槽3(FAB2) 正向有功电能', '5205F2-a', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (394, 'UPS201-D', 'UPS201-D 待定 绝对值和有功电度', 'UPS201-Da', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (395, 'UPS202-D', 'UPS202-D 待定 绝对值和有功电度', 'UPS202-Da', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (396, 'UPS203-D', 'UPS203-D 待定 绝对值和有功电度', 'UPS203-Da', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (397, 'UPS205-D', 'UPS205-D 待定 绝对值和有功电度', 'UPS205-Da', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (398, 'UPS204-D', 'UPS204-D 待定 绝对值和有功电度', 'UPS204-Da', 1, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (399, 'UPS501-D-a', 'UPS501-D-a 备用 绝对值和有功电度', 'UPS501-Da', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (400, 'UPS501-D-b', 'UPS501-D-b 备用 绝对值和有功电度', 'UPS501-Db', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (401, 'UPS501-D-c', 'UPS501-D-c 备用 绝对值和有功电度', 'UPS501-Dc', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (402, 'UPS501-D-d', 'UPS501-D-d 备用 绝对值和有功电度', 'UPS501-Dd', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (403, 'UPS502-D-a', 'UPS502-D-a 备用 绝对值和有功电度', 'UPS502-Da', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (404, 'UPS502-D-b', 'UPS502-D-b 备用 绝对值和有功电度', 'UPS502-Db', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (405, 'UPS502-D-c', 'UPS502-D-c 备用 绝对值和有功电度', 'UPS502-Dc', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (406, 'UPS502-D-d', 'UPS502-D-d 备用 绝对值和有功电度', 'UPS502-Dd', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (407, 'UPS401-D-a', 'UPS401-D-a 备用 绝对值和有功电度', 'UPS401-Da', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (408, 'UPS401-D-b', 'UPS401-D-b 备用 绝对值和有功电度', 'UPS401-Db', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (409, 'UPS401-D-c', 'UPS401-D-c 备用 绝对值和有功电度', 'UPS401-Dc', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (410, 'UPS401-D-d', 'UPS401-D-d 备用 绝对值和有功电度', 'UPS401-Dd', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (411, 'UPS401-D-e', 'UPS401-D-e 备用 绝对值和有功电度', 'UPS401-De', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (412, 'UPS401-D-f', 'UPS401-D-f 6#大宗气站设备 绝对值和有功电度', 'UPS401-Df', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (413, 'UPS402-D-a', 'UPS402-D-a 备用 绝对值和有功电度', 'UPS402-Da', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (414, 'UPS402-D-b', 'UPS402-D-b 备用 绝对值和有功电度', 'UPS402-Db', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (415, 'UPS402-D-c', 'UPS402-D-c 备用 绝对值和有功电度', 'UPS402-Dc', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (416, 'UPS402-D-d', 'UPS402-D-d 备用 绝对值和有功电度', 'UPS402-Dd', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (417, 'UPS402-D-e', 'UPS402-D-e 备用 绝对值和有功电度', 'UPS402-De', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (418, 'UPS402-D-f', 'UPS402-D-f 备用 绝对值和有功电度', 'UPS402-Df', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (419, 'UPS402-D-g', 'UPS402-D-g 备用 绝对值和有功电度', 'UPS402-Dg', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (420, 'UPS403-D-a', 'UPS403-D-a 备用 绝对值和有功电度', 'UPS403-Da', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (421, 'UPS403-D-b', 'UPS403-D-b 备用 绝对值和有功电度', 'UPS403-Db', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (422, 'UPS403-D-c', 'UPS403-D-c 备用 绝对值和有功电度', 'UPS403-Dc', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (423, 'UPS403-D-d', 'UPS403-D-d 备用 绝对值和有功电度', 'UPS403-Dd', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (424, 'UPS403-D-e', 'UPS403-D-e 备用 绝对值和有功电度', 'UPS403-De', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);
INSERT INTO `ems_header_code_mapping` VALUES (425, 'UPS403-D-f', 'UPS403-D-f 备用 绝对值和有功电度', 'UPS403-Df', 5, NULL, '2025-06-20 16:55:38', NULL, '2025-06-20 16:55:38', b'0', 1);

```
####