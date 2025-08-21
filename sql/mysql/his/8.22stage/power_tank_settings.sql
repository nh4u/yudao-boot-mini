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

 Date: 21/08/2025 10:00:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for power_tank_settings
-- ----------------------------
DROP TABLE IF EXISTS `power_tank_settings`;
CREATE TABLE `power_tank_settings`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '储罐名称',
  `standingbook_id` bigint(20) NULL DEFAULT NULL COMMENT '台账id',
  `density` decimal(8, 5) NULL DEFAULT NULL COMMENT '密度ρ',
  `gravity_acceleration` decimal(8, 5) NULL DEFAULT NULL COMMENT '重力加速度g',
  `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
  `pressure_diff_id` bigint(20) NULL DEFAULT NULL COMMENT '设备压差id',
  `sort_no` int(6) NOT NULL DEFAULT 0 COMMENT '排序',
  `code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计量器具编码',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tank_query`(`deleted`, `tenant_id`, `name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '储罐液位设置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of power_tank_settings
-- ----------------------------
INSERT INTO `power_tank_settings` VALUES (11, '1#液氮储罐', NULL, null, null, '1', '2025-08-05 15:05:07', '140', '2025-08-15 14:09:01', b'0', 1, null, 0, 'N2-CG-01-YW');
INSERT INTO `power_tank_settings` VALUES (12, '2#液氮储罐', null, null, null, '1', '2025-08-05 15:05:11', '140', '2025-08-15 14:09:01', b'0', 1, null, 0, 'N2-CG-02-YW');
INSERT INTO `power_tank_settings` VALUES (13, '液氧储罐', NULL, null, null, '1', '2025-08-05 15:05:21', '140', '2025-08-15 14:09:01', b'0', 1, null, 0, 'O2-CG-01-YW');
INSERT INTO `power_tank_settings` VALUES (14, '液氩储罐', NULL, null, null, '1', '2025-08-05 15:05:30', '140', '2025-08-15 14:09:01', b'0', 1, null, 0, 'AR-CG-01-YW');

SET FOREIGN_KEY_CHECKS = 1;
