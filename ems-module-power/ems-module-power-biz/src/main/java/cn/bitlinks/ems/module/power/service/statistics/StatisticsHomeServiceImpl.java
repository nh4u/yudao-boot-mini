package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.EnergyClassifyEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants;
import cn.bitlinks.ems.module.power.enums.StatisticsQueryType;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingBookStageEnum;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.production.ProductionService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.bitlinks.ems.module.power.utils.CommonUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * 统计总览 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
@Slf4j
public class StatisticsHomeServiceImpl implements StatisticsHomeService {

    @Resource
    private StatisticsCommonService statisticsCommonService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private ProductionService productionService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;


    public static final String ITEM_ACCUMULATE = "累计";
    public static final String ITEM_MAX = "最高(Max)";
    public static final String ITEM_MIN = "最低(Min)";
    public static final String ITEM_AVG = "平均(Avg)";
    public static final String OVERVIEW_ENERGY_STR = "综合能耗";

    private List<StatisticsOverviewEnergyData> emptyEnergyList() {
        StatisticsOverviewEnergyData data = new StatisticsOverviewEnergyData();
        data.setName(OVERVIEW_ENERGY_STR);
        return Collections.singletonList(data);
    }

    private List<StatisticsOverviewEnergyData> energyList(LocalDateTime startTime, LocalDateTime endTime, List<Long> stageSbIds, List<EnergyConfigurationDO> energyList) {
        try {
            if (CollUtil.isEmpty(stageSbIds)) {
                return emptyEnergyList();
            }
            List<StatisticsOverviewEnergyData> list = new ArrayList<>();
            List<UsageCostData> energyStandardCoalList = usageCostService.getEnergyStandardCoalCostBySbIds(startTime, endTime, stageSbIds);

            Map<Long, UsageCostData> energyStandardCoalMap = Collections.emptyMap();
            if (CollUtil.isNotEmpty(energyStandardCoalList)) {
                energyStandardCoalMap = energyStandardCoalList
                        .stream()
                        .collect(Collectors.toMap(UsageCostData::getEnergyId, Function.identity()));
            }
            BigDecimal totalStandardCoalSum = null;
            BigDecimal totalStandardCostSum = null;
            for (EnergyConfigurationDO e : energyList) {
                StatisticsOverviewEnergyData data = new StatisticsOverviewEnergyData();

                data.setName(e.getEnergyName());
                data.setEnergyIcon(e.getEnergyIcon());
                data.setUnit(e.getUnit());
                UsageCostData usageCostData = energyStandardCoalMap.get(e.getId());

                if (Objects.nonNull(usageCostData)) {
                    data.setConsumption(dealBigDecimalScale(usageCostData.getCurrentTotalUsage(), DEFAULT_SCALE));
                    data.setStandardCoal(dealBigDecimalScale(usageCostData.getTotalStandardCoalEquivalent(), DEFAULT_SCALE));
                    data.setMoney(dealBigDecimalScale10000(usageCostData.getTotalCost(), DEFAULT_SCALE));
                    BigDecimal coal = usageCostData.getTotalStandardCoalEquivalent();
                    if (coal != null) {
                        totalStandardCoalSum = (totalStandardCoalSum == null)
                                ? coal
                                : totalStandardCoalSum.add(coal);
                    }
                    BigDecimal cost = usageCostData.getTotalCost();
                    if (cost != null) {
                        totalStandardCostSum = (totalStandardCostSum == null)
                                ? cost
                                : totalStandardCostSum.add(cost);
                    }
                }

                list.add(data);
            }

            StatisticsOverviewEnergyData data = new StatisticsOverviewEnergyData();
            data.setName(OVERVIEW_ENERGY_STR);
            data.setStandardCoal(dealBigDecimalScale(totalStandardCoalSum, DEFAULT_SCALE));
            data.setMoney(dealBigDecimalScale10000(totalStandardCostSum, DEFAULT_SCALE));
            list.add(0, data);
            return list;
        } catch (Exception e) {
            log.error("统计总览【能源】发生异常：{}", e.getMessage(), e);
            return emptyEnergyList();
        }

    }


    @Override
    public StatisticsHomeResultVO overview(StatisticsParamHomeVO paramVO) {
        return overviewV2(paramVO);
    }

    public StatisticsHomeResultVO overviewV2(StatisticsParamHomeVO paramVO) {
        StatisticsHomeResultVO statisticsHomeResultVO = new StatisticsHomeResultVO();
        // 尝试读取缓存
        String cacheKey = StatisticsCacheConstants.COMPARISON_HOME_TOTAL + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSONUtil.toBean(cacheRes, StatisticsHomeResultVO.class);
        }

        // 3.能源、折标煤、用能成本展示
        // 能源处理
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassifyUnit(paramVO.getEnergyClassify());
        if (CollUtil.isEmpty(energyList)) {
            return statisticsHomeResultVO;
        }
        List<Long> energyIdList = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        List<Long> stageSbIds = new ArrayList<>();
        if (EnergyClassifyEnum.OUTSOURCED.getCode().equals(paramVO.getEnergyClassify())) {
            // 外购
            stageSbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                    StandingBookStageEnum.PROCUREMENT_STORAGE.getCode(),
                    false,
                    energyIdList);
        } else if (EnergyClassifyEnum.PARK.getCode().equals(paramVO.getEnergyClassify())) {
            // 园区
            stageSbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                    StandingBookStageEnum.PROCESSING_CONVERSION.getCode(),
                    true,
                    energyIdList);
        } else {
            // 不处理
        }


        // 时间参数准备
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];

        // 3.1能源展示
        statisticsHomeResultVO.setStatisticsOverviewEnergyDataList(energyList(startTime, endTime, stageSbIds, energyList));

        // 3.2 折标煤用量统计
        StatisticsParamV2VO param = new StatisticsParamV2VO();
        param.setRange(rangeOrigin);
        param.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());
        param.setDateType(paramVO.getDateType());
        param.setEnergyClassify(paramVO.getEnergyClassify());

        // 查询聚合数据
        // 当前
        StatisticsOverviewStatisticsTableData nowResult = usageCostService.getAggStatisticsByStadingbookIds(
                startTime,
                endTime,
                stageSbIds);
        // 上一周期
        LocalDateTime[] preRange = getPreviousPeriod(startTime, endTime);
        StatisticsOverviewStatisticsTableData prevResult = usageCostService.getAggStatisticsByStadingbookIds(
                preRange[0],
                preRange[1],
                stageSbIds);
        // 同期
        StatisticsOverviewStatisticsTableData lastResult = usageCostService.getAggStatisticsByStadingbookIds(
                startTime.minusYears(1),
                endTime.minusYears(1),
                stageSbIds);

        if (nowResult == null) {
            nowResult = new StatisticsOverviewStatisticsTableData();
        }
        if (lastResult == null) {
            lastResult = new StatisticsOverviewStatisticsTableData();
        }
        if (prevResult == null) {
            prevResult = new StatisticsOverviewStatisticsTableData();
        }
        // 折标煤统计 + 折价统计
        buildStatisticsHomeData(nowResult, prevResult, lastResult, statisticsHomeResultVO);

        // 获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTimeNoParam(
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                stageSbIds);
        statisticsHomeResultVO.setDataUpdateTime(lastTime);

        String jsonStr = JSONUtil.toJsonStr(statisticsHomeResultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        return statisticsHomeResultVO;
    }

    @Deprecated
    public StatisticsHomeResultVO overviewV1(StatisticsParamHomeVO paramVO) {
        StatisticsHomeResultVO statisticsHomeResultVO = new StatisticsHomeResultVO();
        // 尝试读取缓存
        String cacheKey = StatisticsCacheConstants.COMPARISON_HOME_TOTAL + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSONUtil.toBean(cacheRes, StatisticsHomeResultVO.class);
        }

        // 3.能源、折标煤、用能成本展示
        // 能源处理
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassifyUnit(paramVO.getEnergyClassify());
        if (CollUtil.isEmpty(energyList)) {
            return statisticsHomeResultVO;
        }
        List<Long> energyIdList = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 时间参数准备
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];

        // 3.1能源展示
        statisticsHomeResultVO.setStatisticsOverviewEnergyDataList(energyList(startTime, endTime, energyIdList, energyList));

        // 3.2 折标煤用量统计
        StatisticsParamV2VO param = new StatisticsParamV2VO();
        param.setRange(rangeOrigin);
        param.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());
        param.setDateType(paramVO.getDateType());
        param.setEnergyClassify(paramVO.getEnergyClassify());

        // 查询聚合数据
        StatisticsOverviewStatisticsTableData nowResult = usageCostService.getAggStatisticsByEnergyIds(startTime, endTime, energyIdList); // 当前
        // 上一周期
        LocalDateTime[] preRange = getPreviousPeriod(startTime, endTime);
        StatisticsOverviewStatisticsTableData prevResult = usageCostService.getAggStatisticsByEnergyIds(
                preRange[0],
                preRange[1], energyIdList);
        // 同期
        StatisticsOverviewStatisticsTableData lastResult = usageCostService.getAggStatisticsByEnergyIds(startTime.minusYears(1), endTime.minusYears(1), energyIdList);

        if (nowResult == null) {
            nowResult = new StatisticsOverviewStatisticsTableData();
        }
        if (lastResult == null) {
            lastResult = new StatisticsOverviewStatisticsTableData();
        }
        if (prevResult == null) {
            prevResult = new StatisticsOverviewStatisticsTableData();
        }
        // 折标煤统计 + 折价统计
        buildStatisticsHomeData(nowResult, prevResult, lastResult, statisticsHomeResultVO);

        // 获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTime(
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                energyIdList);
        statisticsHomeResultVO.setDataUpdateTime(lastTime);

        String jsonStr = JSONUtil.toJsonStr(statisticsHomeResultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        return statisticsHomeResultVO;
    }

    public static LocalDateTime[] getPreviousPeriod(LocalDateTime start, LocalDateTime end) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 今日
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX).withNano(0);
        if (start.equals(todayStart) && end.equals(todayEnd)) {
            LocalDateTime prevStart = today.minusDays(1).atStartOfDay();
            LocalDateTime prevEnd = today.minusDays(1).atTime(LocalTime.MAX.withNano(0));
            return new LocalDateTime[]{prevStart, prevEnd};
        }

        // 本周
        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);
        LocalDate weekEndDate = today.with(DayOfWeek.SUNDAY);
        LocalDateTime weekStart = weekStartDate.atStartOfDay();
        LocalDateTime weekEnd = weekEndDate.atTime(LocalTime.MAX).withNano(0);
        ;
        if (start.equals(weekStart) && end.equals(weekEnd)) {
            LocalDateTime prevWeekStart = weekStart.minusWeeks(1);
            LocalDateTime prevWeekEnd = weekEnd.minusWeeks(1);
            return new LocalDateTime[]{prevWeekStart, prevWeekEnd};
        }

        // 本月
        LocalDate monthStartDate = today.withDayOfMonth(1);
        LocalDate monthEndDate = today.with(TemporalAdjusters.lastDayOfMonth());
        LocalDateTime monthStart = monthStartDate.atStartOfDay();
        LocalDateTime monthEnd = monthEndDate.atTime(LocalTime.MAX).withNano(0);
        ;
        if (start.equals(monthStart) && end.equals(monthEnd)) {
            LocalDate prevMonthStartDate = monthStartDate.minusMonths(1).withDayOfMonth(1);
            LocalDate prevMonthEndDate = prevMonthStartDate.with(TemporalAdjusters.lastDayOfMonth());
            return new LocalDateTime[]{prevMonthStartDate.atStartOfDay(), prevMonthEndDate.atTime(LocalTime.MAX).withNano(0)};
        }

        // 本季度
        int currentQuarter = (today.getMonthValue() - 1) / 3 + 1;
        LocalDate quarterStartDate = LocalDate.of(today.getYear(), (currentQuarter - 1) * 3 + 1, 1);
        LocalDate quarterEndDate = quarterStartDate.plusMonths(3).minusDays(1);
        LocalDateTime quarterStart = quarterStartDate.atStartOfDay();
        LocalDateTime quarterEnd = quarterEndDate.atTime(LocalTime.MAX).withNano(0);
        ;
        if (start.equals(quarterStart) && end.equals(quarterEnd)) {
            LocalDate prevQuarterStartDate = quarterStartDate.minusMonths(3);
            LocalDate prevQuarterEndDate = prevQuarterStartDate.plusMonths(3).minusDays(1);
            return new LocalDateTime[]{prevQuarterStartDate.atStartOfDay(), prevQuarterEndDate.atTime(LocalTime.MAX).withNano(0)};
        }

        // 本年度
        LocalDate yearStartDate = LocalDate.of(today.getYear(), 1, 1);
        LocalDate yearEndDate = LocalDate.of(today.getYear(), 12, 31);
        LocalDateTime yearStart = yearStartDate.atStartOfDay();
        LocalDateTime yearEnd = yearEndDate.atTime(LocalTime.MAX).withNano(0);
        ;
        if (start.equals(yearStart) && end.equals(yearEnd)) {
            LocalDate prevYearStartDate = yearStartDate.minusYears(1);
            LocalDate prevYearEndDate = yearEndDate.minusYears(1);
            return new LocalDateTime[]{prevYearStartDate.atStartOfDay(), prevYearEndDate.atTime(LocalTime.MAX).withNano(0)};
        }

        // 其他情况：自定义范围，按天数平移
        long days = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
        LocalDateTime prevStart = start.minusDays(days);
        LocalDateTime prevEnd = end.minusDays(days);
        return new LocalDateTime[]{prevStart, prevEnd};
    }

    @Override
    public StatisticsHomeBarVO<BigDecimal> costChart(StatisticsParamHomeVO paramVO) {
        return analysisChart(paramVO, StatisticsHomeChartResultVO::getAccCost, StatisticsHomeChartResultVO::getAvgCost, StatisticsCacheConstants.COMPARISON_HOME_CHART_COST);
    }

    @Override
    public StatisticsHomeBarVO<BigDecimal> coalChart(StatisticsParamHomeVO paramVO) {
        return analysisChart(paramVO, StatisticsHomeChartResultVO::getAccCoal, StatisticsHomeChartResultVO::getAvgCoal, StatisticsCacheConstants.COMPARISON_HOME_CHART_COAL);
    }


    @Override
    public StatisticsHomeTopResultVO overviewTop() {
        // 1.计量器具、重点设备、其他设备
        StatisticsHomeTopResultVO resultVO = new StatisticsHomeTopResultVO();
        resultVO.setMeasurementInstrumentNum(standingbookService.count(CommonConstants.MEASUREMENT_INSTRUMENT_ID));
        resultVO.setKeyEquipmentNum(standingbookService.count(CommonConstants.KEY_EQUIPMENT_ID));
        resultVO.setOtherEquipmentNum(standingbookService.count(CommonConstants.OTHER_EQUIPMENT_ID));
        return resultVO;
    }

    @Override
    public StatisticsHomeTop2ResultVO overviewTop2(StatisticsParamHomeVO paramVO) {
        // 2.产值能耗利用率
        StatisticsHomeTop2ResultVO statisticsHomeResultVO = new StatisticsHomeTop2ResultVO();
        // 单位产值能耗
        dealProductEnergyConsumption(statisticsHomeResultVO, paramVO);

        // 能源利用率（外购）
        statisticsHomeResultVO.setOutsourceEnergyUtilizationRate(getOutsourceEnergyUtilizationRate(paramVO));
        // 能源利用率（园区）
        statisticsHomeResultVO.setParkEnergyUtilizationRate(getParkEnergyUtilizationRate(paramVO));
        // 能源转换率
        statisticsHomeResultVO.setEnergyConversionRate(getEnergyConversionRate(paramVO));

        return statisticsHomeResultVO;

    }

    private StatisticsHomeTop2ResultVO dealProductEnergyConsumption(StatisticsHomeTop2ResultVO statisticsHomeResultVO, StatisticsParamHomeVO paramVO) {

        // 单位产品能耗（8英寸）
        StatisticsHomeTop2Data product8 = new StatisticsHomeTop2Data();
        // 单位产品能耗（12英寸）
        StatisticsHomeTop2Data product12 = new StatisticsHomeTop2Data();
        // 单位产值能耗 综合能耗÷总产值
        StatisticsHomeTop2Data total = new StatisticsHomeTop2Data();

        try {
            ProductionPageReqVO param = BeanUtils.toBean(paramVO, ProductionPageReqVO.class);
            param.setSize(8);
            ProductionDO eight = productionService.getHomeProduction(param);

            param.setSize(12);
            ProductionDO twelve = productionService.getHomeProduction(param);

            // 综合能耗
            // 能源处理 外购
            List<Long> stageSbIds = statisticsCommonService.getStageEnergySbIds(
                    StandingBookStageEnum.PROCUREMENT_STORAGE.getCode(),
                    false,
                    EnergyClassifyEnum.OUTSOURCED);

//            List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(1);
//            if (CollUtil.isEmpty(energyList)) {
//                return statisticsHomeResultVO;
//            }
//            List<Long> energyIdList = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

            // 时间参数准备
            LocalDateTime[] rangeOrigin = paramVO.getRange();
            LocalDateTime startTime = rangeOrigin[0];
            LocalDateTime endTime = rangeOrigin[1];

            // 外购总能耗
            BigDecimal energySumStandardCoal = usageCostService.getSumStandardCoal(startTime, endTime, stageSbIds);
//            BigDecimal energySumStandardCoal = usageCostService.getEnergySumStandardCoal(startTime, endTime, energyIdList);

            BigDecimal eightValue = Optional.ofNullable(eight).map(ProductionDO::getValue).orElse(BigDecimal.ZERO);
            BigDecimal twelveValue = Optional.ofNullable(twelve).map(ProductionDO::getValue).orElse(BigDecimal.ZERO);

            BigDecimal sum = eightValue.add(twelveValue);
            // 单位产品能耗（8英寸）
            if (Objects.nonNull(eight)) {
                BigDecimal eightEnergyStandardCoal = dealProductionConsumption(eightValue, sum, energySumStandardCoal);
                BigDecimal value8 = CommonUtil.divideWithScale(eightValue, eightEnergyStandardCoal, 2);
                product8.setValue(value8);
                product8.setDataUpdateTime(eight.getTime());
                statisticsHomeResultVO.setProductEnergyConsumption8(product8);
            }

            // 单位产品能耗（12英寸）
            if (Objects.nonNull(twelve)) {
                BigDecimal twelveEnergyStandardCoal = dealProductionConsumption(twelveValue, sum, energySumStandardCoal);
                BigDecimal value12 = CommonUtil.divideWithScale(twelveValue, twelveEnergyStandardCoal, 2);
                product12.setValue(value12);
                product12.setDataUpdateTime(twelve.getTime());
                statisticsHomeResultVO.setProductEnergyConsumption12(product12);
            }

            // 单位产值能耗 综合能耗÷总产值 目前总产值无数据 不做计算
//            BigDecimal sumValue = CommonUtil.divideWithScale(sum, energySumStandardCoal, 2);
//            total.setValue(sumValue);
//            total.setDataUpdateTime(product8.getDataUpdateTime().compareTo(product12.getDataUpdateTime()) < 0 ? product8.getDataUpdateTime() : product12.getDataUpdateTime());
//            statisticsHomeResultVO.setOutputValueEnergyConsumption(total);

            return statisticsHomeResultVO;
        } catch (Exception e) {
            log.error("单位产值能耗计算异常", e);
            statisticsHomeResultVO.setProductEnergyConsumption8(product8);
            statisticsHomeResultVO.setProductEnergyConsumption12(product12);
            statisticsHomeResultVO.setOutputValueEnergyConsumption(total);
            return statisticsHomeResultVO;
        }
    }

    private BigDecimal dealProductionConsumption(BigDecimal value, BigDecimal sum, BigDecimal energySumStandardCoal) {
        BigDecimal divide = value.divide(sum, 15, RoundingMode.HALF_UP);
        return energySumStandardCoal.multiply(divide);
    }


    /**
     * 能源转换率
     */
    private StatisticsHomeTop2Data getEnergyConversionRate(StatisticsParamHomeVO paramVO) {
        try {
            String cacheKey = StatisticsCacheConstants.COMPARISON_HOME_TOP2_ENERGY_CONVERSION_RATE + SecureUtil.md5(paramVO.toString());
            byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isNotEmpty(cacheRes)) {
                return JSON.parseObject(cacheRes, new TypeReference<StatisticsHomeTop2Data>() {
                });
            }
            StatisticsHomeTop2Data numeratorRes = getStandardCoalTop2Data(StandingBookStageEnum.PROCESSING_CONVERSION, true, EnergyClassifyEnum.PARK, paramVO);
            StatisticsHomeTop2Data denominatorRes = getStandardCoalTop2Data(StandingBookStageEnum.PROCUREMENT_STORAGE, false, EnergyClassifyEnum.OUTSOURCED, paramVO);
            // 获取最新时间
            LocalDateTime lastTime;
            if (Objects.isNull(numeratorRes.getDataUpdateTime()) || Objects.isNull(denominatorRes.getDataUpdateTime())) {
                lastTime = null;
            } else {
                lastTime = numeratorRes.getDataUpdateTime().isBefore(denominatorRes.getDataUpdateTime()) ? denominatorRes.getDataUpdateTime() : numeratorRes.getDataUpdateTime();
            }
            // 计算结果
            if (BigDecimal.ZERO.equals(denominatorRes.getValue())) {
                return StatisticsHomeTop2Data.builder().dataUpdateTime(lastTime).build();
            }
            StatisticsHomeTop2Data resultData = StatisticsHomeTop2Data.builder().dataUpdateTime(lastTime).value(safeDivide100(numeratorRes.getValue(), denominatorRes.getValue())).build();
            String jsonStr = JSONUtil.toJsonStr(resultData);
            byte[] bytes = StrUtils.compressGzip(jsonStr);
            byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
            return resultData;
        } catch (Exception e) {
            log.error("能源转换率计算异常", e);
            return StatisticsHomeTop2Data.builder().build();
        }
    }

    /**
     * 能源利用率（园区）
     */
    private StatisticsHomeTop2Data getParkEnergyUtilizationRate(StatisticsParamHomeVO paramVO) {
        try {
            String cacheKey = StatisticsCacheConstants.COMPARISON_HOME_TOP2_PARK_ENERGY_UTILIZATION_RATE + SecureUtil.md5(paramVO.toString());
            byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isNotEmpty(cacheRes)) {
                return JSON.parseObject(cacheRes, new TypeReference<StatisticsHomeTop2Data>() {
                });
            }
            StatisticsHomeTop2Data numeratorRes = getStandardCoalTop2Data(StandingBookStageEnum.TERMINAL_USE, false, null, paramVO);
            StatisticsHomeTop2Data denominatorRes = getStandardCoalTop2Data(StandingBookStageEnum.PROCESSING_CONVERSION, true, EnergyClassifyEnum.PARK, paramVO);
            // 获取最新时间
            LocalDateTime lastTime;
            if (Objects.isNull(numeratorRes.getDataUpdateTime()) || Objects.isNull(denominatorRes.getDataUpdateTime())) {
                lastTime = null;
            } else {
                lastTime = numeratorRes.getDataUpdateTime().isBefore(denominatorRes.getDataUpdateTime()) ? denominatorRes.getDataUpdateTime() : numeratorRes.getDataUpdateTime();
            }
            // 计算结果
            if (BigDecimal.ZERO.equals(denominatorRes.getValue())) {
                return StatisticsHomeTop2Data.builder().dataUpdateTime(lastTime).build();
            }
            StatisticsHomeTop2Data resultData = StatisticsHomeTop2Data.builder().dataUpdateTime(lastTime).value(safeDivide100(numeratorRes.getValue(), denominatorRes.getValue())).build();
            String jsonStr = JSONUtil.toJsonStr(resultData);
            byte[] bytes = StrUtils.compressGzip(jsonStr);
            byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
            return resultData;
        } catch (Exception e) {
            log.error("园区能源利用率计算异常", e);
            return StatisticsHomeTop2Data.builder().build();
        }

    }

    /**
     * 能源利用率（外购）
     */
    private StatisticsHomeTop2Data getOutsourceEnergyUtilizationRate(StatisticsParamHomeVO paramVO) {
        try {
            String cacheKey = StatisticsCacheConstants.COMPARISON_HOME_TOP2_OUTSOURCE_ENERGY_UTILIZATION_RATE + SecureUtil.md5(paramVO.toString());
            byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isNotEmpty(cacheRes)) {
                return JSON.parseObject(cacheRes, new TypeReference<StatisticsHomeTop2Data>() {
                });
            }
            StatisticsHomeTop2Data numeratorRes = getStandardCoalTop2Data(StandingBookStageEnum.TERMINAL_USE, false, null, paramVO);
            StatisticsHomeTop2Data denominatorRes = getStandardCoalTop2Data(StandingBookStageEnum.PROCUREMENT_STORAGE, true, EnergyClassifyEnum.OUTSOURCED, paramVO);
            // 获取最新时间
            LocalDateTime lastTime;
            if (Objects.isNull(numeratorRes.getDataUpdateTime()) || Objects.isNull(denominatorRes.getDataUpdateTime())) {
                lastTime = null;
            } else {
                lastTime = numeratorRes.getDataUpdateTime().isBefore(denominatorRes.getDataUpdateTime()) ? denominatorRes.getDataUpdateTime() : numeratorRes.getDataUpdateTime();
            }
            // 计算结果
            if (BigDecimal.ZERO.equals(denominatorRes.getValue())) {
                return StatisticsHomeTop2Data.builder().dataUpdateTime(lastTime).build();
            }
            StatisticsHomeTop2Data resultData = StatisticsHomeTop2Data.builder().dataUpdateTime(lastTime).value(safeDivide100(numeratorRes.getValue(), denominatorRes.getValue())).build();
            String jsonStr = JSONUtil.toJsonStr(resultData);
            byte[] bytes = StrUtils.compressGzip(jsonStr);
            byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
            return resultData;
        } catch (Exception e) {
            log.error("外购能源利用率计算异常", e);
            return StatisticsHomeTop2Data.builder().build();
        }
    }

    /**
     * 获取折标煤总量
     */
    private StatisticsHomeTop2Data getStandardCoalTop2Data(StandingBookStageEnum stageEnum, boolean toppest, EnergyClassifyEnum energyClassifyEnum, StatisticsParamHomeVO paramVO) {
        List<Long> stageSbIds = statisticsCommonService.getStageEnergySbIds(stageEnum.getCode(), toppest, energyClassifyEnum);
        if (CollUtil.isEmpty(stageSbIds)) {
            return StatisticsHomeTop2Data.builder().build();
        }

        if (CollUtil.isEmpty(stageSbIds)) {
            return StatisticsHomeTop2Data.builder().build();
        }
        BigDecimal sumStandardCoal = usageCostService.getSumStandardCoal(
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                stageSbIds);
        LocalDateTime updTime = usageCostService.getLastTimeNoParam(paramVO.getRange()[0],
                paramVO.getRange()[1],
                stageSbIds);
        return StatisticsHomeTop2Data.builder().dataUpdateTime(updTime).value(sumStandardCoal).build();
    }

    public StatisticsHomeBarVO<BigDecimal> analysisChart(StatisticsParamHomeVO paramVO, Function<StatisticsHomeChartResultVO, BigDecimal> accExtractor, Function<StatisticsHomeChartResultVO, BigDecimal> avgExtractor, String commonType) {
        // 1. 校验时间范围合法性
        StatisticsParamV2VO paramV2VO = BeanUtils.toBean(paramVO, StatisticsParamV2VO.class);
        statisticsCommonService.validParamConditionDate(paramV2VO);
        // 3. 尝试读取缓存
        String cacheKey = commonType + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsHomeBarVO<BigDecimal>>() {
            });
        }

        StatisticsHomeBarVO<BigDecimal> result = new StatisticsHomeBarVO<>();

        // 4. 查询能源信息及能源ID
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(
                null, paramVO.getEnergyClassify());

        if (CollUtil.isEmpty(energyList)) {
            return result;
        }
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        if (CollUtil.isEmpty(energyIds)) {
            return result;
        }

        List<Long> stageSbIds = new ArrayList<>();
        if (EnergyClassifyEnum.OUTSOURCED.getCode().equals(paramVO.getEnergyClassify())) {
            // 外购
            stageSbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                    StandingBookStageEnum.PROCUREMENT_STORAGE.getCode(),
                    false,
                    energyIds);
        } else if (EnergyClassifyEnum.PARK.getCode().equals(paramVO.getEnergyClassify())) {
            // 园区
            stageSbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                    StandingBookStageEnum.PROCESSING_CONVERSION.getCode(),
                    true,
                    energyIds);
        } else {
            // 不处理
        }

        // 6. 查询当前周期的数据
        List<StatisticsHomeChartResultVO> usageCostDataList = usageCostService.getListOfHomeBySbIds(paramV2VO, paramVO.getRange()[0], paramVO.getRange()[1], stageSbIds);
        // 7. 构建横轴时间（xdata）
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        LocalDateTime lastTime = usageCostService.getLastTimeNoParam(paramVO.getRange()[0], paramVO.getRange()[1], stageSbIds);
        result.setDataTime(lastTime);
        buildSimpleChart(result, usageCostDataList, xdata, accExtractor);


        String jsonStr = JSONUtil.toJsonStr(result);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return result;
    }

    private void buildSimpleChart(StatisticsHomeBarVO<BigDecimal> resultVO, List<StatisticsHomeChartResultVO> usageCostDataList,
                                  List<String> xdata,
                                  Function<StatisticsHomeChartResultVO, BigDecimal> accExtractor) {
        if (CollUtil.isEmpty(usageCostDataList)) {
            resultVO.setXdata(Collections.emptyList());
            resultVO.setYdata(Collections.emptyList());
            return;
        }
        // 时间点 -> 值 映射
        Map<String, StatisticsHomeChartResultVO> nowMap = usageCostDataList.stream()
                .collect(Collectors.toMap(
                        StatisticsHomeChartResultVO::getTime,  // 时间作为键
                        vo -> vo,  // 值为对象本身
                        (existing, replacement) -> existing  // 遇到重复键时保留第一个
                ));

        List<BigDecimal> nowList = new ArrayList<>();
        List<String> timeList = new ArrayList<>();
        for (String time : xdata) {
            StatisticsHomeChartResultVO chartResultVO = nowMap.get(time);
            if (Objects.isNull(chartResultVO)) {
                continue;
            }

            BigDecimal value = accExtractor.apply(chartResultVO);
            if (value == null) {
                continue;
            }
            nowList.add(dealBigDecimalScale(value, DEFAULT_SCALE));
            timeList.add(time);
        }
        BigDecimal sum = nowList.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);
        resultVO.setXdata(timeList);
        resultVO.setYdata(nowList);
        if (!timeList.isEmpty() && sum != null) {
            BigDecimal avg = sum.divide(new BigDecimal(timeList.size()), 2, RoundingMode.HALF_UP);
            resultVO.setAvg(avg);
        }


    }

    /**
     * 构建统计数据（当前、同比、环比）- 重构版，返回 List<StatisticsHomeData>
     */
    private void buildStatisticsHomeData(StatisticsOverviewStatisticsTableData nowRes,
                                         StatisticsOverviewStatisticsTableData prevRes,
                                         StatisticsOverviewStatisticsTableData lastRes,
                                         StatisticsHomeResultVO statisticsHomeResultVO
    ) {
        try {
            List<StatisticsHomeData> coalList = new ArrayList<>();
            coalList.add(buildSingleItem(ITEM_ACCUMULATE, nowRes.getAccCoal(), prevRes.getAccCoal(), CommonUtil.calculateYearOnYearRatio(nowRes.getAccCoal(), lastRes.getAccCoal()), CommonUtil.calculateYearOnYearRatio(nowRes.getAccCoal(), prevRes.getAccCoal())));
            coalList.add(buildSingleItem(ITEM_MAX, nowRes.getMaxCoal(), prevRes.getMaxCoal(), CommonUtil.calculateYearOnYearRatio(nowRes.getMaxCoal(), lastRes.getMaxCoal()), CommonUtil.calculateYearOnYearRatio(nowRes.getMaxCoal(), prevRes.getMaxCoal())));
            coalList.add(buildSingleItem(ITEM_MIN, nowRes.getMinCoal(), prevRes.getMinCoal(), CommonUtil.calculateYearOnYearRatio(nowRes.getMinCoal(), lastRes.getMinCoal()), CommonUtil.calculateYearOnYearRatio(nowRes.getMinCoal(), prevRes.getMinCoal())));
            coalList.add(buildSingleItem(ITEM_AVG, nowRes.getAvgCoal(), prevRes.getAvgCoal(), CommonUtil.calculateYearOnYearRatio(nowRes.getAvgCoal(), lastRes.getAvgCoal()), CommonUtil.calculateYearOnYearRatio(nowRes.getAvgCoal(), prevRes.getAvgCoal())));
            statisticsHomeResultVO.setStandardCoalStatistics(coalList);
            List<StatisticsHomeData> costList = new ArrayList<>();
            costList.add(buildSingleItem(ITEM_ACCUMULATE, nowRes.getAccCost(), prevRes.getAccCost(), CommonUtil.calculateYearOnYearRatio(nowRes.getAccCost(), lastRes.getAccCost()), CommonUtil.calculateYearOnYearRatio(nowRes.getAccCost(), prevRes.getAccCost())));
            costList.add(buildSingleItem(ITEM_MAX, nowRes.getMaxCost(), prevRes.getMaxCost(), CommonUtil.calculateYearOnYearRatio(nowRes.getMaxCost(), lastRes.getMaxCost()), CommonUtil.calculateYearOnYearRatio(nowRes.getMaxCost(), prevRes.getMaxCost())));
            costList.add(buildSingleItem(ITEM_MIN, nowRes.getMinCost(), prevRes.getMinCost(), CommonUtil.calculateYearOnYearRatio(nowRes.getMinCost(), lastRes.getMinCost()), CommonUtil.calculateYearOnYearRatio(nowRes.getMinCost(), prevRes.getMinCost())));
            costList.add(buildSingleItem(ITEM_AVG, nowRes.getAvgCost(), prevRes.getAvgCost(), CommonUtil.calculateYearOnYearRatio(nowRes.getAvgCost(), lastRes.getAvgCost()), CommonUtil.calculateYearOnYearRatio(nowRes.getAvgCost(), prevRes.getAvgCost())));
            statisticsHomeResultVO.setMoneyStatistics(costList);
        } catch (Exception e) {
            log.error("buildStatisticsHomeData error:{}", e.getMessage(), e);
        }
    }

    private StatisticsHomeData buildSingleItem(String itemName, BigDecimal now, BigDecimal previous, BigDecimal yoy, BigDecimal mom) {
        StatisticsHomeData data = new StatisticsHomeData();
        data.setItem(itemName);
        data.setNow(dealBigDecimalScale(now, DEFAULT_SCALE));
        data.setPrevious(dealBigDecimalScale(previous, DEFAULT_SCALE));
        data.setYOY(dealBigDecimalScale(yoy, DEFAULT_SCALE));
        data.setMOM(dealBigDecimalScale(mom, DEFAULT_SCALE));
        return data;
    }


}
