package cn.bitlinks.ems.module.power.utils;

import cn.bitlinks.ems.module.power.enums.acquisition.AcquisitionFrequencyUnit;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_ACQUISITION_CORN_FAIL;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_ACQUISITION_CORN_UNIT_NOT_EXISTS;

/**
 * corn表达式工具类
 */
public class CornUtils {
    /**
     * 根据采集频率和采集单位获取定时表达式
     *
     * @param frequency     采集频率
     * @param frequencyUnit 采集频率单位 (1:秒, 2:分钟, 3:小时, 4:天)
     * @return cron表达式
     */
    public static String getCron(Long frequency, Integer frequencyUnit) {
        if (frequency == null || frequency <= 0 || frequencyUnit == null) {
            throw exception(STANDINGBOOK_ACQUISITION_CORN_FAIL);
        }

        AcquisitionFrequencyUnit acquisitionFrequencyUnit = AcquisitionFrequencyUnit.codeOf(frequencyUnit);
        String cron;
        switch (acquisitionFrequencyUnit) {
            case SECONDS: // 秒
                cron = String.format("0/%d * * * * ?", frequency);
                break;
            case MINUTES: // 分钟
                cron = String.format("0 0/%d * * * ?", frequency);
                break;
            case HOUR: // 小时
                cron = String.format("0 0 0/%d * * ?", frequency);
                break;
            case DAY: // 天
                cron = String.format("0 0 0 0/%d * ?", frequency);
                break;
            default:
                throw exception(STANDINGBOOK_ACQUISITION_CORN_UNIT_NOT_EXISTS, frequencyUnit);
        }
        return cron;
    }
}