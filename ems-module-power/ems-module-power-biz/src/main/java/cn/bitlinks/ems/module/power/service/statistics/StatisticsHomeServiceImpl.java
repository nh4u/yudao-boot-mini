package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.EnergyClassifyEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.enums.ChartSeriesTypeEnum;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants;
import cn.bitlinks.ems.module.power.enums.StatisticsQueryType;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingBookStageEnum;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.safeDivide100;

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

    private List<StatisticsOverviewEnergyData> energyList(LocalDateTime startTime, LocalDateTime endTime, List<Long> energyIdList, List<EnergyConfigurationDO> energyList) {
        try {
            if (CollUtil.isEmpty(energyIdList)) {
                return emptyEnergyList();
            }
            List<StatisticsOverviewEnergyData> list = new ArrayList<>();
            List<UsageCostData> energyStandardCoalList = usageCostService.getEnergyStandardCoalByEnergyIds(startTime, endTime, energyIdList);

            Map<Long, UsageCostData> energyStandardCoalMap = Collections.emptyMap();
            if (CollUtil.isNotEmpty(energyStandardCoalList)) {
                energyStandardCoalMap = energyStandardCoalList
                        .stream()
                        .collect(Collectors.toMap(UsageCostData::getEnergyId, Function.identity()));
            }

            for (EnergyConfigurationDO e : energyList) {
                StatisticsOverviewEnergyData data = new StatisticsOverviewEnergyData();

                data.setName(e.getEnergyName());
                data.setEnergyIcon(e.getEnergyIcon());
                UsageCostData usageCostData = energyStandardCoalMap.get(e.getId());

                if (Objects.nonNull(usageCostData)) {
                    data.setConsumption(dealBigDecimalScale(usageCostData.getCurrentTotalUsage(), DEFAULT_SCALE));
                    data.setStandardCoal(dealBigDecimalScale(usageCostData.getTotalStandardCoalEquivalent(), DEFAULT_SCALE));
                    data.setMoney(dealBigDecimalScale(usageCostData.getTotalCost(), DEFAULT_SCALE));
                }
                list.add(data);
            }

            // 3.1.2 综合能耗
            BigDecimal totalMoney = list.stream()
                    .map(StatisticsOverviewEnergyData::getMoney)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal::add).orElse(null);

            BigDecimal totalStandardCoal = list.stream()
                    .map(StatisticsOverviewEnergyData::getStandardCoal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal::add).orElse(null);

            StatisticsOverviewEnergyData data = new StatisticsOverviewEnergyData();
            data.setName(OVERVIEW_ENERGY_STR);
            data.setStandardCoal(totalStandardCoal);
            data.setMoney(totalMoney);
            list.add(0, data);
            return list;
        } catch (Exception e) {
            log.error("统计总览【能源】发生异常：{}", e.getMessage(), e);
            return emptyEnergyList();
        }

    }


    @Override
    public StatisticsHomeResultVO overview(StatisticsParamHomeVO paramVO) {
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
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(paramVO.getEnergyClassify());
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
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        StatisticsParamV2VO param = new StatisticsParamV2VO();
        param.setRange(rangeOrigin);
        param.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());
        param.setDateType(paramVO.getDateType());
        param.setEnergyClassify(paramVO.getEnergyClassify());

        // 查询聚合数据
        StatisticsOverviewStatisticsTableData nowResult = usageCostService.getAggStatisticsByEnergyIds(startTime, endTime, energyIdList); // 当前
        // 上一周期
        StatisticsOverviewStatisticsTableData prevResult = usageCostService.getAggStatisticsByEnergyIds(
                LocalDateTimeUtils.getPreviousRange(rangeOrigin, dataTypeEnum)[0],
                LocalDateTimeUtils.getPreviousRange(rangeOrigin, dataTypeEnum)[1], energyIdList);
        // 同期
        StatisticsOverviewStatisticsTableData lastResult = usageCostService.getAggStatisticsByEnergyIds(startTime.minusYears(1), endTime.minusYears(1), energyIdList);

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

    @Override
    public ComparisonChartResultVO costChart(StatisticsParamHomeVO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalCost, StatisticsCacheConstants.COMPARISON_HOME_CHART_COST);
    }

    @Override
    public ComparisonChartResultVO coalChart(StatisticsParamHomeVO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalStandardCoalEquivalent, StatisticsCacheConstants.COMPARISON_HOME_CHART_COAL);
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
        // 单位产值能耗 todo
        StatisticsHomeTop2Data outputValueEnergyConsumption = new StatisticsHomeTop2Data();
        statisticsHomeResultVO.setOutputValueEnergyConsumption(outputValueEnergyConsumption);
        // 单位产品能耗（8英寸） todo
        StatisticsHomeTop2Data productEnergyConsumption8 = new StatisticsHomeTop2Data();
        statisticsHomeResultVO.setProductEnergyConsumption8(productEnergyConsumption8);
        // 单位产品能耗（12英寸） todo
        StatisticsHomeTop2Data productEnergyConsumption12 = new StatisticsHomeTop2Data();
        statisticsHomeResultVO.setProductEnergyConsumption12(productEnergyConsumption12);

        // 能源利用率（外购）
        statisticsHomeResultVO.setOutsourceEnergyUtilizationRate(getOutsourceEnergyUtilizationRate(paramVO));
        // 能源利用率（园区）
        statisticsHomeResultVO.setParkEnergyUtilizationRate(getParkEnergyUtilizationRate(paramVO));
        // 能源转换率
        statisticsHomeResultVO.setEnergyConversionRate(getEnergyConversionRate(paramVO));

        return statisticsHomeResultVO;

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


    public ComparisonChartResultVO analysisChart(StatisticsParamHomeVO paramVO, Function<UsageCostData, BigDecimal> valueExtractor, String commonType) {
        // 1. 校验时间范围合法性
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 2. 校验时间类型参数是否合法
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        // 3. 尝试读取缓存
        String cacheKey = commonType + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, ComparisonChartResultVO.class);
        }

        // 4. 查询能源信息及能源ID
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(
                null, paramVO.getEnergyClassify());
        ComparisonChartResultVO result = new ComparisonChartResultVO();
        if (CollUtil.isEmpty(energyList)) {
            result.setDataTime(LocalDateTime.now());
            return result;
        }
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 5. 查询台账信息（按能源）
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        if (CollUtil.isEmpty(standingbookIdsByEnergy)) {
            result.setDataTime(LocalDateTime.now());
            return result;
        }
        List<Long> standingBookIds = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        StatisticsParamV2VO param = new StatisticsParamV2VO();
        param.setRange(rangeOrigin);
        param.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());
        param.setDateType(paramVO.getDateType());
        param.setEnergyClassify(paramVO.getEnergyClassify());

        // 6. 查询当前周期的数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(param, startTime, endTime, standingBookIds);

        // 7. 构建横轴时间（xdata）
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);
        LocalDateTime lastTime = usageCostService.getLastTime(param, startTime, endTime, standingBookIds);

        // 8. 构建图表组（柱状图 + 折线图）
        List<ComparisonChartGroupVO> groupList = buildSimpleChart(usageCostDataList, xdata, dataTypeEnum, valueExtractor);

        // 9. 构建最终结果并缓存

        result.setList(groupList);
        result.setDataTime(lastTime);

        String jsonStr = JSONUtil.toJsonStr(result);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return result;
    }

    private List<ComparisonChartGroupVO> buildSimpleChart(List<UsageCostData> usageCostDataList,
                                                          List<String> xdata,
                                                          DataTypeEnum dataTypeEnum,
                                                          Function<UsageCostData, BigDecimal> valueExtractor) {
        // 时间点 -> 值 映射
        Map<String, BigDecimal> nowMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getTime,
                        Collectors.mapping(valueExtractor,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        List<BigDecimal> nowList = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;

        for (String time : xdata) {
            BigDecimal value = nowMap.getOrDefault(time, BigDecimal.ZERO);
            nowList.add(value);
            total = total.add(value);
            count++;
        }

        // 构造折线图：平均值序列
        BigDecimal average = count > 0 ? total.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        List<BigDecimal> averageList = Collections.nCopies(xdata.size(), average);

        List<ChartSeriesItemVO> ydata = Arrays.asList(
                new ChartSeriesItemVO("", ChartSeriesTypeEnum.BAR.getType(), nowList, null),
                new ChartSeriesItemVO("", ChartSeriesTypeEnum.LINE.getType(), averageList, 1)
        );

        ComparisonChartGroupVO group = new ComparisonChartGroupVO();
        group.setXdata(xdata);
        group.setYdata(ydata);

        return Collections.singletonList(group);
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
            coalList.add(buildSingleItem(ITEM_ACCUMULATE, nowRes.getAccCoal(), prevRes.getAccCoal(), calculateRatio(nowRes.getAccCoal(), lastRes.getAccCoal()), calculateRatio(nowRes.getAccCoal(), prevRes.getAccCoal())));
            coalList.add(buildSingleItem(ITEM_MAX, nowRes.getMaxCoal(), prevRes.getMaxCoal(), calculateRatio(nowRes.getMaxCoal(), lastRes.getMaxCoal()), calculateRatio(nowRes.getMaxCoal(), prevRes.getMaxCoal())));
            coalList.add(buildSingleItem(ITEM_MIN, nowRes.getMinCoal(), prevRes.getMinCoal(), calculateRatio(nowRes.getMinCoal(), lastRes.getMinCoal()), calculateRatio(nowRes.getMinCoal(), prevRes.getMinCoal())));
            coalList.add(buildSingleItem(ITEM_AVG, nowRes.getAvgCoal(), prevRes.getAvgCoal(), calculateRatio(nowRes.getAvgCoal(), lastRes.getAvgCoal()), calculateRatio(nowRes.getAvgCoal(), prevRes.getAvgCoal())));
            statisticsHomeResultVO.setStandardCoalStatistics(coalList);
            List<StatisticsHomeData> costList = new ArrayList<>();
            costList.add(buildSingleItem(ITEM_ACCUMULATE, nowRes.getAccCost(), prevRes.getAccCost(), calculateRatio(nowRes.getAccCost(), lastRes.getAccCost()), calculateRatio(nowRes.getAccCost(), prevRes.getAccCost())));
            costList.add(buildSingleItem(ITEM_MAX, nowRes.getMaxCost(), prevRes.getMaxCost(), calculateRatio(nowRes.getMaxCost(), lastRes.getMaxCost()), calculateRatio(nowRes.getMaxCost(), prevRes.getMaxCost())));
            costList.add(buildSingleItem(ITEM_MIN, nowRes.getMinCost(), prevRes.getMinCost(), calculateRatio(nowRes.getMinCost(), lastRes.getMinCost()), calculateRatio(nowRes.getMinCost(), prevRes.getMinCost())));
            costList.add(buildSingleItem(ITEM_AVG, nowRes.getAvgCost(), prevRes.getAvgCost(), calculateRatio(nowRes.getAvgCost(), lastRes.getAvgCost()), calculateRatio(nowRes.getAvgCost(), prevRes.getAvgCost())));
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

    /**
     * 构建基础统计项（累计、平均、最大、最小）
     */
    private StatisticsOverviewStatisticsData buildBasicStats(List<UsageCostData> dataList,
                                                             Function<UsageCostData, BigDecimal> extractor) {
        StatisticsOverviewStatisticsData stats = new StatisticsOverviewStatisticsData();

        if (CollUtil.isEmpty(dataList)) {
            stats.setAccumulate(BigDecimal.ZERO);
            stats.setAverage(BigDecimal.ZERO);
            stats.setMax(BigDecimal.ZERO);
            stats.setMin(BigDecimal.ZERO);
            return stats;
        }

        List<BigDecimal> values = dataList.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = values.isEmpty() ? BigDecimal.ZERO :
                sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
        BigDecimal max = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal min = values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        stats.setAccumulate(sum);
        stats.setAverage(avg);
        stats.setMax(max);
        stats.setMin(min);
        return stats;
    }

//    /**
//     * 构建同比/环比差值百分比
//     */
//    private StatisticsOverviewStatisticsData buildRatioStats(List<UsageCostData> nowList,
//                                                             List<UsageCostData> refList,
//                                                             Function<UsageCostData, BigDecimal> extractor) {
//        BigDecimal nowSum = nowList.stream()
//                .map(extractor)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal refSum = refList.stream()
//                .map(extractor)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        BigDecimal ratio = calculateRatio(nowSum, refSum);
//
//        StatisticsOverviewStatisticsData result = new StatisticsOverviewStatisticsData();
//        result.setAccumulate(ratio);
//        result.setAverage(BigDecimal.ZERO); // 可选扩展：对每项做同比
//        result.setMax(BigDecimal.ZERO);
//        result.setMin(BigDecimal.ZERO);
//        return result;
//    }
//
//    /**
//     * 构建同比/环比差值百分比
//     */
//    private StatisticsOverviewStatisticsData buildRatioStats(StatisticsOverviewStatisticsData nowStats,
//                                                             StatisticsOverviewStatisticsData refStats) {
//        StatisticsOverviewStatisticsData result = new StatisticsOverviewStatisticsData();
//        result.setAccumulate(calculateRatio(nowStats.getAccumulate(), refStats.getAccumulate()));
//        result.setAverage(calculateRatio(nowStats.getAverage(), refStats.getAverage()));
//        result.setMax(calculateRatio(nowStats.getMax(), refStats.getMax()));
//        result.setMin(calculateRatio(nowStats.getMin(), refStats.getMin()));
//        return result;
//    }

    /**
     * 同比/环比率计算（避免除零）
     */
    private BigDecimal calculateRatio(BigDecimal now, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0 || now == null) {
            return null;
        }
        return now.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

}
