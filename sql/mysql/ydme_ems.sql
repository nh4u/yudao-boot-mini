/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.2.26.3306
 Source Server Type    : MySQL
 Source Server Version : 50740
 Source Host           : 192.168.2.26:3306
 Source Schema         : ydme_ems

 Target Server Type    : MySQL
 Target Server Version : 50740
 File Encoding         : 65001

 Date: 22/10/2024 17:34:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ems_coal_factor_history
-- ----------------------------
DROP TABLE IF EXISTS `ems_coal_factor_history`;
CREATE TABLE `ems_coal_factor_history`  (
                                            `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                            `energy_id` bigint(20) NULL DEFAULT NULL COMMENT '能源id',
                                            `factor` decimal(10, 6) NULL DEFAULT NULL COMMENT '折标煤系数',
                                            `formula` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联计算公式',
                                            `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
                                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
                                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                            `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
                                            `start_time` datetime NULL DEFAULT NULL COMMENT '生效开始时间',
                                            `end_time` datetime NULL DEFAULT NULL COMMENT '生效结束时间',
                                            PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1848309107853414403 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '折标煤系数历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ems_coal_factor_history
-- ----------------------------
INSERT INTO `ems_coal_factor_history` VALUES (1848308020454936577, 1848307614165291010, 0.141000, NULL, '1', '2024-10-21 18:19:07', '1', '2024-10-21 18:19:07', b'0', 1, '2024-10-21 18:19:07', '2024-10-21 18:23:26');
INSERT INTO `ems_coal_factor_history` VALUES (1848309107853414402, 1848307614165291010, 0.141000, NULL, '1', '2024-10-21 18:23:26', '1', '2024-10-21 18:23:26', b'0', 1, '2024-10-21 18:23:26', NULL);

SET FOREIGN_KEY_CHECKS = 1;
/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.2.26.3306
 Source Server Type    : MySQL
 Source Server Version : 50740
 Source Host           : 192.168.2.26:3306
 Source Schema         : ydme_ems

 Target Server Type    : MySQL
 Target Server Version : 50740
 File Encoding         : 65001

 Date: 22/10/2024 17:34:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ems_energy_configuration
-- ----------------------------
DROP TABLE IF EXISTS `ems_energy_configuration`;
CREATE TABLE `ems_energy_configuration`  (
                                             `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                             `energy_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '能源名称',
                                             `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '编码',
                                             `energy_classify` int(1) NULL DEFAULT NULL COMMENT '能源分类',
                                             `energy_parameter` json NULL COMMENT '能源参数',
                                             `factor` decimal(10, 6) NULL DEFAULT NULL COMMENT '折标煤系数',
                                             `coal_formula` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '折标煤公式',
                                             `coal_scale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '折标煤小数位数',
                                             `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
                                             `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
                                             `billing_method` int(1) NULL DEFAULT NULL COMMENT '计费方式',
                                             `unit_price` json NULL COMMENT '单价详细',
                                             `unit_price_formula` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '单价公式',
                                             `unit_price_scale` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '单价小数位',
                                             `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
                                             `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
                                             `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                             `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                             `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
                                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1848307614165291011 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '能源配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ems_energy_configuration
-- ----------------------------
INSERT INTO `ems_energy_configuration` VALUES (1848307614165291010, '热力', '123418', 1, '[{\"m\": \"0.1\", \"n\": \"0.2\"}]', 0.141000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '1', '2024-10-21 18:17:30', '1', '2024-10-21 18:23:26', b'0', 1);

SET FOREIGN_KEY_CHECKS = 1;
/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.2.26.3306
 Source Server Type    : MySQL
 Source Server Version : 50740
 Source Host           : 192.168.2.26:3306
 Source Schema         : ydme_ems

 Target Server Type    : MySQL
 Target Server Version : 50740
 File Encoding         : 65001

 Date: 22/10/2024 17:34:47
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ems_unit_price_configuration
-- ----------------------------
DROP TABLE IF EXISTS `ems_unit_price_configuration`;
CREATE TABLE `ems_unit_price_configuration`  (
                                                 `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                                 `energy_id` bigint(20) NULL DEFAULT NULL COMMENT '能源id',
                                                 `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
                                                 `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
                                                 `billing_method` int(1) NULL DEFAULT NULL COMMENT '计费方式',
                                                 `accounting_frequency` int(1) NULL DEFAULT NULL COMMENT '核算频率',
                                                 `price_details` json NULL COMMENT '单价详细',
                                                 `formula` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '计算公式',
                                                 `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
                                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                 `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
                                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                 `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                                 `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
                                                 PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1848545708474986498 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '单价配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ems_unit_price_configuration
-- ----------------------------
INSERT INTO `ems_unit_price_configuration` VALUES (1848545708474986497, 1848307614165291010, '2024-10-16 18:32:57', '2024-10-30 18:32:57', 2, 1, '2', '1', '1', '2024-10-22 10:03:36', '1', '2024-10-22 10:05:20', b'0', 1);

SET FOREIGN_KEY_CHECKS = 1;
/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.2.26.3306
 Source Server Type    : MySQL
 Source Server Version : 50740
 Source Host           : 192.168.2.26:3306
 Source Schema         : ydme_ems

 Target Server Type    : MySQL
 Target Server Version : 50740
 File Encoding         : 65001

 Date: 22/10/2024 17:34:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ems_unit_price_history
-- ----------------------------
DROP TABLE IF EXISTS `ems_unit_price_history`;
CREATE TABLE `ems_unit_price_history`  (
                                           `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                           `energy_id` bigint(20) NULL DEFAULT NULL COMMENT '能源id',
                                           `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
                                           `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
                                           `billing_method` int(1) NULL DEFAULT NULL COMMENT '计费方式',
                                           `accounting_frequency` int(1) NULL DEFAULT NULL COMMENT '核算频率',
                                           `price_details` json NULL COMMENT '单价详细',
                                           `formula` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '计算公式',
                                           `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
                                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
                                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                           `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                           `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
                                           PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1848545708915388418 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '单价历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ems_unit_price_history
-- ----------------------------
INSERT INTO `ems_unit_price_history` VALUES (1848545708915388417, 1848307614165291010, '2024-10-16 18:32:57', '2024-10-16 18:32:57', 2, 1, '2', '1', '1', '2024-10-22 10:03:36', '1', '2024-10-22 10:03:36', b'0', 1);

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `ems_device_association_configuration` (
                                                        `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
                                                        `energy_id` bigint(20) DEFAULT NULL COMMENT '能源id',
                                                        `measurement_nstrument_id` bigint(20) DEFAULT NULL COMMENT '计量器具id',
                                                        `
                                                      device_id` bigint(20) DEFAULT NULL COMMENT '设备id',
                                                        `post_measurement` json DEFAULT NULL COMMENT '后置计量',
                                                        `pre_measurement` json DEFAULT NULL COMMENT '前置计量',
                                                        `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '创建者',
                                                        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                        `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '更新者',
                                                        `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                        `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
                                                        `tenant_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '租户编号',
                                                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备关联配置表';