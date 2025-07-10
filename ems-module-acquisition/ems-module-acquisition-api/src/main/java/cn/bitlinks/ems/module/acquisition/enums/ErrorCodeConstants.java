package cn.bitlinks.ems.module.acquisition.enums;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;

/**
 * acquisition  错误码枚举类
 * <p>
 * acquisition 系统，使用 1-010-000-000 段
 */
public interface ErrorCodeConstants {

    ErrorCode STREAM_LOAD_RANGE_FAIL = new ErrorCode(1_010_000_003, "stream load插入数据失败");

    ErrorCode REDIS_MAX_PARTITION_NOT_EXIST = new ErrorCode(1_010_000_004, "redis最大分区不存在");
    ErrorCode CREATE_PARTITION_ERROR = new ErrorCode(1_010_000_005, "创建分区失败");
}
