/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.2.31
 Source Server Type    : MySQL
 Source Server Version : 50744
 Source Host           : 192.168.110.49:3307
 Source Schema         : ydme_ems

 Target Server Type    : MySQL
 Target Server Version : 50744
 File Encoding         : 65001

 Date: 21/08/2025 09:57:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for power_gas_measurement
-- ----------------------------
DROP TABLE IF EXISTS `power_gas_measurement`;
CREATE TABLE `power_gas_measurement`  (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `measurement_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '计量器具名称（可变，不作为依据）',
  `measurement_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计量器具编号（即台账code）',
  `sort_no` int(6) NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint(20) NOT NULL DEFAULT 1 COMMENT '租户编号',
  `energy_param` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '能源参数中文名',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of power_gas_measurement
-- ----------------------------
INSERT INTO `power_gas_measurement` VALUES (1, 'FAB1低压CDA流量计', 'LCDA-FAB1', 1, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (2, 'FAB1高压CDA流量计', 'HCDA-FAB1', 2, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (3, 'FAB2低压CDA流量计', 'LCDA-FAB2', 3, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (4, '氮气主管道流量', 'N2-GD-01-LL', 4, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (5, 'FAB 1F氮气主管道压力', 'N2-GD-01-YL1', 5, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (6, 'FAB 2F氮气主管道压力', 'N2-GD-01-YL2', 6, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (7, 'FAB 1F高纯氮气主管道压力', 'N2-GD-01-YL3', 7, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (8, 'FAB 2F高纯氮气主管道压力', 'N2-GD-01-YL4', 8, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (9, '1#液氮储罐液位', 'N2-CG-01-YW', 9, NULL, '2025-08-14 15:27:08', NULL, '2025-08-14 15:27:08', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (10, '1#液氮储罐压差', 'N2-CG-01-YC', 10, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (11, '1#液氮储罐压力', 'N2-CG-01-YL', 11, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (12, '2#液氮储罐液位', 'N2-CG-02-YW', 12, NULL, '2025-08-14 15:27:11', NULL, '2025-08-14 15:27:11', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (13, '2#液氮储罐压差', 'N2-CG-02-YC', 13, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (14, '2#液氮储罐压力', 'N2-CG-02-YL', 14, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (15, '1#氮气纯化器流量', 'N2-CHQ-01-LL', 15, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (16, '2#氮气纯化器流量', 'N2-CHQ-02-LL', 16, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (17, 'FAB1氢气流量', 'H2-FAB1-LL', 17, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (18, 'FAB1氢气压力', 'H2-FAB1-YL', 18, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (19, 'FAB2氢气流量', 'H2-FAB2-LL', 19, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (20, '1#氢气纯化器流量', 'H2-CHQ-01-LL', 20, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (21, '2#氢气纯化器流量', 'H2-CHQ-02-LL', 21, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (22, '氧气主管道流量', 'O2-GD-01-LL', 22, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (23, 'FAB 1F氧气主管道压力', 'O2-GD-01-YL1', 23, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (24, 'FAB 2F氧气主管道压力', 'O2-GD-01-YL2', 24, NULL, '2025-08-13 14:24:59', NULL, '2025-08-13 14:24:59', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (25, '液氧储罐液位', 'O2-CG-01-YW', 25, NULL, '2025-08-14 15:27:16', NULL, '2025-08-14 15:27:16', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (26, '液氧储罐压差', 'O2-CG-01-YC', 26, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (27, '液氧储罐压力', 'O2-CG-01-YL', 27, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (28, '1#氧气纯化器流量', 'O2-CHQ-01-LL', 28, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (29, '2#氧气纯化器流量', 'O2-CHQ-02-LL', 29, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (30, '氩气主管道流量', 'AR-GD-01-LL', 30, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (31, 'FAB 1F氩气主管道压力1', 'AR-GD-01-YL1', 31, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (32, 'FAB 2F氩气主管道压力2', 'AR-GD-01-YL2', 32, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (33, '氩气储罐液位', 'AR-CG-01-YW', 33, NULL, '2025-08-14 15:27:22', NULL, '2025-08-14 15:27:22', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (34, '氩气储罐压差', 'AR-CG-01-YC', 34, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压差');
INSERT INTO `power_gas_measurement` VALUES (35, '氩气储罐压力', 'AR-CG-01-YL', 35, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (36, '1#氩气纯化器流量', 'AR-CHQ-01-LL', 36, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (37, '2#氩气纯化器流量', 'AR-CHQ-02-LL', 37, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (38, '氦气主管道流量', 'HE-GD-01-LL', 38, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (39, 'FAB 1F氦气主管道压力1', 'HE-GD-01-YL1', 39, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (40, 'FAB2 1F氦气主管道压力2', 'HE-GD-01-YL2', 40, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (41, 'FAB 2F氦气主管道压力3', 'HE-GD-01-YL3', 41, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '压力');
INSERT INTO `power_gas_measurement` VALUES (42, '1#氦气纯化器流量', 'HE-CHQ-01-LL', 42, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');
INSERT INTO `power_gas_measurement` VALUES (43, '2#氦气纯化器流量', 'HE-CHQ-02-LL', 43, NULL, '2025-08-13 14:25:00', NULL, '2025-08-13 14:25:00', b'0', 1, '正累积');

SET FOREIGN_KEY_CHECKS = 1;
