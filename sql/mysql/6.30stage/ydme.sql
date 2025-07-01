use ydme_ems;
-- 修改字段范围
ALTER TABLE ems_additional_recording MODIFY COLUMN this_value DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '本次数值';
ALTER TABLE ems_additional_recording MODIFY COLUMN pre_value DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '上次采集值';
ALTER TABLE ems_price_detail MODIFY COLUMN `usage_min` DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '档位用量下限';
ALTER TABLE ems_price_detail MODIFY COLUMN `usage_max` DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '档位用量上限';
ALTER TABLE ems_price_detail MODIFY COLUMN `unit_price` DECIMAL ( 30, 10 ) DEFAULT NULL COMMENT '单价';
ALTER TABLE ems_voucher MODIFY COLUMN `price` DECIMAL ( 30, 10 ) DEFAULT '0.00000' COMMENT '金额';
ALTER TABLE ems_voucher MODIFY COLUMN `usage` DECIMAL ( 30, 10 ) DEFAULT '0.00000' COMMENT '用量';
-- 更新普通角色的数据权限为全部数据范围
update system_role set data_scope = 1 where code = 'common';

DROP TABLE IF EXISTS `power_cop_settings`;
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
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 'm5', '正向瞬时流量',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 'm6', '正向瞬时流量',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ('LTC', 2, 'm7', '正向瞬时流量',  1);
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
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 'm5', '正向瞬时流量',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 'm6', '正向瞬时流量',  1);
INSERT INTO `power_cop_settings` (`cop_type`, `data_feature`, `param`, `param_cn_name`, `tenant_id`) VALUES ( 'LTS', 2, 'm7', '正向瞬时流量',  1);
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
DROP TABLE IF EXISTS `power_cop_formula`;
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
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES ('LTC', 'avg = AVG(m5 > 100 ? t1 : 0,m6 > 100 ? t2 : 0,m7 > 100 ? t3 : 0); return avg <= 0 ? null : 4.2 * (m1 + m2 + m3 + m4) * Math.abs(avg - t4) / ((W1 + W2 + W3 ) * 3.6);', '', '2025-06-20 13:51:15', '', '2025-06-26 10:04:09', b'0', 1);
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES ('LTS', 'avg = AVG(m5 > 100 ? t1 : 0,m6 > 100 ? t2 : 0,m7 > 100 ? t3 : 0); return avg <= 0 ? null : 4.2 * (m1 + m2 + m3 + m4) * Math.abs(avg - t4) / ((W1 + W2 + W3 + W4 + W5 + W6) * 3.6);', '', '2025-06-20 13:51:15', '', '2025-06-26 10:04:17', b'0', 1);
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES ('MTC', '4.2 * (m1 + m2 + m3 + m4+m5 + m6 + m7) * Math.abs(t1 - t2) / ((W1 + W2 + W3 + W4 + W5 ) * 3.6);', '', '2025-06-20 13:51:15', '', '2025-06-26 10:04:28', b'0', 1);
INSERT INTO `power_cop_formula` ( `cop_type`, `formula`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES ('MTS', '4.2 * (m1 + m2 + m3 + m4+m5 + m6 + m7) * Math.abs(t1 - t2) / ((W1+W2+W3+W4+W5+W6+W7+W8+W9+W10+W11+W12+W13+W14+W15+W16+W17) * 3.6);', '', '2025-06-20 13:51:15', '', '2025-06-26 10:04:32', b'0', 1);


INSERT INTO `system_dict_type` ( `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`) VALUES ('COP系统类型', 'cop_type', 0, 'COP系统类型', '1', '2025-05-09 13:40:13', '1', '2025-06-22 16:27:56', b'0', '2025-05-12 15:34:02');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 1, '低温冷机', 'LTC', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:28:41', '1', '2025-06-22 16:28:41', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 2, '低温系统', 'LTS', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:28:52', '1', '2025-06-22 16:28:52', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ( 3, '中温冷机', 'MTC', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:29:11', '1', '2025-06-22 16:29:11', b'0');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES (4, '中温系统', 'MTS', 'cop_type', 0, '', '', '', '1', '2025-06-22 16:29:23', '1', '2025-06-22 16:29:23', b'0');




-- 报表菜单
INSERT INTO `system_menu` (`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ('报表管理', '', 2, 1, 0, '/report', 'ep:pie-chart', 'report/index', 'reportIndex', 0, b'1', b'1', b'1', '1', '2025-05-28 13:56:12', '1', '2025-06-24 18:37:50', b'0');
