package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.enums.ChartSeriesTypeEnum;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
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
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

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

    @Override
    public StatisticsHomeResultVO overview(StatisticsParamV2VO paramVO) {
        StatisticsHomeResultVO statisticsHomeResultVO = new StatisticsHomeResultVO();

        // 尝试读取缓存
        String cacheKey = StatisticsCacheConstants.COMPARISON_HOME_TOTAL + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsHomeResultVO.class);
        }

        // 计量器具、重点设备、其他设备
        statisticsHomeResultVO.setMeasurementInstrumentNum(standingbookService.count(CommonConstants.MEASUREMENT_INSTRUMENT_ID));
        statisticsHomeResultVO.setKeyEquipmentNum(standingbookService.count(CommonConstants.KEY_EQUIPMENT_ID));
        statisticsHomeResultVO.setOtherEquipmentNum(standingbookService.count(CommonConstants.OTHER_EQUIPMENT_ID));

        // 查询能源类型与台账
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(
                new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        List<Long> energyIdList = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIdList);
        List<Long> standingBookIds = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        statisticsHomeResultVO.setDataUpdateTime(LocalDateTime.now());
        if (CollectionUtil.isEmpty(standingBookIds)) {
            return statisticsHomeResultVO;
        }

        // 时间参数准备
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());

        // 查询数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, startTime, endTime, standingBookIds); // 当前
        // 上一周期
        List<UsageCostData> comparisonDataList = usageCostService.getList(paramVO,
                LocalDateTimeUtils.getPreviousRange(rangeOrigin, dataTypeEnum)[0],
                LocalDateTimeUtils.getPreviousRange(rangeOrigin, dataTypeEnum)[1], standingBookIds);
        // 同期
        List<UsageCostData> yoyDataList = usageCostService.getList(paramVO,
                LocalDateTimeUtils.getSamePeriodLastYear(rangeOrigin, dataTypeEnum)[0],
                LocalDateTimeUtils.getSamePeriodLastYear(rangeOrigin, dataTypeEnum)[1], standingBookIds);

        // 折标煤统计
        StatisticsHomeData standardCoalStatistics = buildStatisticsHomeData(
                usageCostDataList, comparisonDataList, yoyDataList, UsageCostData::getTotalStandardCoalEquivalent);
        //折价统计
        StatisticsHomeData moneyStatistics = buildStatisticsHomeData(
                usageCostDataList, comparisonDataList, yoyDataList, UsageCostData::getTotalCost);

        statisticsHomeResultVO.setStandardCoalStatistics(standardCoalStatistics);
        statisticsHomeResultVO.setMoneyStatistics(moneyStatistics);
        statisticsHomeResultVO.setDataUpdateTime(LocalDateTime.now());

        String jsonStr = JSONUtil.toJsonStr(statisticsHomeResultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        return statisticsHomeResultVO;
    }

    @Override
    public ComparisonChartResultVO costChart(StatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalCost,StatisticsCacheConstants.COMPARISON_HOME_CHART_COST);
    }

    @Override
    public ComparisonChartResultVO coalChart(StatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalStandardCoalEquivalent,StatisticsCacheConstants.COMPARISON_HOME_CHART_COAL);
    }


    public ComparisonChartResultVO analysisChart(StatisticsParamV2VO paramVO, Function<UsageCostData, BigDecimal> valueExtractor,String commonType) {
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
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, ComparisonChartResultVO.class);
        }

        // 4. 查询能源信息及能源ID
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(
                new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 5. 查询台账信息（按能源）
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIds = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        // 6. 查询当前周期的数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, startTime, endTime, standingBookIds);

        // 7. 构建横轴时间（xdata）
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);
        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, startTime, endTime, standingBookIds);

        // 8. 构建图表组（柱状图 + 折线图）
        List<ComparisonChartGroupVO> groupList = buildSimpleChart(usageCostDataList, xdata, dataTypeEnum, valueExtractor);

        // 9. 构建最终结果并缓存
        ComparisonChartResultVO result = new ComparisonChartResultVO();
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
     * 构建统计数据（当前、同比、环比）
     */
    private StatisticsHomeData buildStatisticsHomeData(List<UsageCostData> nowList,
                                                       List<UsageCostData> previousList,
                                                       List<UsageCostData> yoyList,
                                                       Function<UsageCostData, BigDecimal> extractor) {
        StatisticsHomeData result = new StatisticsHomeData();

        result.setNow(buildBasicStats(nowList, extractor));
        result.setPrevious(buildBasicStats(previousList, extractor));
        result.setMOM(buildRatioStats(nowList, previousList, extractor)); // 环比
        result.setYOY(buildRatioStats(nowList, yoyList, extractor));       // 同比

        return result;
    }

    /**
     * 构建基础统计项（累计、平均、最大、最小）
     */
    private StatisticsOverviewStatisticsData buildBasicStats(List<UsageCostData> dataList,
                                                             Function<UsageCostData, BigDecimal> extractor) {
        StatisticsOverviewStatisticsData stats = new StatisticsOverviewStatisticsData();

        if (CollectionUtil.isEmpty(dataList)) {
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

    /**
     * 构建同比/环比差值百分比
     */
    private StatisticsOverviewStatisticsData buildRatioStats(List<UsageCostData> nowList,
                                                             List<UsageCostData> refList,
                                                             Function<UsageCostData, BigDecimal> extractor) {
        BigDecimal nowSum = nowList.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refSum = refList.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ratio = calculateRatio(nowSum, refSum);

        StatisticsOverviewStatisticsData result = new StatisticsOverviewStatisticsData();
        result.setAccumulate(ratio);
        result.setAverage(BigDecimal.ZERO); // 可选扩展：对每项做同比
        result.setMax(BigDecimal.ZERO);
        result.setMin(BigDecimal.ZERO);
        return result;
    }

    /**
     * 同比/环比率计算（避免除零）
     */
    private BigDecimal calculateRatio(BigDecimal now, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0 || now == null) {
            return BigDecimal.ZERO;
        }
        return now.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

}
