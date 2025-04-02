package cn.bitlinks.ems.module.power.service.unitpriceconfiguration;

import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.UnitPriceHistoryRespVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.UnitPriceHistorySaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpricehistory.UnitPriceHistoryDO;
import cn.bitlinks.ems.module.power.dal.mysql.unitpricehistory.UnitPriceHistoryMapper;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.unitpricehistory.UnitPriceHistoryService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.json.JsonUtils.objectMapper;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static javax.xml.bind.DatatypeConverter.parseDecimal;

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
            // 插入
            UnitPriceConfigurationDO unitPriceConfiguration = BeanUtils.toBean(createReqVO, UnitPriceConfigurationDO.class);
            unitPriceConfigurationMapper.insertOrUpdate(unitPriceConfiguration);

            // 添加ID到列表
            ids.add(unitPriceConfiguration.getId());
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

                boolean isModified =
                        // 时间比较（截断到分钟级）
                        !Objects.equals(existingConfig.getStartTime().truncatedTo(ChronoUnit.MINUTES),
                                updateReqVO.getStartTime().truncatedTo(ChronoUnit.MINUTES))
                                || !Objects.equals(existingConfig.getEndTime().truncatedTo(ChronoUnit.MINUTES),
                                updateReqVO.getEndTime().truncatedTo(ChronoUnit.MINUTES))
                                // JSON内容比较（反序列化后比较）
                                || !comparePriceDetails(existingConfig.getPriceDetails(), updateReqVO.getPriceDetails())
                                // 其他业务字段
                                || !Objects.equals(existingConfig.getBillingMethod(), updateReqVO.getBillingMethod())
                                || !Objects.equals(existingConfig.getAccountingFrequency(), updateReqVO.getAccountingFrequency())
                                // 隐藏字段（如公式版本）
                                || !Objects.equals(existingConfig.getFormula(), updateReqVO.getFormula());

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
                    // 检查下一周期冲突（未来周期允许修改开始时间）
                    checkNextPeriodConflict(energyId, existingConfig.getEndTime(), updateReqVO.getEndTime());
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
                ids.add(unitPriceConfig.getId());
            }
        }
        return ids;
    }

    // 辅助方法：深度比较价格详情
    private boolean comparePriceDetails(String existingJson, String newJson) {
        try {
            List<PriceDetail> existingDetails = JSON.parseArray(existingJson, PriceDetail.class);
            List<PriceDetail> newDetails = JSON.parseArray(newJson, PriceDetail.class);
            return Objects.equals(existingDetails, newDetails);
        } catch (Exception e) {
            return false;
        }
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
    public PageResult<UnitPriceConfigurationDO> getUnitPriceConfigurationPage(UnitPriceConfigurationPageReqVO pageReqVO) {
        return unitPriceConfigurationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<UnitPriceConfigurationDO> getUnitPriceConfigurationByEnergyId(Long energyId) {
        return unitPriceConfigurationMapper.selectList(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .ge(UnitPriceConfigurationDO::getEndTime, LocalDateTime.now()) // 新增过滤条件[7](@ref)
                        .orderByAsc(UnitPriceConfigurationDO::getStartTime)
        );
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
        // 1. 查询有效的价格配置
        List<UnitPriceConfigurationDO> configs = unitPriceConfigurationMapper.selectList(
                Wrappers.<UnitPriceConfigurationDO>lambdaQuery()
                        .eq(UnitPriceConfigurationDO::getEnergyId, energyId)
                        .le(UnitPriceConfigurationDO::getStartTime, targetTime)
                        .ge(UnitPriceConfigurationDO::getEndTime, targetTime)
        );

        if (configs.isEmpty()) {
            throw exception(UNIT_PRICE_CONFIGURATION_NOT_EXISTS);
        }

        UnitPriceConfigurationDO config = configs.get(0);
        PriceResultDTO result = new PriceResultDTO();
        result.setPriceType(config.getBillingMethod());

        // 解析价格明细JSON
        List<PriceDetail> priceDetails = parsePriceDetails(config.getPriceDetails());

        switch (config.getBillingMethod()) {
            case 1: // 固定单价
                handleFixedPrice(result, priceDetails);
                break;
            case 2: // 分时电价
                handleTimeBasedPrice(result, priceDetails, targetTime);
                break;
            case 3: // 阶梯电价
                handleLadderPrice(result, priceDetails, config, targetTime);
                break;
            default:
                throw exception(INVALID_PRICE_TYPE);
        }

        return result;
    }

    private void handleFixedPrice(PriceResultDTO result, List<PriceDetail> priceDetails) {
        if (!priceDetails.isEmpty()) {
            result.setFixedPrice(new BigDecimal(priceDetails.get(0).getPrice()));
        }
    }

    private void handleTimeBasedPrice(PriceResultDTO result, List<PriceDetail> priceDetails, LocalDateTime targetTime) {
        Map<String, BigDecimal> timePriceMap = new LinkedHashMap<>();
        LocalTime queryTime = targetTime.toLocalTime();

        priceDetails.forEach(detail -> {
            if (detail.getTime().size() == 2) {
                LocalTime start = LocalTime.parse(detail.getTime().get(0));
                LocalTime end = LocalTime.parse(detail.getTime().get(1));

                // 处理跨天时间段（如23:00-01:00）
                boolean isCrossDay = end.isBefore(start);
                boolean match = isCrossDay ?
                        queryTime.isAfter(start) || queryTime.isBefore(end) :
                        queryTime.isAfter(start) && queryTime.isBefore(end);

                if (match) {
                    timePriceMap.put(
                            detail.getTime().get(0) + "-" + detail.getTime().get(1),
                            new BigDecimal(detail.getPrice())
                    );
                }
            }
        });

        result.setTimePrices(timePriceMap);
    }

    private void handleLadderPrice(PriceResultDTO result, List<PriceDetail> priceDetails,
                                   UnitPriceConfigurationDO config, LocalDateTime targetTime) {
        // 获取核算周期起始时间
        LocalDateTime periodStart = calculatePeriodStart(config.getAccountingFrequency(), targetTime);
        result.setPeriodStart(periodStart);

        // 转换阶梯价格配置
        List<PriceResultDTO.LadderPrice> ladderPrices = priceDetails.stream()
                .map(detail -> {
                    PriceResultDTO.LadderPrice lp = new PriceResultDTO.LadderPrice();
                    lp.setMin(parseDecimal(detail.getMin()));
                    lp.setMax(parseDecimal(detail.getMax()));
                    lp.setPrice(new BigDecimal(detail.getPrice()));
                    return lp;
                })
                .collect(Collectors.toList());

        result.setLadderPrices(ladderPrices);
    }

    private LocalDateTime calculatePeriodStart(Integer accountingFrequency, LocalDateTime dateTime) {
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

    // JSON解析辅助方法
    private List<PriceDetail> parsePriceDetails(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<PriceDetail>>() {});
        } catch (JsonProcessingException e) {
            throw exception(FAILED_PRICE_DETAILS,e);
        }
    }

}