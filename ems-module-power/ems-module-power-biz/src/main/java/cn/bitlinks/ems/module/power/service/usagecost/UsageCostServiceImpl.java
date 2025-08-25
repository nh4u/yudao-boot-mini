package cn.bitlinks.ems.module.power.service.usagecost;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import com.baomidou.dynamic.datasource.annotation.DS;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.starrocks.StreamLoadApi;
import cn.bitlinks.ems.module.acquisition.api.starrocks.dto.StreamLoadDTO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.usagecost.UsageCostDO;
import cn.bitlinks.ems.module.power.dal.mysql.usagecost.UsageCostMapper;
import cn.bitlinks.ems.module.power.dto.UsageCostDTO;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangl
 * @date 2025年05月13日 10:52
 */
@DS("starrocks")
@Slf4j
@Service
@Validated
public class UsageCostServiceImpl implements UsageCostService {

    @Resource
    private UsageCostMapper usageCostMapper;

    @Resource
    @Lazy
    private StreamLoadApi streamLoadApi;

    private static final String LABEL_PREFIX = "label_usage_cost";
    private static final String TABLE_NAME = "usage_cost";

    @Override
    @TenantIgnore
    public List<UsageCostData> getList(StatisticsParamV2VO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getList(paramVO, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public List<UsageCostData> getList(ConsumptionStatisticsParamVO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getList(paramVO, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public List<UsageCostData> getList(Integer dateType, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getTimeDataList(dateType, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public List<UsageCostData> getList( LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getDataList(startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public LocalDateTime getLastTime(Integer dateType, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getLastTime(dateType, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public LocalDateTime getLastTime(StatisticsParamV2VO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getLastTime(paramVO, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public LocalDateTime getLastTime(ConsumptionStatisticsParamVO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getLastTime(paramVO, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public void saveList(List<UsageCostDTO> usageCostDOS) {
        log.info("saveList size: {}", usageCostDOS.size());
        StreamLoadDTO dto = new StreamLoadDTO();
        dto.setData(usageCostDOS);
        dto.setLabel(LABEL_PREFIX + System.currentTimeMillis() + "_" + RandomUtil.randomNumbers(6));
        dto.setTableName(TABLE_NAME);
        streamLoadApi.streamLoadData(dto);
        log.info("saveList end");
    }

    @Override
    @TenantIgnore
    public List<UsageCostData> getListOfHome(LocalDateTime startDate, LocalDateTime endDate, List<Long> energyIdList) {
        return usageCostMapper.getListOfHome(startDate, endDate, energyIdList);
    }

    /**
     * 按能源和台账分组
     *
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    @Override
    @TenantIgnore
    public List<UsageCostData> getEnergyAndSbStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getEnergyAndSbStandardCoal(startDate, endDate, standingBookIds);
    }

    /**
     * 按能源分组
     *
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    @Override
    @TenantIgnore
    public List<UsageCostData> getEnergyStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getEnergyStandardCoal(startDate, endDate, standingBookIds);
    }

    /**
     * 按能源分组
     *
     * @param startDate
     * @param endDate
     * @param energyIds
     * @return
     */
    @Override
    @TenantIgnore
    public List<UsageCostData> getEnergyStandardCoalByEnergyIds(LocalDateTime startDate, LocalDateTime endDate, List<Long> energyIds) {
        return usageCostMapper.getEnergyStandardCoalByEnergyIds(startDate, endDate, energyIds);
    }

    /**
     * 按台账分组
     *
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    @Override
    @TenantIgnore
    public List<UsageCostData> getStandingbookStandardCoal(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getStandingbookStandardCoal(startDate, endDate, standingBookIds);
    }

    /**
     * 获取能源用量
     * @param dateType
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    @Override
    @TenantIgnore
    public List<UsageCostData> getEnergyUsage(Integer dateType, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getEnergyUsage(dateType, startDate, endDate, standingBookIds);
    }

    /**
     * 按台账分组
     *
     * @param startDate
     * @param endDate
     * @param standingBookIds
     * @return
     */
    @Override
    @TenantIgnore
    public List<UsageCostData> getUsageByStandingboookIdGroup(BaseTimeDateParamVO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getUsageByStandingboookIdGroup(paramVO,startDate, endDate, standingBookIds);
    }
    @Override
    @TenantIgnore
    public LocalDateTime getLastTimeNoParam(LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getLastTime2(startDate, endDate, standingBookIds);
    }

}
