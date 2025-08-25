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

 Date: 21/08/2025 10:00:16
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for power_transformer_utilization_settings
-- ----------------------------
DROP TABLE IF EXISTS `power_transformer_utilization_settings`;
CREATE TABLE `power_transformer_utilization_settings`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `transformer_id` bigint(20) NOT NULL COMMENT '变压器',
  `load_current_id` bigint(20) NULL DEFAULT NULL COMMENT '负载电流',
  `voltage_level` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '电压等级',
  `rated_capacity` decimal(10, 2) NULL DEFAULT NULL COMMENT '额定容量',
  `creator` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint(20) NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1957403226516049922 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '变压器利用率设置' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of power_transformer_utilization_settings
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
