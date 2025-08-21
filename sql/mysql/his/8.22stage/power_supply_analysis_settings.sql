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

 Date: 21/08/2025 09:58:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for power_supply_analysis_settings
-- ----------------------------
DROP TABLE IF EXISTS `power_supply_analysis_settings`;
CREATE TABLE `power_supply_analysis_settings`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `system` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统',
  `item` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分析项',
  `standingbook_id` bigint(20) NULL DEFAULT NULL COMMENT '台账id',
  `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '供应分设置析表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of power_supply_analysis_settings
-- ----------------------------
INSERT INTO `power_supply_analysis_settings` VALUES (1, 'PCW', 'FAB1', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 18:01:11', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (2, 'PCW', 'FAB2', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 18:11:10', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (3, '低温水', 'FAB1', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 18:54:22', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (4, '低温水', 'FAB2', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 18:54:22', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (5, '低温水', '办公楼', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 20:33:42', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (6, '低温水', '宿舍楼', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 17:33:32', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (7, '中温水', 'FAB1', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 18:58:33', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (8, '中温水', 'FAB2', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 18:58:09', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (9, '中温水', '空压机冷却水', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 17:33:32', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (10, '中温水', 'PCW系统', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 18:58:14', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (11, '中温水', 'UPW系统', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 18:58:33', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (12, '中温水', 'CUB三层空调机', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 14:09:10', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (13, '中温水', 'CUB三层风机盘管', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 14:08:56', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (14, '热回收水/温水', 'FAB1', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 17:33:32', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (15, '热回收水/温水', 'FAB2', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 17:33:32', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (16, '热回收水/温水', 'CUB三层风机盘管', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 18:58:36', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (17, '热回收水/温水', 'UPW系统', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 14:08:33', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (18, '锅炉热水', 'FAB1', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 17:33:32', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (19, '锅炉热水', 'FAB2', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 17:33:32', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (20, '锅炉热水', '纯废水系统', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 18:58:27', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (21, '锅炉热水', '冷机热回收系统', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 18:58:37', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (22, '锅炉热水', 'CUB', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-02 17:33:32', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (23, '市政热水', '办公楼', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 17:00:59', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (24, '市政热水', '宿舍楼', NULL, '1', '2025-07-30 14:57:07', '1', '2025-08-05 13:52:33', b'0', 1);
INSERT INTO `power_supply_analysis_settings` VALUES (25, '市政热水', '6、7号楼', NULL, '1', '2025-07-30 14:57:07', '140', '2025-08-05 12:00:36', b'0', 1);

SET FOREIGN_KEY_CHECKS = 1;
