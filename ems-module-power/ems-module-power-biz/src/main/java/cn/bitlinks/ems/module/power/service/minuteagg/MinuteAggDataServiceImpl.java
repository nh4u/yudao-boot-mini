package cn.bitlinks.ems.module.power.service.minuteagg;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateData;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.SupplyWaterTmpMinuteAggData;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.MinuteAggDataDTO;
import cn.bitlinks.ems.module.power.dal.mysql.minuteagg.MinuteAggregateDataMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;


/**
 * @author liumingqiang
 */
@DS("starrocks")
@Slf4j
@Service
@Validated
public class MinuteAggDataServiceImpl implements MinuteAggDataService {

    @Resource
    private MinuteAggregateDataMapper minuteAggregateDataMapper;

    @Override
    @TenantIgnore
    public List<SupplyWaterTmpMinuteAggData> getTmpRangeDataSteady(List<Long> standingbookIds, List<String> paramCodes, LocalDateTime starTime, LocalDateTime endTime) {
        return minuteAggregateDataMapper.getTmpRangeDataSteady(standingbookIds, paramCodes, starTime, endTime);
    }

    @Override
    public List<MinuteAggDataDTO> getMaxDataGpByDateType(List<Long> standingbookIds, List<String> paramCodes, Integer dateType, LocalDateTime starTime, LocalDateTime endTime) {
        return minuteAggregateDataMapper.getMaxDataGpByDateType(standingbookIds, paramCodes, dateType, starTime, endTime);
    }

    @Override
    public LocalDateTime getLastTime(List<Long> standingbookIds, List<String> paramCodes, LocalDateTime starTime, LocalDateTime endTime) {
        return minuteAggregateDataMapper.getLastTime(standingbookIds, paramCodes, starTime, endTime);
    }

    /**
     * 根据台账 能源 参数 时间 获取数采数据
     *
     * @param standingbookId
     * @param paramCode
     * @param dateType
     * @param energyFlag
     * @param starTime
     * @param endTime
     * @return
     */
    @Override
    public List<MinuteAggregateData> getList(
            Long standingbookId,
            String paramCode,
            Integer dateType,
            Integer energyFlag,
            Integer dataFeature,
            LocalDateTime starTime,
            LocalDateTime endTime) {
        return minuteAggregateDataMapper.getList(standingbookId, paramCode, dateType, energyFlag, dataFeature, starTime, endTime);
    }

    /**
     * 根据台账 能源 参数 时间 获取实时数采数据
     *
     * @param standingbookId
     * @param paramCode
     * @param dateType
     * @param energyFlag
     * @param starTime
     * @param endTime
     * @return
     */
    @Override
    public List<MinuteAggregateData> getRealTimeList(
            Long standingbookId,
            String paramCode,
            Integer dateType,
            Integer energyFlag,
            LocalDateTime starTime,
            LocalDateTime endTime) {
        return minuteAggregateDataMapper.getRealTimeList(standingbookId, paramCode, dateType, energyFlag, starTime, endTime);
    }
}
