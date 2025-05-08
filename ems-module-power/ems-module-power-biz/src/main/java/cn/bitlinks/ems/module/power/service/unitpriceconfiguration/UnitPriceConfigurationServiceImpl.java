package cn.bitlinks.ems.module.power.service.unitpriceconfiguration;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationRespVO;
import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailRespVO;
import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.PriceResultDTO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.UnitPriceConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.UnitPriceConfigurationRespVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.UnitPriceConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import cn.bitlinks.ems.module.power.dal.mysql.pricedetail.PriceDetailMapper;
import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.pricedetail.PriceDetailService;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 单价配置 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class UnitPriceConfigurationServiceImpl implements UnitPriceConfigurationService {

    @Resource
    private UnitPriceConfigurationMapper unitPriceConfigurationMapper;
    //    @Resource
//    private UnitPriceHistoryService unitPriceHistoryService;
//    @Resource
//    private UnitPriceHistoryMapper unitPriceHistoryMapper;
    @Resource
    private EnergyConfigurationService energyConfigurationService;
    @Resource
    private PriceDetailService priceDetailService;
    @Resource
    private PriceDetailMapper priceDetailMapper;

    private void insertBatch(Long energyId, List<UnitPriceConfigurationSaveReqVO> updateReqVOList) {
        EnergyConfigurationRespVO energyConfig = energyConfigurationService.getEnergyConfiguration(energyId);
        updateReqVOList.forEach(updReqVO -> {
            updReqVO.setFormula(energyConfig.getUnitPriceFormula());
        });
        List<UnitPriceConfigurationDO> unitPriceConfigList = BeanUtils.toBean(updateReqVOList, UnitPriceConfigurationDO.class);
        unitPriceConfigList.forEach(unitPriceConfigurationDO -> {
            unitPriceConfigurationDO.setEnergyId(energyId);
        });
        // 批量插入单价配置(周期)
        unitPriceConfigurationMapper.insertBatch(unitPriceConfigList);
        List<PriceDetailDO> priceDetailDOList = new ArrayList<>();
        unitPriceConfigList.forEach(unitPriceConfigurationDO -> {
            // 新增价格详情（子表）
            if (unitPriceConfigurationDO.getPriceDetails() != null && !unitPriceConfigurationDO.getPriceDetails().isEmpty()) {
                List<PriceDetailDO> priceDetails = unitPriceConfigurationDO.getPriceDetails().stream()
                        .map(d -> {
                            PriceDetailDO detail = BeanUtils.toBean(d, PriceDetailDO.class);
                            detail.setPriceId(unitPriceConfigurationDO.getId()); // 关联主表 ID
                            return detail;
                        })
                        .collect(Collectors.toList());
                priceDetailDOList.addAll(priceDetails);
            }
        });
        // 批量插入单价详情(收费)
        priceDetailMapper.insertBatch(priceDetailDOList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUnitPriceConfiguration(Long energyId, List<UnitPriceConfigurationSaveReqVO> updateReqVOList) {
        // 当前时间
        LocalDateTime currentTime = LocalDateTime.now();

        // 0.获取新配置的最小周期(当前周期,只会修改结束时间)  2025-03-01 - 2025-04-11 15 02 01
        updateReqVOList.sort(Comparator.comparing(UnitPriceConfigurationSaveReqVO::getStartTime));
        UnitPriceConfigurationSaveReqVO currentUpdConfig = updateReqVOList.get(0);

        // 1.获取原有的当前周期和未来周期
        List<UnitPriceConfigurationDO> rawConfigList = unitPriceConfigurationMapper.selectList(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .ge(UnitPriceConfigurationDO::getEndTime, currentTime)
                        .orderByAsc(UnitPriceConfigurationDO::getStartTime)
        );

        if (CollUtil.isEmpty(rawConfigList)) {
            // 1.1 没有找到.直接新增所有
            insertBatch(energyId, updateReqVOList);
            return;
        }
        // 存在情况当前周期的结束时间小于当前时间(极端情况,提交的当前时间和后端接收到的当前时间可能会不一致,可能会导致提交的配置的当前周期的结束时间会早于提交时的时间.)
        if (currentUpdConfig.getEndTime().isBefore(currentTime)) {
            throw exception(PAST_PERIOD_MODIFY_NOT_ALLOWED);
        }

        UnitPriceConfigurationDO existingConfig = rawConfigList.get(0);
        // 2.找到了, 做单价的数据对比
        // 检查价格详情变更和公式是否变更
        boolean changePrice =
                !Objects.equals(existingConfig.getBillingMethod(), currentUpdConfig.getBillingMethod())
                        || !Objects.equals(existingConfig.getAccountingFrequency(), currentUpdConfig.getAccountingFrequency())
                        || isPriceDetailsModified(existingConfig.getId(), currentUpdConfig.getPriceDetails())
                        || !Objects.equals(existingConfig.getFormulaId(), currentUpdConfig.getFormulaId());

        // 2.1 当前周期单价有改变,则把原有周期直接修改结束时间为当前时间,修改到数据库中
        if (changePrice) {
            // 修改原有当前周期的结束时间为当前时间,单价详细不变
            existingConfig.setEndTime(currentTime);
            unitPriceConfigurationMapper.update(new LambdaUpdateWrapper<UnitPriceConfigurationDO>()
                    .set(UnitPriceConfigurationDO::getEndTime, currentTime)
                    .eq(UnitPriceConfigurationDO::getId, existingConfig.getId())
            );
            // 删除结束时间
            rawConfigList.removeIf(rawConfig -> rawConfig.getId().equals(existingConfig.getId()));
            // 删除结束时间大于当前时间的
            // 修改当前周期的起始时间为当前时间的下一秒
            currentUpdConfig.setStartTime(currentTime.plusSeconds(1L));
        }
        //删除当前周期和未来周期,
        if(CollUtil.isNotEmpty(rawConfigList)){
            List<Long> rawConfigIds = rawConfigList.stream()
                    .map(UnitPriceConfigurationDO::getId)
                    .collect(Collectors.toList());
            unitPriceConfigurationMapper.deleteByIds(rawConfigIds);
            priceDetailMapper.delete(new LambdaQueryWrapper<PriceDetailDO>()
                    .in(PriceDetailDO::getPriceId, rawConfigIds));
        }

        // 新增变动周期 (拆分的当前周期和没拆分的当前周期)
        insertBatch(energyId, updateReqVOList);

    }

    // 判断价格详情是否变更
    private boolean isPriceDetailsModified(Long configId, List<PriceDetailSaveReqVO> newDetails) {
        List<PriceDetailDO> existingDetails = priceDetailService.getDetailsByPriceId(configId);
        if (existingDetails.size() != newDetails.size()) return true;

        // 转换 existingDetails：剥离非业务字段
        List<PriceDetailDO> existingDetailsForCompare = existingDetails.stream().map(d -> {
            PriceDetailDO detail = new PriceDetailDO();
            detail.setPeriodType(d.getPeriodType());
            detail.setPeriodStart(d.getPeriodStart());
            detail.setPeriodEnd(d.getPeriodEnd());
            detail.setUsageMin(d.getUsageMin());
            detail.setUsageMax(d.getUsageMax());
            detail.setUnitPrice(d.getUnitPrice());
            return detail;
        }).collect(Collectors.toList());

        // 转换 newDetails：修正字段映射，统一精度
        List<PriceDetailDO> newDetailsDO = newDetails.stream().map(d -> {
            PriceDetailDO detail = new PriceDetailDO();
            detail.setPeriodType(d.getPeriodType());
            detail.setPeriodStart(d.getPeriodStart());
            detail.setPeriodEnd(d.getPeriodEnd());
            BigDecimal usageMin = d.getUsageMin();
            BigDecimal usageMax = d.getUsageMax();

            if (usageMin != null) {
                detail.setUsageMin(usageMin.setScale(6, RoundingMode.HALF_UP));
            } else {
                detail.setUsageMin(null);
            }

            if (usageMax != null) {
                detail.setUsageMax(usageMax.setScale(6, RoundingMode.HALF_UP));
            } else {
                detail.setUsageMax(null);
            }
            BigDecimal price = d.getUnitPrice();
            detail.setUnitPrice(price.setScale(6, RoundingMode.HALF_UP));
            return detail;
        }).collect(Collectors.toList());

        return !CollectionUtils.isEqualCollection(existingDetailsForCompare, newDetailsDO);
    }

    @Override
    public PageResult<UnitPriceConfigurationRespVO> getUnitPriceConfigurationPage(UnitPriceConfigurationPageReqVO pageReqVO) {
        // 1. 分页查询主表数据
        PageResult<UnitPriceConfigurationDO> pageResult = unitPriceConfigurationMapper.selectPage(pageReqVO);
        List<UnitPriceConfigurationDO> configs = pageResult.getList();

        if (configs.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        // 2. 批量加载子表数据
        List<Long> configIds = configs.stream()
                .map(UnitPriceConfigurationDO::getId)
                .collect(Collectors.toList());
        Map<Long, List<PriceDetailDO>> priceDetailsMap = priceDetailService.getDetailsByPriceIds(configIds);

        // 3. 转换主表 DO -> VO（关联子表数据）
        List<UnitPriceConfigurationRespVO> voList = configs.stream().map(config -> {
            // 转换主表字段
            UnitPriceConfigurationRespVO vo = BeanUtils.toBean(config, UnitPriceConfigurationRespVO.class);

            // 处理子表数据
            List<PriceDetailRespVO> detailVOs = priceDetailsMap.getOrDefault(config.getId(), Collections.emptyList())
                    .stream()
                    .map(detail -> BeanUtils.toBean(detail, PriceDetailRespVO.class))
                    .collect(Collectors.toList());
            vo.setPriceDetails(detailVOs);

            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, pageResult.getTotal());
    }

    @Override
    public UnitPriceConfigurationDO getCurrentUnitConfigByEnergyId(Long energyId) {
        // 1. 查询主表列表（不带子表数据）
        UnitPriceConfigurationDO config = unitPriceConfigurationMapper.selectOne(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .le(UnitPriceConfigurationDO::getStartTime, LocalDateTime.now())
                        .ge(UnitPriceConfigurationDO::getEndTime, LocalDateTime.now())
        );

        if (Objects.isNull(config)) {
            return null;
        }

        List<PriceDetailDO> priceDetails = priceDetailService.getDetailsByPriceId(config.getId());

        config.setPriceDetails(priceDetails);

        return config;
    }

    @Override
    public List<UnitPriceConfigurationDO> getUnitPriceConfigurationByEnergyId(Long energyId) {
        // 1. 查询主表列表（不带子表数据）
        List<UnitPriceConfigurationDO> configs = unitPriceConfigurationMapper.selectList(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .ge(UnitPriceConfigurationDO::getEndTime, LocalDateTime.now())
                        .orderByAsc(UnitPriceConfigurationDO::getStartTime)
        );

        // 2. 批量加载子表数据并关联到主表
        if (CollUtil.isEmpty(configs)) {
            return configs;
        }
        // 提取主表 ID 列表
        List<Long> configIds = configs.stream()
                .map(UnitPriceConfigurationDO::getId)
                .collect(Collectors.toList());

        // 批量查询子表数据（按 price_id 分组）
        Map<Long, List<PriceDetailDO>> priceDetailsMap = priceDetailService.getDetailsByPriceIds(configIds);

        // 将子表数据设置到主表 DO 中
        configs.forEach(config ->
                config.setPriceDetails(priceDetailsMap.getOrDefault(config.getId(), Collections.emptyList()))
        );

        return configs;
    }

    @Override
    public LocalDateTime getLatestEndTime(Long energyId) {
        List<UnitPriceConfigurationDO> configs = unitPriceConfigurationMapper.selectList(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .orderByDesc(UnitPriceConfigurationDO::getEndTime)  // 按结束时间倒序排列
                        .last("LIMIT 1")  // 只取最新的一条
        );
        if (CollUtil.isEmpty(configs)) {
            return null;
        }

        return configs.get(0).getEndTime();
    }

    @Override
    public PriceResultDTO getPriceByTime(Long energyId, LocalDateTime targetTime) {
        // 获取主配置
        UnitPriceConfigurationDO config = getValidConfig(energyId, targetTime);

        // 获取关联的子表详情
        List<PriceDetailDO> priceDetails = priceDetailService.getDetailsByPriceId(config.getId());

        PriceResultDTO result = new PriceResultDTO();
        result.setPriceType(config.getBillingMethod());

        switch (config.getBillingMethod()) {
            case 1:
                handleFixedPrice(result, priceDetails);
                break;
            case 2:
                handleTimeBasedPrice(result, priceDetails, targetTime);
                break;
            case 3:
                handleLadderPrice(result, config, priceDetails, targetTime);
                break;
            default:
                throw exception(INVALID_PRICE_TYPE);
        }
        return result;
    }

    private UnitPriceConfigurationDO getValidConfig(Long energyId, LocalDateTime targetTime) {
        // 查询主表配置
        List<UnitPriceConfigurationDO> configs = unitPriceConfigurationMapper.selectList(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .le(UnitPriceConfigurationDO::getStartTime, targetTime)
                        .ge(UnitPriceConfigurationDO::getEndTime, targetTime)
        );

        if (configs.isEmpty()) {
            throw exception(UNIT_PRICE_CONFIGURATION_NOT_EXISTS);
        }

        // 批量加载子表数据
        List<Long> configIds = configs.stream()
                .map(UnitPriceConfigurationDO::getId)
                .collect(Collectors.toList());
        Map<Long, List<PriceDetailDO>> priceDetailsMap = priceDetailService.getDetailsByPriceIds(configIds);

        // 将子表数据关联到主表对象
        configs.forEach(config ->
                config.setPriceDetails(priceDetailsMap.getOrDefault(config.getId(), Collections.emptyList()))
        );

        return configs.get(0);
    }

    private void handleFixedPrice(PriceResultDTO result, List<PriceDetailDO> priceDetails) {
        if (priceDetails.isEmpty()) {
            throw exception(FIXED_PRICE_DETAILS_NOT_FOUND);
        }

        // 确保仅有一条有效子表记录
        if (priceDetails.size() > 1) {
            throw exception(INVALID_FIXED_PRICE_DETAILS);
        }

        PriceDetailDO detail = priceDetails.get(0);
        result.setFixedPrice(detail.getUnitPrice());
    }

    private void handleTimeBasedPrice(PriceResultDTO result,
                                      List<PriceDetailDO> priceDetails,
                                      LocalDateTime targetTime) {
        LocalTime queryTime = targetTime.toLocalTime();
        Map<String, BigDecimal> timePrices = new LinkedHashMap<>();

        for (PriceDetailDO detail : priceDetails) {
            LocalTime periodStart = detail.getPeriodStart();
            LocalTime periodEnd = detail.getPeriodEnd();

            if (isTimeWithinPeriod(queryTime, periodStart, periodEnd)) {
                String timeRange = formatTimeRange(periodStart, periodEnd);
                timePrices.put(timeRange, detail.getUnitPrice());
            }
        }

        if (timePrices.isEmpty()) {
            throw exception(NO_MATCHING_TIME_PERIOD);
        }

        result.setTimePrices(timePrices);
    }

    // 判断当前时间是否在时段内
    private boolean isTimeWithinPeriod(LocalTime queryTime, LocalTime start, LocalTime end) {
        return !queryTime.isBefore(start) && !queryTime.isAfter(end);
    }

    // 格式化时段范围（HH:mm:ss）
    private String formatTimeRange(LocalTime start, LocalTime end) {
        return String.format("%s-%s",
                start.format(DateTimeFormatter.ISO_TIME),
                end.format(DateTimeFormatter.ISO_TIME));
    }

    private void handleLadderPrice(PriceResultDTO result,
                                   UnitPriceConfigurationDO config,
                                   List<PriceDetailDO> priceDetails,
                                   LocalDateTime targetTime) {
        if (config.getAccountingFrequency() == null) {
            throw exception(ACCOUNTING_FREQUENCY_NOT_SET);
        }
        LocalDateTime periodStart = calculatePeriodStart(config.getAccountingFrequency(), targetTime);
        result.setPeriodStart(periodStart);

        // 转换并排序阶梯配置
        List<PriceResultDTO.LadderPrice> ladderPrices = priceDetails.stream()
                .sorted(Comparator.comparing(PriceDetailDO::getUsageMin))
                .map(detail -> {
                    PriceResultDTO.LadderPrice lp = new PriceResultDTO.LadderPrice();
                    lp.setMin(detail.getUsageMin());
                    lp.setMax(detail.getUsageMax());
                    lp.setPrice(detail.getUnitPrice());
                    return lp;
                })
                .collect(Collectors.toList());

        // 校验阶梯连续性
        validateLadderContinuity(ladderPrices);

        result.setLadderPrices(ladderPrices);
    }

    // 阶梯连续性校验
    private void validateLadderContinuity(List<PriceResultDTO.LadderPrice> ladderPrices) {
        for (int i = 1; i < ladderPrices.size(); i++) {
            BigDecimal prevMax = ladderPrices.get(i - 1).getMax();
            BigDecimal currMin = ladderPrices.get(i).getMin();
            if (prevMax.compareTo(currMin) != 0) {
                throw exception(INVALID_LADDER_CONTINUITY);
            }
        }
    }

    private LocalDateTime calculatePeriodStart(Integer accountingFrequency, LocalDateTime dateTime) {
        if (accountingFrequency == null) {
            throw exception(ACCOUNTING_FREQUENCY_NOT_SET);
        }
        switch (accountingFrequency) {
            case 1: // 按月
                return dateTime.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            case 2: // 按季
                int quarter = (dateTime.getMonthValue() - 1) / 3;
                return dateTime.withMonth(quarter * 3 + 1)
                        .withDayOfMonth(1)
                        .withHour(0).withMinute(0).withSecond(0);
            case 3: // 按年
                return dateTime.withDayOfYear(1)
                        .withHour(0).withMinute(0).withSecond(0);
            default:
                throw exception(INVALID_TIME_TYPE);
        }
    }


}