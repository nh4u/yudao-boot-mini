ALTER TABLE power_warning_info
ADD COLUMN handle_opinion VARCHAR(500) NULL COMMENT '处理意见';

CREATE TABLE `power_double_carbon_settings` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统名称',
`url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '接口地址',
`update_frequency` int(11) NOT NULL COMMENT '更新频率',
`update_frequency_unit` tinyint(4) NOT NULL COMMENT '更新频率单位',
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
`standingbook_code` varchar(400) NOT NULL COMMENT '台账编码',
`double_carbon_code` varchar(400) DEFAULT NULL COMMENT '双碳编码',
`creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1956238533871136770 DEFAULT CHARSET=utf8mb4 COMMENT='双碳对接 映射';

## 注意插入字段配置
```sql
INSERT INTO `power_double_carbon_settings` (`id`, `name`, `url`, `update_frequency`, `update_frequency_unit`,  `tenant_id`) VALUES (1, '双碳系统', 'http://www.baidu.com', 10, 2, 1);
insert into infra_config (id, category, type, name, config_key, value, visible, remark, creator, create_time, updater, update_time, deleted) values (2, 'biz', 1, '设备详情跳转连接', 'power.device.monitor.url', '<a href="/aa/aa?id=%s">查看详情</a>', false, '设备详情跳转连接', 'admin', '2025-08-31 18:47:02', '1', '2025-09-02 18:01:29', false);
```
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
