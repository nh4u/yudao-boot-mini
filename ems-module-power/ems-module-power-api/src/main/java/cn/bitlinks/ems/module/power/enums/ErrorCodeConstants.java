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
}
