CREATE TABLE `ems_invoice_power_record` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`record_month` date NOT NULL COMMENT '补录月份（建议存当月第一天，如 2025-09-01）',
`amount` decimal(16,2) DEFAULT NULL COMMENT '金额(含税，税率13%)，可为空',
`remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '备注',
`creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`) USING BTREE,
KEY `idx_record_month` (`record_month`),
KEY `idx_tenant_month` (`tenant_id`,`record_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='发票电量记录（按月汇总）';


CREATE TABLE `ems_invoice_power_record_item` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`record_id` bigint(20) NOT NULL COMMENT '发票电量记录ID（关联 ems_invoice_power_record.id）',
`meter_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '表计编号（CUB-1AH03 等）',
`total_kwh` decimal(16,4) DEFAULT NULL COMMENT '总电度(kWh)，可为空',
`demand_kwh` decimal(16,4) DEFAULT NULL COMMENT '需量电度(kWh)，可为空',
`creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`) USING BTREE,
KEY `idx_record_id` (`record_id`),
KEY `idx_meter_code` (`meter_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='发票电量记录明细（按表计）';

## 新增字典
INSERT INTO `ydme_ems`.`system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES (659, '发票电量编号', 'invoice_meter_code', 0, '', '1', '2025-11-30 10:53:07', '1', '2025-11-30 10:53:07', b'0', '1970-01-01 00:00:00');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1699, 8, 'R1AH03J', 'R1AH03J', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:32', '1', '2025-11-30 10:56:32', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1698, 9, 'R1AH02J', 'R1AH02J', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:21', '1', '2025-11-30 10:57:18', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1697, 7, 'R1AH01J', 'R1AH01J', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:12', '1', '2025-11-30 10:56:12', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1696, 6, 'FAB1-2AH23', 'FAB1-2AH23', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:03', '1', '2025-11-30 10:56:03', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1695, 5, 'FAB1-2AH03', 'FAB1-2AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:52', '1', '2025-11-30 10:55:52', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1694, 4, 'FAB1-1AH03', 'FAB1-1AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:40', '1', '2025-11-30 10:55:40', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1693, 3, 'CUB-3AH12', 'CUB-3AH12', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:30', '1', '2025-11-30 10:55:30', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1692, 2, 'CUB-2AH03', 'CUB-2AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:16', '1', '2025-11-30 10:55:16', b'0');
INSERT INTO `ydme_ems`.`system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1691, 1, 'CUB-1AH03', 'CUB-1AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:04', '1', '2025-11-30 10:55:04', b'0');
