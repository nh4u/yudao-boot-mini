DROP TABLE IF EXISTS `power_warning_info`;
CREATE TABLE `power_warning_info` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`user_id` bigint(20) NOT NULL COMMENT '用户id',
`level` tinyint(4) NOT NULL COMMENT '告警等级：紧急4 重要3 次要2 警告1 提示0',
`warning_time` datetime NOT NULL COMMENT '告警时间',
`status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '处理状态:0-未处理1-处理中2-已处理',
`device_rel` varchar(255) NOT NULL COMMENT '设备名称与编号',
`template_id` bigint(20) NOT NULL COMMENT '模板id',
`title` text NOT NULL COMMENT '标题',
`content` text NOT NULL COMMENT '内容',
`creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 COMMENT='告警信息表';

DROP TABLE IF EXISTS `power_warning_strategy`;
CREATE TABLE `power_warning_strategy` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`name` varchar(30) NOT NULL COMMENT '规则名称',
`description` varchar(255) NOT NULL COMMENT '描述',
`device_scope` text COMMENT '设备范围',
`device_type_scope` text COMMENT '设备分类范围',
`condition` text NOT NULL COMMENT '告警条件',
`level` tinyint(4) NOT NULL COMMENT '告警等级：紧急4 重要3 次要2 警告1 提示0',
`site_template_id` bigint(20) NOT NULL COMMENT '站内信模板id',
`mail_template_id` bigint(20) DEFAULT NULL COMMENT '邮件模板id',
`site_staff` text NOT NULL COMMENT '站内信人员',
`mail_staff` text COMMENT '邮件人员',
`common_staff` text COMMENT '公共人员通知',
`interval` varchar(20) NOT NULL COMMENT '告警间隔',
`interval_unit` tinyint(4) NOT NULL COMMENT '告警间隔单位',
`status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '开启状态',
`creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=88 DEFAULT CHARSET=utf8mb4 COMMENT='告警策略表';

DROP TABLE IF EXISTS `power_warning_template`;
CREATE TABLE `power_warning_template` (
`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
`name` varchar(63) NOT NULL COMMENT '模板名称',
`code` varchar(63) NOT NULL COMMENT '模板编码',
`content` text NOT NULL COMMENT '模板内容',
`title` varchar(255) NOT NULL COMMENT '模板标题',
`t_params` varchar(255) NOT NULL COMMENT '标题参数数组',
`params` varchar(255) NOT NULL COMMENT '内容参数数组',
`remark` varchar(255) DEFAULT NULL COMMENT '备注',
`type` tinyint(4) NOT NULL COMMENT '模板类型:0-站内信1-邮件',
`creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
`create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
`updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
`update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
`deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
`tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='告警模板表';

