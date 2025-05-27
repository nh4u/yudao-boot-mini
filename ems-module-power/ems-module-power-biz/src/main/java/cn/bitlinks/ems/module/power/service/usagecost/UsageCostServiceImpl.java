package cn.bitlinks.ems.module.power.service.usagecost;

import com.baomidou.dynamic.datasource.annotation.DS;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.usagecost.UsageCostDO;
import cn.bitlinks.ems.module.power.dal.mysql.usagecost.UsageCostMapper;
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

    @Override
    @TenantIgnore
    public List<UsageCostData> getList(StatisticsParamV2VO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getList(paramVO, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public LocalDateTime getLastTime(StatisticsParamV2VO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getLastTime(paramVO, startDate, endDate, standingBookIds);
    }

    @Override
    @TenantIgnore
    public void saveList(List<UsageCostDO> usageCostDOS) {
        log.info("saveList: {}", JSONUtil.toJsonStr(usageCostDOS));
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
}
