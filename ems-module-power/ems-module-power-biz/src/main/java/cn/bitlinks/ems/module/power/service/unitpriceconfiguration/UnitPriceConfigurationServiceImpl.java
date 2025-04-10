package cn.bitlinks.ems.module.power.service.unitpriceconfiguration;

import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailRespVO;
import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import cn.bitlinks.ems.module.power.dal.mysql.pricedetail.PriceDetailMapper;
import cn.bitlinks.ems.module.power.dal.mysql.unitpricehistory.UnitPriceHistoryMapper;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.pricedetail.PriceDetailService;
import cn.bitlinks.ems.module.power.service.unitpricehistory.UnitPriceHistoryService;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;

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
    @Resource
    private UnitPriceHistoryService unitPriceHistoryService;
    @Resource
    private UnitPriceHistoryMapper unitPriceHistoryMapper;
    @Resource
    private EnergyConfigurationService energyConfigurationService;
    @Resource
    private PriceDetailService priceDetailService;
    @Autowired
    private PriceDetailMapper priceDetailMapper;

    @Override
    public List<Long> createUnitPriceConfigurations(Long energyId, List<UnitPriceConfigurationSaveReqVO> createReqVOList) {
        List<Long> ids = new ArrayList<>();
        for (UnitPriceConfigurationSaveReqVO createReqVO : createReqVOList) {
            // 设置能源ID
            createReqVO.setEnergyId(energyId);

            // 处理时间范围
            if (createReqVO.getTimeRange() != null && createReqVO.getTimeRange().size() == 2) {
                createReqVO.setStartTime(createReqVO.getTimeRange().get(0));
                createReqVO.setEndTime(createReqVO.getTimeRange().get(1));
            }

            if (createReqVO.getStartTime() != null && createReqVO.getEndTime() != null
                    && !createReqVO.getEndTime().isAfter(createReqVO.getStartTime())) {
                throw exception(END_TIME_MUST_AFTER_START_TIME);
            }


            // 检查时间冲突
            if (isTimeConflict(energyId, createReqVO.getStartTime(), createReqVO.getEndTime())) {
                // 时间冲突，抛出异常或处理逻辑
                throw exception(TIME_CONFLICT);
            }
            EnergyConfigurationDO energyConfiguration=energyConfigurationService.getEnergyConfiguration(energyId);
            createReqVO.setFormula(energyConfiguration.getUnitPriceFormula());
            // 插入主表
            UnitPriceConfigurationDO unitPriceConfig = BeanUtils.toBean(createReqVO, UnitPriceConfigurationDO.class);
            unitPriceConfigurationMapper.insertOrUpdate(unitPriceConfig);
            ids.add(unitPriceConfig.getId());

            // 插入子表价格详情
            if (CollectionUtil.isNotEmpty(createReqVO.getPriceDetails())) {
                createReqVO.getPriceDetails().forEach(detail -> {
                    detail.setPriceId(unitPriceConfig.getId()); // 关联主表ID
                    priceDetailService.createPriceDetail(detail);
                });
            }
        }
        return ids;
    }

    private boolean isTimeConflict(Long energyId, LocalDateTime startTime, LocalDateTime endTime) {
        List<UnitPriceConfigurationDO> existingConfigs = unitPriceConfigurationMapper.findByEnergyId(energyId);
        for (UnitPriceConfigurationDO config : existingConfigs) {
            if ((startTime.isBefore(config.getEndTime()) && endTime.isAfter(config.getStartTime())) ||
                    startTime.equals(config.getStartTime()) || endTime.equals(config.getEndTime())) {
                // 时间范围重叠
                return true;
            }
        }
        // 没有发现时间冲突
        return false;
    }

    // 修改后的时间冲突检查方法（增加 excludeId 参数）
    private boolean isTimeConflict(Long energyId, LocalDateTime startTime, LocalDateTime endTime, Long excludeId) {
        // 查询时排除指定ID
        List<UnitPriceConfigurationDO> existingConfigs = unitPriceConfigurationMapper.findByEnergyIdExcludeId(energyId, excludeId);

        for (UnitPriceConfigurationDO config : existingConfigs) {
            if (isOverlap(startTime, endTime, config.getStartTime(), config.getEndTime())) {
                return true;
            }
        }
        return false;
    }

    // 时间重叠判断工具方法
    private boolean isOverlap(LocalDateTime start1, LocalDateTime end1,
                              LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    @Override
    public List<Long> updateUnitPriceConfiguration(Long energyId, List<UnitPriceConfigurationSaveReqVO> updateReqVOList) {
        List<Long> ids = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (UnitPriceConfigurationSaveReqVO updateReqVO : updateReqVOList) {
            // 设置能源ID
            updateReqVO.setEnergyId(energyId);

            // 处理时间范围
            if (updateReqVO.getTimeRange() != null && updateReqVO.getTimeRange().size() == 2) {
                updateReqVO.setStartTime(updateReqVO.getTimeRange().get(0));
                updateReqVO.setEndTime(updateReqVO.getTimeRange().get(1));
            }

            if (updateReqVO.getStartTime() != null && updateReqVO.getEndTime() != null
                    && !updateReqVO.getEndTime().isAfter(updateReqVO.getStartTime())) {
                throw exception(END_TIME_MUST_AFTER_START_TIME);
            }

            if (updateReqVO.getId() != null) {
                UnitPriceConfigurationDO existingConfig = unitPriceConfigurationMapper.selectById(updateReqVO.getId());
                if (existingConfig == null) {
                    throw exception(UNIT_PRICE_CONFIGURATION_NOT_EXISTS);
                }
                // 检查价格详情变更
                boolean isPriceDetailsModified = isPriceDetailsModified(updateReqVO.getId(), updateReqVO.getPriceDetails());

                boolean isModified =
                        // 时间比较（截断到分钟级）
                        !Objects.equals(existingConfig.getStartTime().truncatedTo(ChronoUnit.MINUTES),
                                updateReqVO.getStartTime().truncatedTo(ChronoUnit.MINUTES))
                                || !Objects.equals(existingConfig.getEndTime().truncatedTo(ChronoUnit.MINUTES),
                                updateReqVO.getEndTime().truncatedTo(ChronoUnit.MINUTES))
                                || !Objects.equals(existingConfig.getBillingMethod(), updateReqVO.getBillingMethod())
                                || !Objects.equals(existingConfig.getAccountingFrequency(), updateReqVO.getAccountingFrequency())
                                || !Objects.equals(existingConfig.getFormula(), updateReqVO.getFormula())
                                || isPriceDetailsModified;

                if (!isModified) {
                    ids.add(existingConfig.getId());
                    continue; // 无变更直接跳过后续校验
                }

                // 判断周期状态
                boolean isPast = existingConfig.getEndTime().isBefore(now);
                boolean isCurrent = !existingConfig.getStartTime().isAfter(now)
                        && !existingConfig.getEndTime().isBefore(now);
                boolean isFuture = existingConfig.getStartTime().isAfter(now);

                if (isPast) {
                    throw exception(PAST_PERIOD_MODIFY_NOT_ALLOWED);
                } else if (isCurrent) {
                    // 新增下一周期存在性检查
                    UnitPriceConfigurationDO nextPeriod = unitPriceConfigurationMapper.findNextPeriod(
                            energyId,
                            existingConfig.getEndTime()
                    );
                    if (nextPeriod != null) {
                        throw exception(NEXT_PERIOD_CONFLICT); // 关联异常提示
                    }
                    // 校验当前周期：开始时间不可修改，结束时间需合法
                    if (!updateReqVO.getStartTime().isEqual(existingConfig.getStartTime())) {
                        throw exception(CANNOT_MODIFY_START_TIME_OF_CURRENT_PERIOD);
                    }
                    if (updateReqVO.getEndTime().isBefore(now)
                            || !updateReqVO.getEndTime().isAfter(existingConfig.getStartTime())) {
                        throw exception(INVALID_END_TIME_FOR_CURRENT_PERIOD);
                    }
                } else if (isFuture) {
                    checkNextPeriodConflict(energyId, existingConfig.getEndTime(), updateReqVO.getEndTime());
                }

                // 更新子表价格详情
                if (isPriceDetailsModified) {
                    updatePriceDetails(existingConfig.getId(), updateReqVO.getPriceDetails());
                }
            }

            // 原有时间冲突校验
            if (isTimeConflict(energyId, updateReqVO.getStartTime(), updateReqVO.getEndTime(), updateReqVO.getId())) {
                throw exception(TIME_CONFLICT);
            }


            // 后续保存逻辑...
            EnergyConfigurationDO energyConfig = energyConfigurationService.getEnergyConfiguration(energyId);
            updateReqVO.setFormula(energyConfig.getUnitPriceFormula());
            UnitPriceConfigurationDO unitPriceConfig = BeanUtils.toBean(updateReqVO, UnitPriceConfigurationDO.class);

            if (updateReqVO.getId() != null) {
                int affectedRows = unitPriceConfigurationMapper.updateById(unitPriceConfig);
                if (affectedRows > 0) {
                    // 插入历史记录
                    updateReqVO.setId(null);
                    ids.add(unitPriceConfig.getId());
                }
            } else {
                unitPriceConfigurationMapper.insertOrUpdate(unitPriceConfig);
                Long newConfigId = unitPriceConfig.getId();
                ids.add(unitPriceConfig.getId());

                // 新增价格详情（子表）
                if (updateReqVO.getPriceDetails() != null && !updateReqVO.getPriceDetails().isEmpty()) {
                    List<PriceDetailDO> priceDetails = updateReqVO.getPriceDetails().stream()
                            .map(d -> {
                                PriceDetailDO detail = BeanUtils.toBean(d, PriceDetailDO.class);
                                detail.setPriceId(newConfigId); // 关联主表 ID
                                return detail;
                            })
                            .collect(Collectors.toList());
                    priceDetailMapper.insertBatch(priceDetails); // 批量插入子表
                }
            }
        }
        return ids;
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
                detail.setUsageMin(usageMin.setScale(5, RoundingMode.HALF_UP));
            } else {
                detail.setUsageMin(null);
            }

            if (usageMax != null) {
                detail.setUsageMax(usageMax.setScale(5, RoundingMode.HALF_UP));
            } else {
                detail.setUsageMax(null);
            }
            BigDecimal price = d.getUnitPrice();
            detail.setUnitPrice(price.setScale(5,RoundingMode.HALF_UP));
            return detail;
        }).collect(Collectors.toList());

        return !CollectionUtils.isEqualCollection(existingDetailsForCompare, newDetailsDO);
    }

    // 更新子表数据
    private void updatePriceDetails(Long priceId, List<PriceDetailSaveReqVO> details) {
        // 清空原有数据
        priceDetailService.deleteByPriceId(priceId);

        // 插入新数据
        details.forEach(detail -> {
            detail.setPriceId(priceId);
            priceDetailService.createPriceDetail(detail);
        });
    }

    // 检查下一周期冲突
    private void checkNextPeriodConflict(Long energyId, LocalDateTime originalEndTime, LocalDateTime newEndTime) {
        UnitPriceConfigurationDO nextPeriod = unitPriceConfigurationMapper.findNextPeriod(energyId, originalEndTime);
        if (nextPeriod != null && (newEndTime.isAfter(nextPeriod.getStartTime())
                || newEndTime.isEqual(nextPeriod.getStartTime()))) {
            throw exception(NEXT_PERIOD_CONFLICT);
        }
    }

    @Override
    public void deleteUnitPriceConfiguration(Long id) {
        UnitPriceConfigurationDO existingConfig = validateUnitPriceConfigurationExists(id);
        LocalDateTime now = LocalDateTime.now();

        if (existingConfig.getEndTime().isBefore(now)) {
            throw exception(CANNOT_DELETE_PAST_PERIOD);
        } else if (!existingConfig.getStartTime().isAfter(now)
                && !existingConfig.getEndTime().isBefore(now)) {
            throw exception(CANNOT_DELETE_CURRENT_PERIOD);
        }
        priceDetailService.deleteByPriceId(id);
        unitPriceConfigurationMapper.deleteById(id);
    }


    private UnitPriceConfigurationDO validateUnitPriceConfigurationExists(Long id) {
        UnitPriceConfigurationDO config = unitPriceConfigurationMapper.selectById(id);
        if (config == null) {
            throw exception(UNIT_PRICE_CONFIGURATION_NOT_EXISTS);
        }
        return config;
    }

    @Override
    public UnitPriceConfigurationDO getUnitPriceConfiguration(Long id) {
        return unitPriceConfigurationMapper.selectById(id);
    }

    @Override
    public List<UnitPriceConfigurationDO> getUnitPriceConfigurationVOByEnergyId(Long energyId) {
        // 获取主表数据（已关联子表）
        List<UnitPriceConfigurationDO> configs = this.getUnitPriceConfigurationByEnergyId(energyId);

        return configs.stream().map(config -> {
            // 转换主表字段
            UnitPriceConfigurationDO vo = BeanUtils.toBean(config, UnitPriceConfigurationDO.class);

            // 手动转换子表数据（DO -> VO）
            List<PriceDetailDO> detailVOs = config.getPriceDetails().stream()
                    .map(detail -> BeanUtils.toBean(detail, PriceDetailDO.class))
                    .collect(Collectors.toList());

            vo.setPriceDetails(detailVOs);
            return vo;
        }).collect(Collectors.toList());
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
    public List<UnitPriceConfigurationDO> getUnitPriceConfigurationByEnergyId(Long energyId) {
        // 1. 查询主表列表（不带子表数据）
        List<UnitPriceConfigurationDO> configs = unitPriceConfigurationMapper.selectList(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .ge(UnitPriceConfigurationDO::getEndTime, LocalDateTime.now())
                        .orderByAsc(UnitPriceConfigurationDO::getStartTime)
        );

        // 2. 批量加载子表数据并关联到主表
        if (!configs.isEmpty()) {
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
        }

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

        return configs.isEmpty() ? LocalDateTime.now() : configs.get(0).getEndTime();
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
            BigDecimal prevMax = ladderPrices.get(i-1).getMax();
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