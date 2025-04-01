package cn.bitlinks.ems.module.power.service.unitpriceconfiguration;

import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.UnitPriceHistoryRespVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.UnitPriceHistorySaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpricehistory.UnitPriceHistoryDO;
import cn.bitlinks.ems.module.power.dal.mysql.unitpricehistory.UnitPriceHistoryMapper;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.unitpricehistory.UnitPriceHistoryService;
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

            // 插入历史记录
            UnitPriceHistorySaveReqVO unitPriceHistory = BeanUtils.toBean(createReqVO, UnitPriceHistorySaveReqVO.class);
            unitPriceHistoryService.createUnitPriceHistory(unitPriceHistory);

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
        for (UnitPriceConfigurationSaveReqVO updateReqVO : updateReqVOList) {
            // 设置能源ID
            updateReqVO.setEnergyId(energyId);

            // 处理时间范围
            if (updateReqVO.getTimeRange() != null && updateReqVO.getTimeRange().size() == 2) {
                updateReqVO.setStartTime(updateReqVO.getTimeRange().get(0));
                updateReqVO.setEndTime(updateReqVO.getTimeRange().get(1));
            }

            // 修改记录时需要排除自身ID
            if (isTimeConflict(energyId, updateReqVO.getStartTime(), updateReqVO.getEndTime(), updateReqVO.getId())) {
                throw exception(TIME_CONFLICT);
            }
            // 插入
            EnergyConfigurationDO energyConfiguration=energyConfigurationService.getEnergyConfiguration(energyId);
            updateReqVO.setFormula(energyConfiguration.getUnitPriceFormula());
            UnitPriceConfigurationDO unitPriceConfiguration = BeanUtils.toBean(updateReqVO, UnitPriceConfigurationDO.class);
            // 执行更新并获取影响行数
            if(updateReqVO.getId() != null) {
                int affectedRows = unitPriceConfigurationMapper.updateById(unitPriceConfiguration);
                if (affectedRows > 0) {
                    // 插入历史记录
                    updateReqVO.setId(null);
                    UnitPriceHistorySaveReqVO unitPriceHistory = BeanUtils.toBean(updateReqVO, UnitPriceHistorySaveReqVO.class);
                    unitPriceHistoryService.createUnitPriceHistory(unitPriceHistory);
                    ids.add(unitPriceConfiguration.getId());
                }
            }else {
                UnitPriceHistorySaveReqVO unitPriceHistory = BeanUtils.toBean(updateReqVO, UnitPriceHistorySaveReqVO.class);
                unitPriceConfigurationMapper.insertOrUpdate(unitPriceConfiguration);
                unitPriceHistoryService.createUnitPriceHistory(unitPriceHistory);
                ids.add(unitPriceConfiguration.getId());
            }
        }
        return ids;
    }

    @Override
    public void deleteUnitPriceConfiguration(Long id) {
        // 校验存在
        validateUnitPriceConfigurationExists(id);
        // 删除
        unitPriceConfigurationMapper.deleteById(id);
    }

    private void validateUnitPriceConfigurationExists(Long id) {
        if (unitPriceConfigurationMapper.selectById(id) == null) {
            throw exception(UNIT_PRICE_CONFIGURATION_NOT_EXISTS);
        }
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

        // TODO: 调用用量服务获取当前周期用量
        // BigDecimal usage = getUsage(config.getEnergyId(), periodStart, targetTime);
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