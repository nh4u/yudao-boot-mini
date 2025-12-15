use ydme_ems;
CREATE TABLE `ems_invoice_power_record` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
    `record_month` date NOT NULL COMMENT '补录月份（建议存当月第一天，如 2025-09-01）',
    `amount` decimal(23,3) DEFAULT NULL COMMENT '金额(含税，税率13%)，可为空',
    `remark` varchar(255)  DEFAULT '' COMMENT '备注',
    `creator` varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64)  DEFAULT '' COMMENT '更新者',
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
     `meter_code` varchar(32)  NOT NULL COMMENT '表计编号（CUB-1AH03 等）',
     `total_kwh` decimal(23,3) DEFAULT NULL COMMENT '总电度(kWh)，可为空',
     `demand_kwh` decimal(23,3) DEFAULT NULL COMMENT '需量电度(kWh)，可为空',
     `creator` varchar(64)  DEFAULT '' COMMENT '创建者',
     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     `updater` varchar(64)  DEFAULT '' COMMENT '更新者',
     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
     `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
     PRIMARY KEY (`id`) USING BTREE,
     KEY `idx_record_id` (`record_id`),
     KEY `idx_meter_code` (`meter_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='发票电量记录明细（按表计）';

INSERT INTO `system_dict_type` ( `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES ('发票电量编号', 'invoice_meter_code', 0, '', '1', '2025-11-30 10:53:07', '1', '2025-11-30 10:53:07', b'0', '1970-01-01 00:00:00');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (9, 'R1AH03J', 'R1AH03J', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:32', '1', '2025-11-30 10:56:32', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 8, 'R1AH02J', 'R1AH02J', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:21', '1', '2025-11-30 10:57:18', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (7, 'R1AH01J', 'R1AH01J', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:12', '1', '2025-11-30 10:56:12', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (6, 'FAB1-2AH23', 'FAB1-2AH23', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:56:03', '1', '2025-11-30 10:56:03', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (5, 'FAB1-2AH03', 'FAB1-2AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:52', '1', '2025-11-30 10:55:52', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (4, 'FAB1-1AH03', 'FAB1-1AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:40', '1', '2025-11-30 10:55:40', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (3, 'CUB-3AH12', 'CUB-3AH12', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:30', '1', '2025-11-30 10:55:30', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (2, 'CUB-2AH03', 'CUB-2AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:16', '1', '2025-11-30 10:55:16', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (1, 'CUB-1AH03', 'CUB-1AH03', 'invoice_meter_code', 0, '', '', '', '1', '2025-11-30 10:55:04', '1', '2025-11-30 10:55:04', b'0');





update power_gas_measurement set sort_no = sort_no*10;
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (44, '普氮用量', 'GN2', 41, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (45, '高纯氮用量', 'PN2-CHQ', 161, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (46, '氢气用量', 'PH2-CHQ',  211, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (47, '氧气用量', 'PO2-CHQ', 291, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (48, '氩气用量', 'PAR-CHQ', 371, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (49, '氦气用量', 'PHE-CHQ', 431, b'0', 1, '');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`) VALUES ('气化部分计算公式', 'gas_report_formula', 0, '气化部分计算公式');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 1, 'GN2', '(N2_GD_01_LL)-(N2_CHQ_01_LL)-(N2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '普氮用量：氮气主管道流量-高纯氮用量');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 2, 'PN2-CHQ', '(N2_CHQ_01_LL)+(N2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '高纯氮用量：1#氮气纯化器流量+2#氮气纯化器流量');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 3, 'PH2-CHQ', '(H2_CHQ_01_LL)+(H2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氢气用量：1#氢气纯化器流量+2#氢气纯化器流量');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES (4, 'PO2-CHQ', '(O2_CHQ_01_LL)+(O2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氧气用量：1#氧气纯化器流量+2#氧气纯化器流量');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 5, 'PAR-CHQ', '(AR_CHQ_01_LL)+(AR_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氩气用量：1#氩气纯化器流量+2#氩气纯化器流量');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES (6, 'PHE-CHQ', '(HE_CHQ_01_LL)+(HE_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氦气用量：1#氦气纯化器流量+2#氦气纯化器流量');


INSERT INTO `infra_config` (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ('biz', 1, '用电量统计默认标签', 'power.electric.default.labels', '[[161,165],[161,166],[163,195],[163,193],[163,194,278],[163,194,281],[163,194,280],[163,194,279],[163,195,229],[163,195,227],[163,195,222],[163,195,223],[163,195,225],[163,195,226],[163,195,228],[163,195,224],[163,194,278,201],[163,194,278,202],[163,194,278,203],[163,194,278,205],[163,194,278,206],[163,194,278,207],[163,194,278,208],[163,194,278,209],[163,194,278,210],[163,194,278,200],[163,194,281,211],[163,194,281,289],[163,194,280,217],[163,194,280,218]]', b'1', '用电量统计默认展示标签', '', '2025-12-03 11:42:04', '', '2025-12-03 16:40:12', b'0');
INSERT INTO `infra_config` (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ('biz', 1, '全厂', 'power.label.all', '161,165#161,166', b'0', '全厂（用能单位->燕东科技、用能单位->高可靠）', 'admin', '2025-12-03 10:29:22', '', '2025-12-03 16:55:45', b'0');
