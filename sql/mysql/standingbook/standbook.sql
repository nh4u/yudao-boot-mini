DROP TABLE IF EXISTS `power_standingbook_type`;
CREATE TABLE `power_standingboook_type`
(
    `id`          bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '编号',
    `name`        varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '名字',
    `super_id`    bigint                                                        NOT NULL COMMENT '父级类型编号',
    `super_name`  varchar(255)                                                  NOT NULL COMMENT '父级名字',
    `top_type`    varchar(10)                                                   NOT NULL COMMENT '类型',
    `sort`        bigint COMMENT '排序',
    `level`       bigint                                                        NOT NULL COMMENT '当前层级',
    `code`        varchar(63) COMMENT '编码',
    `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '简介',
    `creator`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
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
    `is_required`     varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '是否必填',
    `code`            varchar(63) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '编码',
    `sort`            bigint COMMENT '排序',
    `format`          varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '格式',
    `node`            varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '归属节点',
    `options`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '下拉框选项',
    `description`     varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '简介',
    `creator`         varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
    `create_time`     datetime                                                      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
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
    `creator`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL                                             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL                                             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)   NOT NULL                                             DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint   NOT NULL                                             DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '台账表';


