package cn.bitlinks.ems.module.power.service.usagecost;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostDiscountData;
import com.baomidou.dynamic.datasource.annotation.DS;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.mysql.usagecost.UsageCostMapper;

/**
 * @author wangl
 * @date 2025年05月13日 10:52
 */
@DS("starrocks")
@Service
@Validated
public class UsageCostServiceImpl implements UsageCostService{

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
    public List<UsageCostDiscountData> getDiscountList(StatisticsParamV2VO paramVO, LocalDateTime startDate, LocalDateTime endDate, List<Long> standingBookIds) {
        return usageCostMapper.getDiscountList(paramVO, startDate, endDate, standingBookIds);
    }
}
