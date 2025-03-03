DROP TABLE IF EXISTS `power_standingbook_type`;
CREATE TABLE `power_standingboook_type`
(
    `id`          bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '名字',
    `super_id`    bigint                                                        COMMENT '父级类型编号'
    `top_type`    varchar(255)                                                   NOT NULL COMMENT '类型',
    `is_default`    varchar(255)                                                   NOT NULL COMMENT '是否默认',
    `sort`        bigint COMMENT '排序',
    `level`       bigint                                                        NOT NULL COMMENT '当前层级',
    `code`        varchar(255) COMMENT '编码',
    `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '简介',
    `creator`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)                                                        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint                                                        NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '台账类型表';

DROP TABLE IF EXISTS `power_standingboook_attribute`;
CREATE TABLE `power_standingbook_attribute`
(
    `id`              bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '属性名字',
    `value`           text COMMENT '属性值',
    `type_id`         bigint COMMENT '类型编号',
    `standingbook_id` bigint COMMENT '台账编号',
    `file_id`         bigint COMMENT '文件编号',
    `is_required`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '是否必填',
    `auto_generated`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '是否自动生成',
    `code`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '编码',
    `sort`            bigint COMMENT '排序',
    `format`          varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '格式',
    `node`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '归属节点',
    `options`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '下拉框选项',
    `description`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '简介',
    `creator`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
    `update_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         bit(1)                                                        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`       bigint                                                        NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '台账属性表';

DROP TABLE IF EXISTS `power_standingbook`;
CREATE TABLE `power_standingboook`
(
    `id`          bigint   NOT NULL AUTO_INCREMENT COMMENT '编号',
    `type_id`     bigint   NOT NULL COMMENT 'type编号',
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '属性名字',
    `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '简介',
    `creator`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL                                             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL                                             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)   NOT NULL                                             DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint   NOT NULL                                             DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '台账表';

INSERT INTO`power_standingbook_type`(`id`, `name`, `super_id`, `super_name`, `top_type`, `sort`, `level`, `code`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `is_default`) VALUES (1, '重点设备', NULL, NULL, '1', 1, 1, '1', NULL, '系统', '2024-10-17 11:35:16', '系统', '2024-11-26 09:59:24', b'0', 1, "1");
INSERT INTO`power_standingbook_type`(`id`, `name`, `super_id`, `super_name`, `top_type`, `sort`, `level`, `code`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `is_default`) VALUES (2, '计量器具', NULL, NULL, '2', 1, 1, '2', NULL, '系统', '2024-10-17 11:35:40', '系统', '2024-11-26 10:00:02', b'0', 1, "1");

INSERT INTO `power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1872414551084617730, '表类型', NULL, 2, NULL, NULL, '0', 'tableType', 1, 'SELECT', '计量器具', '实体表计;虚拟表计', '系统生成：表类型', '1', '2025-02-20 11:21:19', '1', '2025-02-20 11:22:43', b'0', 1, '0');
INSERT INTO `power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1872414551239806977, '计量器具编号', NULL, 2, NULL, NULL, '0', 'measuringInstrumentId', 2, 'TEXT', '计量器具', NULL, '系统生成：计量器具编号', '1', '2025-02-20 11:21:19', '1', '2025-02-20 11:22:43', b'0', 1, '0');
INSERT INTO `power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1872414551403384833, '计量器具名称', NULL, 2, NULL, NULL, '0', 'measuringInstrumentName', 3, 'TEXT', '计量器具', NULL, '系统生成：计量器具名称', '1', '2025-02-20 11:21:19', '1', '2025-02-20 11:22:43', b'0', 1, '0');
INSERT INTO `power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1872414551579545602, '能源', NULL, 2, NULL, NULL, '0', 'energy', 4, 'SELECT', '计量器具', 'energy', '系统生成：能源', '1', '2025-02-20 11:21:19', '1', '2025-02-20 11:22:43', b'0', 1, '0');
INSERT INTO `power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1872414551755706370, '数值类型', NULL, 2, NULL, NULL, '0', 'valueType', 5, 'SELECT', '计量器具', '抄表数;用量数', '系统生成：数值类型', '1', '2025-02-20 11:21:19', '1', '2025-02-20 11:22:43', b'0', 1, '0');
INSERT INTO `power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1872414552456155137, '虚拟表', NULL, 2, NULL, NULL, '1', 'virtualTable', 9, 'TEXT', '计量器具', NULL, '系统生成：虚拟表', '1', '2025-02-20 11:21:19', '1', '2025-02-20 11:22:43', b'0', 1, '0');

INSERT INTO`power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1874699451946893313, '设备编号', NULL, 1, NULL, NULL, '0', 'equipmentId', 1, 'TEXT', '重点设备', NULL, '系统生成：设备编号', '1', '2025-02-20 11:21:04', '1', '2025-02-26 18:42:06', b'0', 1, '0');
INSERT INTO`power_standingbook_attribute`(`id`, `name`, `value`, `type_id`, `standingbook_id`, `file_id`, `is_required`, `code`, `sort`, `format`, `node`, `options`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `auto_generated`) VALUES (1874699452118859778, '设备名称', NULL, 1, NULL, NULL, '0', 'equipmentName', 2, 'TEXT', '重点设备', NULL, '系统生成：设备名称', '1', '2025-02-20 11:21:04', '1', '2025-02-26 18:42:06', b'0', 1, '0');

