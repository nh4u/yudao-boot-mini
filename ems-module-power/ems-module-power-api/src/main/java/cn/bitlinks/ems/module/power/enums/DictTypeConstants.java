package cn.bitlinks.ems.module.power.enums;

/**
 * Infra 字典类型的枚举类
 *
 * @author bitlinks
 */
public interface DictTypeConstants {

    String JOB_STATUS = "infra_job_status"; // 定时任务状态的枚举
    String JOB_LOG_STATUS = "infra_job_log_status"; // 定时任务日志状态的枚举

    String API_ERROR_LOG_PROCESS_STATUS = "infra_api_error_log_process_status"; // API 错误日志的处理状态的枚举

    String CONFIG_TYPE = "infra_config_type"; // 参数配置类型
    String BOOLEAN_STRING = "infra_boolean_string"; // Boolean 是否类型

    String OPERATE_TYPE = "infra_operate_type"; // 操作类型
    String WARNING_INFO_LEVEL = "warning_level"; // 告警等级
    String ACQUISITION_PROTOCOL = "acquisition_protocol"; // 数采服务协议
    String ACQUISITION_FREQUENCY = "acquisition_frequency"; // 数采采集频率
    /**
     * 系统类型 低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS
     */
    String SYSTEM_TYPE = "cop_type";
    /**
     * 暖通报表-热力汇总统计项
     */
    String REPORT_HVAC_HEAT = "report_hvac_heat";
    /**
     * 暖通报表-天然气统计项
     */
    String REPORT_NATURAL_GAS = "report_natural_gas";
    /**
     * 暖通报表-暖通电量统计标签
     */
    String REPORT_HVAC_ELECTRICITY = "report_hvac_electricity";
}
