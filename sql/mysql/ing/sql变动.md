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
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't1', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't2', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't3', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 't4', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTC', 2, 't5', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTC', 1, 'W1', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTC', 1, 'W2', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTC', 1, 'W3', '正向有功电能',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm1', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm2', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm3', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 1, 'm4', '正累积',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't1', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't2', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't3', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't4', '瞬时温度',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 't5', '瞬时温度',  1);
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
####