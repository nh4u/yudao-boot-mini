DROP TABLE IF EXISTS `power_standingbook_type`;
CREATE TABLE `power_standingboook_type`
(
    `id`          bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '名字',
    `super_id`    bigint                                                        COMMENT '父级类型编号'
    `top_type`    varchar(255)                                                   NOT NULL COMMENT '类型',
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

INSERT INTO`power_standingbook_type`(`id`, `name`, `super_id`, `super_name`, `top_type`, `sort`, `level`, `code`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (1, '重点设备', NULL, NULL, '1', 1, 1, '1', NULL, '系统', '2024-10-17 11:35:16', '系统', '2024-11-26 09:59:24', b'0', 1);
INSERT INTO`power_standingbook_type`(`id`, `name`, `super_id`, `super_name`, `top_type`, `sort`, `level`, `code`, `description`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`) VALUES (2, '计量器具', NULL, NULL, '2', 1, 1, '2', NULL, '系统', '2024-10-17 11:35:40', '系统', '2024-11-26 10:00:02', b'0', 1);

