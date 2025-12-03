ALTER TABLE power_warning_info
ADD COLUMN handle_opinion VARCHAR(500) NULL COMMENT '处理意见';

CREATE TABLE `power_double_carbon_settings` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统名称',
`url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口地址',
`update_frequency` int(11) NOT NULL COMMENT '更新频率',
`update_frequency_unit` tinyint(4) NOT NULL COMMENT '更新频率单位',
`last_sync_time` datetime DEFAULT NULL COMMENT '上次同步时间',
`creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1956238533871136770 DEFAULT CHARSET=utf8mb4 COMMENT='双碳对接设置';
CREATE TABLE `power_double_carbon_mapping` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`standingbook_id` bigint(20) NOT NULL COMMENT '台账id',
`standingbook_code` varchar(400) NOT NULL COMMENT '台账编码',
`double_carbon_code` varchar(400) DEFAULT NULL COMMENT '双碳编码',
`creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1968948134542438403 DEFAULT CHARSET=utf8mb4 COMMENT='双碳对接 映射';

## 注意插入字段配置
```sql
INSERT INTO `power_double_carbon_settings` (`id`, `name`, `url`, `update_frequency`, `update_frequency_unit`,  `tenant_id`) VALUES (1, '双碳系统', 'http://www.baidu.com', 10, 2, 1);
INSERT INTO `ydme_ems`.`infra_config` (`id`, `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (13, 'biz', 1, '设备详情跳转连接', 'power.device.monitor.url', '<a href=\"http://82.157.40.213:8170/monitor/detail?id=%s&topType=%s\">查看详情</a>', b'0', '设备详情跳转连接', 'admin', '2025-08-31 18:47:02', '1', '2025-09-22 14:42:00', b'0');
INSERT INTO `ydme_ems`.`infra_config` (`id`, `category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (14, 'biz', 1, '二维码', 'power.device.monitor.qrcode.url', 'http://82.157.40.213:8170/monitor/equipment?id=%s&type=%s', b'0', '二维码', 'admin', '2025-08-31 18:47:02', '1', '2025-09-10 15:22:05', b'0');

```
ALTER TABLE power_warning_info DROP COLUMN user_id;
CREATE TABLE `power_warning_info_user` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`user_id` bigint(20) NOT NULL COMMENT '用户id',
`info_id` bigint(20) NOT NULL COMMENT '站内信id',
`creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1962814526492635139 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='告警信息-用户关联表';
CREATE TABLE `power_device_monitor_qrcode` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`device_id` bigint(20) NOT NULL COMMENT '用户id',
`qrcode` varchar(300) NOT NULL COMMENT '二维码内容',
`creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1962814526492635139 DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='设备监控-设备二维码维护';
```sql
-- 双碳频率字典
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES ('双碳接口频率', 'double_carbon_internal', 0, '', '1', '2025-09-03 10:17:14', '1', '2025-09-03 10:17:14', b'0', '1970-01-01 00:00:00');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 1, '分钟', '1', 'double_carbon_internal', 0, '', '', '', '1', '2025-09-03 10:17:22', '1', '2025-09-03 10:17:22', b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (2, '小时', '2', 'double_carbon_internal', 0, '', '', '', '1', '2025-09-03 10:17:28', '1', '2025-09-03 10:17:28', b'0');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 3, '天', '3', 'double_carbon_internal', 0, '', '', '', '1', '2025-09-03 10:17:33', '1', '2025-09-03 10:17:33', b'0');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES ('寄存器类型', 'register_type', 0, '', '140', '2025-09-08 16:59:56', '140', '2025-09-08 16:59:56', b'0', '1970-01-01 00:00:00');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 1, '01（线圈）', 'coils', 'register_type', 0, '', '', '', '140', '2025-09-08 17:01:01', '140', '2025-09-08 17:03:50', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (2, '02（离散输入）', 'discrete_inputs', 'register_type', 0, '', '', '', '140', '2025-09-08 17:01:31', '140', '2025-09-08 17:04:00', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 3, '03（保持寄存器）', 'holding_registers', 'register_type', 0, '', '', '', '140', '2025-09-08 17:02:05', '140', '2025-09-08 17:04:11', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 4, '04（输入寄存器）', 'input_registers', 'register_type', 0, '', '', '', '140', '2025-09-08 17:02:35', '140', '2025-09-08 17:04:20', b'0');

ALTER TABLE power_standingbook_acquisition_detail
    ADD COLUMN `modbus_salve` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '从地址';
ALTER TABLE power_standingbook_acquisition_detail
    ADD COLUMN `modbus_register_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '寄存器地址';
```

```sql
-- 初始化 双碳编号映射，同步所有本系统计量器具编码
INSERT INTO power_double_carbon_mapping (standingbook_code,standingbook_id,tenant_id)
select value, standingbook_id,1 from power_standingbook_attribute where code='measuringInstrumentId' and deleted =0 and standingbook_id is not null;
```


```sql
-- 凭证表添加字段
ALTER TABLE ems_voucher
    ADD COLUMN `month` date DEFAULT NULL COMMENT '月份';
```

```sql
-- 化学品数据设置表
CREATE TABLE `power_chemicals_settings` (
                                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                            `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型',
                                            `time` datetime NOT NULL COMMENT '日期',
                                            `price` decimal(15,2) DEFAULT NULL COMMENT '金额',
                                            `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                            `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1969936006635446275 DEFAULT CHARSET=utf8mb4 COMMENT='化学品数据设置表';

-- 初始值
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969936006635446274, 'HCL', '2025-09-22 00:00:00', NULL, NULL, '2025-09-22 09:25:18', NULL, '2025-09-22 09:25:18', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969936006119546881, 'NAOH', '2025-09-22 00:00:00', NULL, NULL, '2025-09-22 09:25:18', NULL, '2025-09-22 09:25:18', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969808842981240834, 'HCL', '2025-09-22 00:00:00', NULL, NULL, '2025-09-22 01:00:00', NULL, '2025-09-22 01:00:00', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969808842628919297, 'NAOH', '2025-09-22 00:00:00', NULL, NULL, '2025-09-22 01:00:00', NULL, '2025-09-22 01:00:00', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969446499080335362, 'HCL', '2025-09-21 00:00:00', NULL, NULL, '2025-09-21 01:00:11', NULL, '2025-09-21 01:00:11', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969446498686070786, 'NAOH', '2025-09-21 00:00:00', NULL, NULL, '2025-09-21 01:00:10', NULL, '2025-09-21 01:00:10', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969084067241652226, 'HCL', '2025-09-20 00:00:00', NULL, NULL, '2025-09-20 01:00:00', NULL, '2025-09-20 01:00:00', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1969084066885136386, 'NAOH', '2025-09-20 00:00:00', NULL, NULL, '2025-09-20 01:00:00', NULL, '2025-09-20 01:00:00', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1968721680542068738, 'NAOH', '2025-09-19 00:00:00', NULL, NULL, '2025-09-19 01:00:00', NULL, '2025-09-19 01:00:00', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1968721680927944706, 'HCL', '2025-09-19 00:00:00', NULL, NULL, '2025-09-19 01:00:00', NULL, '2025-09-19 01:00:00', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1968359293213339649, 'HCL', '2025-09-18 00:00:00', NULL, NULL, '2025-09-18 01:00:01', '1', '2025-09-18 14:38:34', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1968359292781326337, 'NAOH', '2025-09-18 00:00:00', NULL, NULL, '2025-09-18 01:00:00', '1', '2025-09-18 14:38:34', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1967996905096081410, 'HCL', '2025-09-17 00:00:00', NULL, NULL, '2025-09-17 01:00:00', '1', '2025-09-18 14:38:34', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1967996904722788354, 'NAOH', '2025-09-17 00:00:00', NULL, NULL, '2025-09-17 01:00:00', '1', '2025-09-18 14:38:34', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1967634528953561089, 'HCL', '2025-09-16 00:00:00', NULL, NULL, '2025-09-16 01:00:03', '1', '2025-09-18 14:38:34', b'0', 1);
INSERT INTO `ydme_ems`.`power_chemicals_settings` (`id`, `code`, `time`, `price`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1967634528567685121, 'NAOH', '2025-09-16 00:00:00', NULL, NULL, '2025-09-16 01:00:03', '1', '2025-09-18 14:38:34', b'0', 1);

```

```sql
-- 外部接口表
CREATE TABLE `power_external_api` (
                                      `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                      `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口名称',
                                      `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口编码',
                                      `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口地址',
                                      `method` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '请求方式',
                                      `body` text COMMENT 'body',
                                      `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                      `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
                                      PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1965315048739041282 DEFAULT CHARSET=utf8mb4 COMMENT='外部接口表';

INSERT INTO `ydme_ems`.`power_external_api` (`id`, `name`, `code`, `url`, `method`, `body`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1, '产量接口', 'chanliang', 'http://82.157.40.213:8107/admin-api/power/externalApi/getAllOut', 'POST', '', '1', '2025-08-28 10:37:32', '1', '2025-08-28 15:21:47', b'0', 1);

```

```sql
-- 本月计划设置表
CREATE TABLE `power_month_plan_settings` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                             `energy_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '能源名称',
                                             `energy_code` varchar(255) NOT NULL COMMENT '能源编号',
                                             `energy_unit` varchar(40) DEFAULT NULL COMMENT '能源单位',
                                             `plan` decimal(22,2) DEFAULT NULL COMMENT '计划用量',
                                             `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                             `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
                                             PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COMMENT='本月计划设置表';

INSERT INTO `ydme_ems`.`power_month_plan_settings` (`id`, `energy_name`, `energy_code`, `energy_unit`, `plan`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1, '电力', 'W_Dl_10KV', 'kwh', 0.00, '1', '2025-09-10 17:48:14', '140', '2025-09-16 18:14:04', b'0', 1);
INSERT INTO `ydme_ems`.`power_month_plan_settings` (`id`, `energy_name`, `energy_code`, `energy_unit`, `plan`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (2, '天然气', 'W_gas', 'm³', 0.00, '1', '2025-09-10 17:48:14', '140', '2025-09-16 18:14:04', b'0', 1);
INSERT INTO `ydme_ems`.`power_month_plan_settings` (`id`, `energy_name`, `energy_code`, `energy_unit`, `plan`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (3, '自来水', 'W_Tap Water', 'm³', 0.00, '1', '2025-09-10 17:48:14', '140', '2025-09-16 18:14:04', b'0', 1);
INSERT INTO `ydme_ems`.`power_month_plan_settings` (`id`, `energy_name`, `energy_code`, `energy_unit`, `plan`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (4, '高品质再生水', 'W_Reclaimed Water', 'm³', 0.00, '1', '2025-09-10 17:48:14', '140', '2025-09-16 18:14:04', b'0', 1);
INSERT INTO `ydme_ems`.`power_month_plan_settings` (`id`, `energy_name`, `energy_code`, `energy_unit`, `plan`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (5, '热力', 'W_heat', 'GJ', 0.00, '1', '2025-09-10 17:48:14', '140', '2025-09-16 18:14:04', b'0', 1);

```

```sql
-- 纯废水压缩空气设置表
CREATE TABLE `power_pure_waste_water_gas_settings` (
                                                       `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                                       `system` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型',
                                                       `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '编码',
                                                       `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '名称',
                                                       `energy_codes` varchar(255) DEFAULT NULL COMMENT '能源codes',
                                                       `standingbook_ids` text COMMENT '台账ids',
                                                       `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                       `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                       `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                                       `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
                                                       PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COMMENT='纯废水压缩空气设置表';

INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1, 'PURE', 'NAOH', '30%NAOH', '', '', '1', '2025-09-05 18:33:23', '1', '2025-09-17 16:01:09', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (2, 'PURE', 'HCL', '30%HCL', '', '', '1', '2025-09-05 18:33:23', '1', '2025-09-17 16:01:09', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (3, 'PURE', 'TW', '自来水', 'W_Tap Water', null, '1', '2025-09-05 18:33:23', '1', '2025-09-17 16:01:09', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (4, 'PURE', 'RW', '高品质再生水', 'W_Reclaimed Water', null, '1', '2025-09-05 18:33:23', '1', '2025-09-17 16:01:09', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (5, 'PURE', 'DL', '电力', 'W_Dl_10KV,Dl_400V,Dl_215V,Dl_480V', null, '1', '2025-09-05 18:33:23', '1', '2025-09-17 16:01:09', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (6, 'PURE', 'PW', '纯水供水量', 'RO_water,DIW,UPW', null, '1', '2025-09-05 18:33:23', '1', '2025-09-17 16:01:09', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (7, 'WASTE', 'NAOH', '30%NAOH', '', '', '1', '2025-09-05 18:33:23', '1', '2025-09-17 15:58:32', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (8, 'WASTE', 'HCL', '30%HCL', '', '', '1', '2025-09-05 18:33:23', '1', '2025-09-17 15:58:32', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (9, 'WASTE', 'DL', '电力', 'W_Dl_10KV,Dl_400V,Dl_215V,Dl_480V', null, '1', '2025-09-05 18:33:23', '1', '2025-09-17 15:58:32', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (10, 'WASTE', 'FL', '废水量', 'ACW_FL505', null, '1', '2025-09-05 18:33:23', '1', '2025-09-17 15:58:32', b'0', 1);
INSERT INTO `ydme_ems`.`power_pure_waste_water_gas_settings` (`id`, `system`, `code`, `name`, `energy_codes`, `standingbook_ids`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (11, 'GAS', 'DL', '电力', 'W_Dl_10KV,Dl_400V,Dl_215V,Dl_480V', null, '1', '2025-09-05 18:33:23', '1', '2025-09-17 11:48:50', b'0', 1);

```

```sql
-- 产品产量同步统计表
CREATE TABLE `power_production` (
                                    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                    `time` datetime NOT NULL COMMENT '获取时间',
                                    `origin_time` varchar(40) DEFAULT NULL COMMENT '原始时间',
                                    `plan` decimal(15,2) DEFAULT NULL COMMENT '计划产量',
                                    `lot` decimal(15,2) DEFAULT NULL COMMENT '实际产量',
                                    `size` int(11) NOT NULL COMMENT '产量尺寸',
                                    `value` decimal(15,2) DEFAULT NULL COMMENT '间隔产量数',
                                    `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                    `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
                                    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1969944740413693954 DEFAULT CHARSET=utf8mb4;
```

```sql
-- 内网共享文件设置表
CREATE TABLE `power_share_file_settings` (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
                                             `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门名称',
                                             `type` int(11) NOT NULL COMMENT '目录拼接类型[1：年月日；2：年。]',
                                             `ip` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部门服务器ip地址',
                                             `dir` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '共享文件夹地址前缀',
                                             `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                             `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
                                             PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='内网共享文件设置表';

INSERT INTO `ydme_ems`.`power_share_file_settings` (`id`, `name`, `type`, `ip`, `dir`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1, '燃气', 2, '172.16.150.23', '\\\\172.16.150.23\\Users\\YDME-C03\\Desktop\\数据', '1', '2025-10-13 15:15:48', '1', '2025-10-13 15:16:01', b'0', 1);

```