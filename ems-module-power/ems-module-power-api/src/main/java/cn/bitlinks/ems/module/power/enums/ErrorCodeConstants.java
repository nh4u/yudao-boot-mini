package cn.bitlinks.ems.module.power.enums;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;

/**
 * Infra 错误码枚举类
 *
 * infra 系统，使用 1-001-000-000 段
 */
public interface ErrorCodeConstants {
    // ========== 能源配置 1-001-301-000 ==========
    ErrorCode COAL_FACTOR_HISTORY_NOT_EXISTS = new ErrorCode(1_001_301_000, "折标煤系数历史不存在");
    ErrorCode UNIT_PRICE_HISTORY_NOT_EXISTS = new ErrorCode(1_001_301_001, "单价历史不存在");
    ErrorCode ENERGY_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_002, "能源配置不存在");
    ErrorCode UNIT_PRICE_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_003, "单价配置不存在");

    //========== 设备关联配置 1-001-301-004 ==========
    ErrorCode DEVICE_ASSOCIATION_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_004, "设备关联配置不存在");

    //========== 设备关联配置 1-001-301-005 ==========
    ErrorCode ADDITIONAL_RECORDING_NOT_EXISTS = new ErrorCode(1_001_301_005, "补录不存在");

// ========== 台账类型 ==========
    ErrorCode STANDINGBOOK_TYPE_NOT_EXISTS = new ErrorCode(1_001_202_000, "台账类型不存在");
    ErrorCode STANDINGBOOK_TYPE_EXITS_CHILDREN = new ErrorCode(1_001_202_001, "存在存在子台账类型，无法删除");
    ErrorCode STANDINGBOOK_TYPE_PARENT_NOT_EXITS = new ErrorCode(1_001_202_002,"父级台账类型不存在");
    ErrorCode STANDINGBOOK_TYPE_PARENT_ERROR = new ErrorCode(1_001_202_003, "不能设置自己为父台账类型");
    ErrorCode STANDINGBOOK_TYPE_NAME_DUPLICATE = new ErrorCode(1_001_202_004, "已经存在该名字的台账类型");
    ErrorCode STANDINGBOOK_TYPE_PARENT_IS_CHILD = new ErrorCode(1_001_202_005, "不能设置自己的子StandingboookType为父StandingboookType");
    ErrorCode STANDINGBOOK_ATTRIBUTE_NOT_EXISTS = new ErrorCode(1_001_202_006, "台账属性不存在");
    ErrorCode STANDINGBOOK_NOT_EXISTS = new ErrorCode(1_001_202_007, "台账不存在");

    // ========== 标签配置 ==========
    ErrorCode LABEL_CONFIG_NOT_EXISTS = new ErrorCode(1_001_401_001, "配置标签不存在");
    ErrorCode LABEL_CONFIG_REACH_LIMIT = new ErrorCode(1_001_401_002, "单层标签超过限制");
    ErrorCode LABEL_CONFIG_REACH_LAYER_LIMIT = new ErrorCode(1_001_401_003, "标签层数超过限制");
    ErrorCode LABEL_CONFIG_CODE_NOT_UNIQUE = new ErrorCode(1_001_401_004, "标签编码重复");

    // ========== 凭证管理 ==========
    ErrorCode VOUCHER_NOT_EXISTS = new ErrorCode(1_001_501_001, "凭证不存在");
    ErrorCode VOUCHER_LIST_IS_EMPTY = new ErrorCode(1_001_501_002, "凭证ID列表为空");
    ErrorCode VOUCHER_USAGE_MODIFIED_ERROR = new ErrorCode(1_001_501_003, "凭证ID已在数据补录中使用，无法修改用量值");

    ErrorCode DA_PARAM_FORMULA_NOT_EXISTS = new ErrorCode(1_001_601_001, "参数公式不存在");



    // ========== 其他业务错误 ==========

    ErrorCode DATE_RANGE_NOT_EXISTS = new ErrorCode(1_001_601_001, "日期范围不存在");
    ErrorCode DATE_RANGE_EXCEED_LIMIT = new ErrorCode(1_001_601_002, "日期范围超出限制（MAX：366）");
}
