package cn.bitlinks.ems.module.power.enums;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;

/**
 * Infra 错误码枚举类
 * <p>
 * infra 系统，使用 1-001-000-000 段
 */
public interface ErrorCodeConstants {
    // ========== 能源配置 1-001-301-000 ==========
    ErrorCode COAL_FACTOR_HISTORY_NOT_EXISTS = new ErrorCode(1_001_301_000, "折标煤系数历史不存在");
    ErrorCode UNIT_PRICE_HISTORY_NOT_EXISTS = new ErrorCode(1_001_301_001, "单价历史不存在");
    ErrorCode ENERGY_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_002, "能源配置不存在");
    ErrorCode UNIT_PRICE_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_003, "单价配置不存在");
    ErrorCode TIME_CONFLICT = new ErrorCode(1_001_301_004, "与已有单价时间重叠");

    ErrorCode FORMULA_TYPE_NOT_EXISTS = new ErrorCode(1_001_301_004, "公式类型不存在");
    ErrorCode ENERGY_CODE_DUPLICATE = new ErrorCode(1_001_301_006, "能源编码重复");
    ErrorCode ENERGY_NAME_DUPLICATE = new ErrorCode(1_001_301_007, "能源名称重复");
    ErrorCode ENERGY_CONFIGURATION_STANDINGBOOK_EXISTS = new ErrorCode(1_001_301_008, "该能源关联计量器具，不可进行删除");
    ErrorCode ENERGY_CONFIGURATION_STANDINGBOOK_UNIT = new ErrorCode(1_001_301_009, "该能源已有参数单位，且已关联计量器具，不可进行修改");

    //========== 设备关联配置 1-001-301-004 ==========
    ErrorCode DEVICE_ASSOCIATION_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_004, "设备关联配置不存在");

    //========== 数采补录 1-001-301-005 ==========
    ErrorCode ADDITIONAL_RECORDING_NOT_EXISTS = new ErrorCode(1_001_301_005, "补录不存在");
    ErrorCode THIS_TIME_EXISTS_DATA = new ErrorCode(1_001_301_007, "该时间点已存在非补录数据，请选择其他时间");
    ErrorCode THIS_VALUE_NOT_LESS = new ErrorCode(1_001_301_008, "本次数值不可小于上次数值");
    ErrorCode THIS_VALUE_NOT_MORE = new ErrorCode(1_001_301_008, "本次数值不可大于下次数值");

    // ========== 台账类型 ==========
    ErrorCode STANDINGBOOK_TYPE_NOT_EXISTS = new ErrorCode(1_001_202_000, "台账类型不存在");
    ErrorCode STANDINGBOOK_TYPE_EXITS_CHILDREN = new ErrorCode(1_001_202_001, "存在存在子台账类型，无法删除");
    ErrorCode STANDINGBOOK_TYPE_PARENT_NOT_EXITS = new ErrorCode(1_001_202_002, "父级台账类型不存在");
    ErrorCode STANDINGBOOK_TYPE_PARENT_ERROR = new ErrorCode(1_001_202_003, "不能设置自己为父台账类型");
    ErrorCode STANDINGBOOK_TYPE_NAME_DUPLICATE = new ErrorCode(1_001_202_004, "已经存在该名字的台账类型");
    ErrorCode STANDINGBOOK_TYPE_PARENT_IS_CHILD = new ErrorCode(1_001_202_005, "不能设置自己的子StandingboookType为父StandingboookType");
    ErrorCode STANDINGBOOK_ATTRIBUTE_NOT_EXISTS = new ErrorCode(1_001_202_006, "台账属性不存在");
    ErrorCode STANDINGBOOK_NOT_EXISTS = new ErrorCode(1_001_202_007, "台账不存在");
    ErrorCode STANDINGBOOK_EXISTS = new ErrorCode(1_001_202_008, "该分类存在设备，不可进行删除");

    ErrorCode STANDINGBOOK_CODE_REPEAT_CHILDREN = new ErrorCode(1_001_202_009, "模板编码与子级台账类型编码重复，请修改后再提交");
    ErrorCode STANDINGBOOK_CODE_EXISTS = new ErrorCode(1_001_202_010, "台账编码已存在，请检查后提交");
    // ========== 标签配置 ==========
    ErrorCode LABEL_CONFIG_NOT_EXISTS = new ErrorCode(1_001_401_001, "配置标签不存在");
    ErrorCode LABEL_CONFIG_REACH_LIMIT = new ErrorCode(1_001_401_002, "单层标签超过限制");
    ErrorCode LABEL_CONFIG_REACH_LAYER_LIMIT = new ErrorCode(1_001_401_003, "标签层数超过限制");
    ErrorCode LABEL_CONFIG_CODE_DUPLICATE = new ErrorCode(1_001_401_004, "标签编码已存在");
    ErrorCode LABEL_CONFIG_CODE_REQUIRED = new ErrorCode(1_001_401_005, "标签编码不能为空");
    ErrorCode LABEL_CONFIG_HAS_DEVICE = new ErrorCode(1006, "该标签存在设备，不可进行删除");
    ErrorCode LABEL_CONFIG_CHILDREN_HAS_DEVICE = new ErrorCode(1006, "该标签子级存在设备，不可进行删除");

    // ========== 凭证管理 ==========
    ErrorCode VOUCHER_NOT_EXISTS = new ErrorCode(1_001_501_001, "凭证不存在");
    ErrorCode VOUCHER_LIST_IS_EMPTY = new ErrorCode(1_001_501_002, "凭证ID列表为空");
    ErrorCode VOUCHER_USAGE_MODIFIED_ERROR = new ErrorCode(1_001_501_003, "凭证ID已在数据补录中使用，无法修改用量值");
    ErrorCode VOUCHER_HAS_ADDITIONAL_RECORDING = new ErrorCode(1_001_501_004, "该凭证信息已关联补录数据，不可进行删除！");
    ErrorCode DA_PARAM_FORMULA_NOT_EXISTS = new ErrorCode(1_001_601_001, "参数公式不存在");



    // ========== 告警管理 ==========
    ErrorCode WARNING_INFO_NOT_EXISTS = new ErrorCode(1_001_701_001, "告警信息不存在");
    ErrorCode WARNING_TEMPLATE_NOT_EXISTS = new ErrorCode(1_001_701_002, "告警模板不存在");
    ErrorCode WARNING_TEMPLATE_CODE_EXISTS = new ErrorCode(1_001_701_003, "邮件模版 code({}) 已存在");
    ErrorCode WARNING_TEMPLATE_DELETE_ERROR = new ErrorCode(1_001_701_004, "该模板已关联告警规则，不可删除！");
    ErrorCode WARNING_TEMPLATE_DELETE_BATCH_ERROR = new ErrorCode(1_001_701_005, "{}模板已关联告警规则，不可删除！");
    ErrorCode WARNING_STRATEGY_NOT_EXISTS = new ErrorCode(1_001_701_006, "告警策略不存在");
    // ========== 其他业务错误 ==========

    ErrorCode DATE_RANGE_NOT_EXISTS = new ErrorCode(1_001_601_001, "日期范围不存在");
    ErrorCode DATE_RANGE_EXCEED_LIMIT = new ErrorCode(1_001_601_002, "日期范围超出限制（MAX：366）");
    ErrorCode QUERY_TYPE_NOT_EXISTS = new ErrorCode(1_001_601_003, "查看类型不存在");
    ErrorCode DATE_TYPE_NOT_EXISTS = new ErrorCode(1_001_601_004, "时间类型不存在");
    ErrorCode BENCH_MARK_NOT_EXISTS = new ErrorCode(1_001_601_005, "基准年限不存在");
}
