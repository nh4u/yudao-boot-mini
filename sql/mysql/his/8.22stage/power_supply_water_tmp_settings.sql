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

 Date: 21/08/2025 09:58:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for power_supply_water_tmp_settings
-- ----------------------------
DROP TABLE IF EXISTS `power_supply_water_tmp_settings`;
CREATE TABLE `power_supply_water_tmp_settings`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标识',
  `system` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '系统',
  `standingbook_id` bigint(20) NULL DEFAULT NULL COMMENT '台账id',
  `energy_param_name` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '能源参数名称',
  `energy_param_code` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '能源参数编码（数据表用）',
  `max` decimal(5, 2) NULL DEFAULT NULL COMMENT '上限',
  `min` decimal(5, 2) NULL DEFAULT NULL COMMENT '下限',
  `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '供水温度报表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of power_supply_water_tmp_settings
-- ----------------------------
INSERT INTO `power_supply_water_tmp_settings` VALUES (1, 'LTWT', '低温水供水温度', null, '瞬时温度', null, null, null, '1', '2025-08-05 18:40:34', '140', '2025-08-20 16:48:08', b'0', 1);
INSERT INTO `power_supply_water_tmp_settings` VALUES (2, 'MTWT', '中温水供水温度', null, '瞬时温度', null, null, null, '1', '2025-08-05 18:40:45', '140', '2025-08-20 16:48:08', b'0', 1);
INSERT INTO `power_supply_water_tmp_settings` VALUES (3, 'HRWT', '热回收水供水温度', null, '瞬时温度', null, null, null, '1', '2025-08-05 18:40:49', '140', '2025-08-20 16:48:08', b'0', 1);
INSERT INTO `power_supply_water_tmp_settings` VALUES (4, 'BHWT', '热水供水温度（锅炉出水）', null, '瞬时温度', null, null, null, '1', '2025-08-05 18:40:57', '140', '2025-08-20 16:48:08', b'0', 1);
INSERT INTO `power_supply_water_tmp_settings` VALUES (5, 'MHWT', '热水供水温度（市政出水）', null, '瞬时温度', null, null, null, '1', '2025-08-05 18:41:04', '140', '2025-08-20 16:48:08', b'0', 1);
INSERT INTO `power_supply_water_tmp_settings` VALUES (6, 'PCWP', 'PCW供水压力温度（供水压力）', null, '压力', null, null, null, '1', '2025-08-05 18:41:18', '140', '2025-08-20 16:48:08', b'0', 1);
INSERT INTO `power_supply_water_tmp_settings` VALUES (7, 'PCWT', 'PCW供水压力温度（供水温度）', null, '瞬时温度', null, null, null, '1', '2025-08-05 18:41:21', '140', '2025-08-20 16:48:08', b'0', 1);

SET FOREIGN_KEY_CHECKS = 1;
