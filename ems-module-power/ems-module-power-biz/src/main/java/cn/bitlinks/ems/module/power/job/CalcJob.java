package cn.bitlinks.ems.module.power.job;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.bitlinks.ems.module.power.api.usagecost.dto.UsageCostCalcReqDTO;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.CoalFactorFormulaData;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookEnergyTypeVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.EnergyTimeResultVO;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import cn.bitlinks.ems.module.power.dal.dataobject.usagecost.UsageCostDO;
import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;
import cn.bitlinks.ems.module.power.enums.BillingMethod;
import cn.bitlinks.ems.module.power.service.coalfactorhistory.CoalFactorHistoryService;
import cn.bitlinks.ems.module.power.service.pricedetail.PriceDetailService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.bitlinks.ems.module.power.utils.CalculateUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangl
 * @date 2025年05月22日 15:40
 */
@Slf4j
@Component
public class CalcJob {

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private UnitPriceConfigurationMapper unitPriceConfigurationMapper;

    @Resource
    private PriceDetailService priceDetailService;

    @Resource
    private CoalFactorHistoryService coalFactorHistoryService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Value("${ems.calc.scale}")
    private Integer scale;

    private static final String COAL_FACTOR = "折标煤系数";
    private static final String PRICE = "单价";
    private static final String USAGE = "用量";

    private static final String CALC_USAGE_COAL_FACTOR_JOB_LOCK = "CALC_USAGE_COAL_FACTOR";

    private static final String CALC_USAGE_COAL_FACTOR_JOB_TIME_CACHE_KEY = "CALC_USAGE_COAL_FACTOR_TIME";


    /**
     * 计算同一时间的时间
     * 每分钟的第30秒执行一次
     *
     * @return
     */
    @Scheduled(cron = "30 * * * * ?")
    public void process() {

        RLock lock = redissonClient.getLock(CALC_USAGE_COAL_FACTOR_JOB_LOCK);
        // 加锁
        if (!lock.tryLock()) {
            log.info("计算计量器具折标煤和用能成本任务获取锁失败");
            return;
        }
        log.info("【计算计量器具折标煤和用能成本任务开始】");
        try {
            LocalDateTime queryTime = LocalDateTime.now();
            String cacheTimeStr = redisTemplate.opsForValue().get(CALC_USAGE_COAL_FACTOR_JOB_TIME_CACHE_KEY);
            if (StringUtils.isNoneBlank(cacheTimeStr)) {
                LocalDateTime cacheTime = LocalDateTimeUtil.parse(cacheTimeStr, DatePattern.NORM_DATETIME_MINUTE_PATTERN);
                queryTime = cacheTime.plusMinutes(1L);

                LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
                LocalDateTime jobTime = queryTime.truncatedTo(ChronoUnit.MINUTES);
                //计算数据最晚时间为前1分钟的数据
                long between = ChronoUnit.MINUTES.between(jobTime, now);
                if (between < 1) {
                    log.info("计算计量器具折标煤和用能成本任务时间未到");
                    return;
                }

            }

            //根据时间查询数据，每次处理1分钟的数据
            List<UsageCostCalcReqDTO> reqDTOS = new ArrayList<>();
            if (CollectionUtil.isEmpty(reqDTOS)) {
                log.info("计算计量器具折标煤和用能成本暂无数据");
                return;
            }


            //台账ID
            Set<Long> standingbookIds = reqDTOS.stream().map(UsageCostCalcReqDTO::getStandingbookId).collect(Collectors.toSet());
            //台账能源类型关系
            List<StandingbookEnergyTypeVO> byStandingbookIds = standingbookService.getByStandingbookIds(new ArrayList<>(standingbookIds));
            // 台账能源类型关系Map
            Map<Long, StandingbookEnergyTypeVO> standingbookEnergyTypeVOMap = byStandingbookIds.stream().collect(Collectors.toMap(StandingbookEnergyTypeVO::getStandingbookId, Function.identity()));
            //能源ID
            Set<Long> energyIds = new HashSet<>();
            reqDTOS.forEach(usageCostCalcReqDTO -> {
                StandingbookEnergyTypeVO standingbookEnergyTypeVO = standingbookEnergyTypeVOMap.get(usageCostCalcReqDTO.getStandingbookId());
                energyIds.add(standingbookEnergyTypeVO.getEnergyId());
            });
            //能源单价公式
            List<EnergyTimeResultVO> byEnergyTime = unitPriceConfigurationMapper.getByEnergyTime(new ArrayList<>(energyIds), queryTime);
            //单价ID
            List<Long> priceIds = byEnergyTime.stream().map(EnergyTimeResultVO::getId).collect(Collectors.toList());
            //公式IDmap
            Map<Long, List<PriceDetailDO>> detailsByPriceIds = priceDetailService.getDetailsByPriceIds(priceIds);

            //能源单价公式
            Map<Long, EnergyTimeResultVO> energyTimeFormula = byEnergyTime.stream()
                    .collect(Collectors.toMap(EnergyTimeResultVO::getEnergyId, Function.identity()));

            //折标煤公式
            List<CoalFactorFormulaData> coalFactorFormulaList = coalFactorHistoryService.getByEnergyIdsAndTime(new ArrayList<>(energyIds), queryTime);
            //能源折标煤系数公式
            Map<Long, CoalFactorFormulaData> coalFactorFormulaDataMap = coalFactorFormulaList.stream()
                    .collect(Collectors.toMap(CoalFactorFormulaData::getEnergyId, Function.identity()));


            List<UsageCostDO> saveList = new ArrayList<>();

            LocalDateTime finalQueryTime = queryTime;
            reqDTOS.forEach(dto -> {
                //台账能源类型
                StandingbookEnergyTypeVO standingbookEnergyTypeVO = standingbookEnergyTypeVOMap.get(dto.getStandingbookId());
                //单价公式
                EnergyTimeResultVO energyTimeResultVO = energyTimeFormula.get(standingbookEnergyTypeVO.getEnergyId());
                //能源单价详细
                List<PriceDetailDO> priceDetailList = detailsByPriceIds.get(energyTimeResultVO.getId());
                BigDecimal currentUsage = dto.getCurrentUsage();
                //成本
                BigDecimal cost = calcCost(priceDetailList, currentUsage, energyTimeResultVO, finalQueryTime);

                //折标煤公式
                CoalFactorFormulaData coalFactorFormulaData = coalFactorFormulaDataMap.get(standingbookEnergyTypeVO.getEnergyId());
                BigDecimal coalNum = calcCoalFactorFormula(coalFactorFormulaData, currentUsage);
                //组装成本
                UsageCostDO usageCostDO = new UsageCostDO();
                usageCostDO.setStandingbookId(dto.getStandingbookId());
                usageCostDO.setEnergyId(energyTimeResultVO.getEnergyId());
                usageCostDO.setCurrentUsage(dto.getCurrentUsage());
                usageCostDO.setAggregateTime(dto.getAggregateTime());
                usageCostDO.setCost(cost);
                usageCostDO.setStandardCoalEquivalent(coalNum);

                saveList.add(usageCostDO);

            });
            usageCostService.saveList(saveList);
            String time = LocalDateTimeUtil.format(finalQueryTime, DatePattern.NORM_DATETIME_MINUTE_PATTERN);
            redisTemplate.opsForValue().set(CALC_USAGE_COAL_FACTOR_JOB_TIME_CACHE_KEY, time);
            log.info("【计算计量器具折标煤和用能成本任务结束】");

        } catch (Exception ex) {
            log.error("[计算计量器具折标煤和用能成本任务][执行异常]", ex);
        } finally {
            lock.unlock();
        }


    }

    private BigDecimal calcCoalFactorFormula(CoalFactorFormulaData coalFactorFormulaData, BigDecimal currentUsage) {
        log.info("【计算计量器具折标煤】");
        Map<String, Object> calcData = new HashMap<>();
        calcData.put(USAGE, currentUsage);
        calcData.put(COAL_FACTOR, coalFactorFormulaData.getFactor());
        Integer formulaScale = coalFactorFormulaData.getFormulaScale();
        //结果
        BigDecimal execute = (BigDecimal) CalculateUtil.execute(coalFactorFormulaData.getEnergyFormula(), calcData);
        if (Objects.isNull(formulaScale) || formulaScale == 0) {
            return execute.setScale(scale, RoundingMode.HALF_UP);
        }
        return execute.setScale(formulaScale, RoundingMode.HALF_UP);
    }

    /**
     * 计算成本
     */
    private BigDecimal calcCost(List<PriceDetailDO> priceDetailList, BigDecimal currentUsage, EnergyTimeResultVO energyTimeResultVO, LocalDateTime queryTime) {
        log.info("计算计量器具用能成本，计价方式：{}，当前用量，{}", energyTimeResultVO.getBillingMethod(), currentUsage.toString());
        //统一计价
        if (energyTimeResultVO.getBillingMethod().equals(BillingMethod.UNIFIED_PRICE.getCode())) {

            PriceDetailDO priceDetailDOS = priceDetailList.get(0);
            BigDecimal unitPrice = priceDetailDOS.getUnitPrice();
            Map<String, Object> calcData = new HashMap<>();
            calcData.put(USAGE, currentUsage);
            calcData.put(PRICE, unitPrice);
            //结果
            BigDecimal execute = (BigDecimal) CalculateUtil.execute(energyTimeResultVO.getEnergyFormula(), calcData);
            return execute;
        } else if (energyTimeResultVO.getBillingMethod().equals(BillingMethod.TIME_SPAN_PRICE.getCode())) { //分时段计价
            PriceDetailDO priceDetailDO = priceDetailList.stream()
                    .filter(pd -> pd.getPeriodStart().isBefore(queryTime.toLocalTime())
                            && pd.getPeriodEnd().isAfter(queryTime.toLocalTime()))
                    .findAny().orElse(null);
            if (Objects.isNull(priceDetailDO)) {
                return BigDecimal.ZERO;
            } else {
                BigDecimal unitPrice = priceDetailDO.getUnitPrice();
                Map<String, Object> calcData = new HashMap<>();
                calcData.put(USAGE, currentUsage);
                calcData.put(PRICE, unitPrice);
                //结果
                BigDecimal execute = (BigDecimal) CalculateUtil.execute(energyTimeResultVO.getEnergyFormula(), calcData);
                return execute;
            }
        } else if (energyTimeResultVO.getBillingMethod().equals(BillingMethod.STAIR_PRICE.getCode())) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }


}
