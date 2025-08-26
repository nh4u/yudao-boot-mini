package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.EnergyClassifyEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.enums.StatisticsQueryType;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingBookStageEnum;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.ENERGY_UTILIZATION_RATE_CHART;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.ENERGY_UTILIZATION_RATE_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.getConvertData;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.safeDivide100;

@Service
@Validated
@Slf4j
public class EnergyUtilizationRateServiceImpl implements EnergyUtilizationRateService {
    @Resource
    private StatisticsCommonService statisticsCommonService;
    @Resource
    private UsageCostService usageCostService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;
    public static final String UTILIZATION_RATE_STR = "利用率";

    @Override
    public StatisticsResultV2VO<EnergyUtilizationRateInfo> getTable(StatisticsParamV2VO paramVO) {
        paramVO.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());
        // 校验条件的合法性
        statisticsCommonService.validParamConditionDate(paramVO);

        String cacheKey = ENERGY_UTILIZATION_RATE_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsResultV2VO<EnergyUtilizationRateInfo>>() {
            });
        }
        // 构建表头
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        StatisticsResultV2VO<EnergyUtilizationRateInfo> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);

        // 查询台账id
        // 查询园区利用率 台账ids
        List<Long> sbIds = statisticsCommonService.getStageEnergySbIds(StandingBookStageEnum.TERMINAL_USE.getCode(), false, null);
        List<Long> outsourceSbIds = statisticsCommonService.getStageEnergySbIds(StandingBookStageEnum.PROCUREMENT_STORAGE.getCode(), true, EnergyClassifyEnum.OUTSOURCED);
        List<Long> parkSbIds = statisticsCommonService.getStageEnergySbIds(StandingBookStageEnum.PROCESSING_CONVERSION.getCode(), true, EnergyClassifyEnum.PARK);

        // 无台账数据直接返回
        if (CollUtil.isEmpty(sbIds)) {
            return defaultNullData(tableHeader);
        }

        // 查询外购
        List<UsageCostData> outsourceList = new ArrayList<>();
        if (CollUtil.isNotEmpty(outsourceSbIds)) {
            outsourceList = usageCostService.getList(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], outsourceSbIds);
        }
        // 查询园区
        List<UsageCostData> parkList = new ArrayList<>();
        if (CollUtil.isNotEmpty(parkSbIds)) {
            parkList = usageCostService.getList(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], parkSbIds);
        }
        // 查询分子
        List<UsageCostData> numeratorList = usageCostService.getList(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], sbIds);

        // 综合默认查看
        List<EnergyUtilizationRateInfo> statisticsInfoList = queryList(outsourceList, parkList, numeratorList, tableHeader);

        // 设置最终返回值
        resultVO.setStatisticsInfoList(statisticsInfoList);
        LocalDateTime lastTime1 = usageCostService.getLastTimeNoParam(
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                outsourceSbIds
        );
        LocalDateTime lastTime2 = usageCostService.getLastTimeNoParam(
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                parkSbIds
        );
        LocalDateTime lastTime3 = usageCostService.getLastTimeNoParam(
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                sbIds
        );
        // 找出三个时间中的最大值（最新时间）
        LocalDateTime latestTime = Arrays.stream(new LocalDateTime[]{lastTime1, lastTime2, lastTime3})
                .max(Comparator.naturalOrder())
                .orElse(null);
        resultVO.setDataTime(latestTime);
        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }

    private StatisticsResultV2VO<EnergyUtilizationRateInfo> defaultNullData(List<String> tableHeader) {
        StatisticsResultV2VO<EnergyUtilizationRateInfo> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);
        List<EnergyUtilizationRateInfo> infoList = new ArrayList<>();

        EnergyUtilizationRateInfo osInfo = new EnergyUtilizationRateInfo();
        osInfo.setEnergyUtilizationRateInfoDataList(Collections.emptyList());
        osInfo.setItemName(EnergyClassifyEnum.OUTSOURCED.getDetail() + UTILIZATION_RATE_STR);

        EnergyUtilizationRateInfo parkInfo = new EnergyUtilizationRateInfo();
        parkInfo.setEnergyUtilizationRateInfoDataList(Collections.emptyList());
        parkInfo.setItemName(EnergyClassifyEnum.PARK.getDetail() + UTILIZATION_RATE_STR);
        infoList.add(osInfo);
        infoList.add(parkInfo);
        resultVO.setStatisticsInfoList(infoList);
        return resultVO;
    }

    private EnergyUtilizationRateInfo getUtilizationRateInfo(EnergyClassifyEnum energyClassifyEnum, Map<String, TimeAndNumData> denominatorMap, Map<String, TimeAndNumData> numeratorMap, List<String> tableHeader) {

        List<EnergyUtilizationRateInfoData> dataList = new ArrayList<>();
        for (String time : tableHeader) {
            TimeAndNumData numeratorData = numeratorMap.get(time);
            BigDecimal numeratorValue = Optional.ofNullable(numeratorData)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            TimeAndNumData denominatorData = denominatorMap.get(time);
            BigDecimal denominatorValue = Optional.ofNullable(denominatorData)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);
            BigDecimal ratio = safeDivide100(numeratorValue, denominatorValue);
            dataList.add(new EnergyUtilizationRateInfoData(time, ratio));
        }

        // 汇总统计
        BigDecimal sumDenominator = denominatorMap.values().stream().filter(Objects::nonNull).map(data -> data.getNum() != null ? data.getNum() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumNumerator = numeratorMap.values().stream().filter(Objects::nonNull).map(data -> data.getNum() != null ? data.getNum() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ratio = safeDivide100(sumNumerator, sumDenominator);
        // 构造结果对象
        EnergyUtilizationRateInfo info = new EnergyUtilizationRateInfo();
        info.setEnergyUtilizationRateInfoDataList(dataList);
        info.setItemName(energyClassifyEnum.getDetail() + UTILIZATION_RATE_STR);
        info.setPeriodRate(ratio);
        return info;
    }

    /**
     * 按能源维度统计：以 energyId 为主键，构建同比统计数据
     */
    private List<EnergyUtilizationRateInfo> queryList(List<UsageCostData> outsourceList,
                                                      List<UsageCostData> parkList,
                                                      List<UsageCostData> numeratorList,
                                                      List<String> tableHeader) {
        if (CollUtil.isEmpty(outsourceList)) {
            outsourceList = Collections.emptyList();
        }
        if (CollUtil.isEmpty(parkList)) {
            parkList = Collections.emptyList();
        }
        if (CollUtil.isEmpty(numeratorList)) {
            numeratorList = Collections.emptyList();
        }
        Map<String, TimeAndNumData> outsourceMap = getTimeAndNumDataMap(outsourceList);
        Map<String, TimeAndNumData> parkMap = getTimeAndNumDataMap(parkList);
        Map<String, TimeAndNumData> numeratorMap = getTimeAndNumDataMap(numeratorList);

        List<EnergyUtilizationRateInfo> result = new ArrayList<>();

        result.add(getUtilizationRateInfo(EnergyClassifyEnum.OUTSOURCED, outsourceMap, numeratorMap, tableHeader));
        result.add(getUtilizationRateInfo(EnergyClassifyEnum.PARK, parkMap, numeratorMap, tableHeader));
        return result;

    }

    /**
     * 根据 usageCostDataList 来获取按时间分组的数据Map
     *
     * @param usageCostDataList
     * @return
     */
    private Map<String, TimeAndNumData> getTimeAndNumDataMap(List<UsageCostData> usageCostDataList) {
        return usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getTime,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    BigDecimal totalStandardCoal = list.stream()
                                            .map(UsageCostData::getTotalStandardCoalEquivalent)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    return new TimeAndNumData(list.get(0).getTime(), totalStandardCoal);
                                }
                        )
                ));

    }

    @Override
    public List<BaseReportChartResultVO<BigDecimal>> getChart(StatisticsParamV2VO paramVO) {
        // 校验参数
        statisticsCommonService.validParamConditionDate(paramVO);

        String cacheKey = ENERGY_UTILIZATION_RATE_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<List<BaseReportChartResultVO<BigDecimal>>>() {
            });
        }
        StatisticsResultV2VO<EnergyUtilizationRateInfo> tableResult = getTable(paramVO);

        List<BaseReportChartResultVO<BigDecimal>> resultVOList = new ArrayList<>();
        // x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));


        List<EnergyUtilizationRateInfo> tableDataList = tableResult.getStatisticsInfoList();

        tableDataList.forEach(info -> {
            BaseReportChartResultVO<BigDecimal> resultVO = new BaseReportChartResultVO<>();
            List<EnergyUtilizationRateInfoData> dateList = info.getEnergyUtilizationRateInfoDataList();
            if (CollUtil.isEmpty(dateList)) {
                return;
            }
            // 处理空数据
            dateList.forEach(data -> {
                data.setRate(data.getRate() == null ? BigDecimal.ZERO : data.getRate());
            });
            Map<String, EnergyUtilizationRateInfoData> timeMap = dateList.stream()
                    .filter(data -> data.getDate() != null)
                    .collect(Collectors.toMap(
                            EnergyUtilizationRateInfoData::getDate,
                            data -> data,
                            (existing, replacement) -> replacement // 处理重复时间，保留后者
                    ));
            List<BigDecimal> nowList = xdata.stream().map(time -> {
                        EnergyUtilizationRateInfoData infoData = timeMap.get(time);
                        if (Objects.isNull(infoData)) {
                            return BigDecimal.ZERO;
                        }
                        return infoData.getRate();
                    }
            ).collect(Collectors.toList());
            LocalDateTime lastTime = tableResult.getDataTime();
            resultVO.setDataTime(lastTime);
            resultVO.setYdata(nowList);
            resultVO.setXdata(xdata);
            resultVOList.add(resultVO);
        });


        String jsonStr = JSONUtil.toJsonStr(resultVOList);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVOList;
    }

    @Override
    public List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO) {
        statisticsCommonService.validParamConditionDate(paramVO);

        List<List<String>> list = ListUtils.newArrayList();
        list.add(Arrays.asList("表单名称", "统计周期", ""));
        String sheetName = "利用率";
        // 统计周期
        String period = getFormatTime(paramVO.getRange()[0]) + "~" + getFormatTime(paramVO.getRange()[1]);

        // 月份处理
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, period, x));
        });
        list.add(Arrays.asList(sheetName, period, "周期利用率（%）"));
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(StatisticsParamV2VO paramVO) {
        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();

        StatisticsResultV2VO<EnergyUtilizationRateInfo> resultVO = getTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<EnergyUtilizationRateInfo> energyUtilizationRateInfo = resultVO.getStatisticsInfoList();

        for (EnergyUtilizationRateInfo s : energyUtilizationRateInfo) {
            List<Object> data = ListUtils.newArrayList();
            data.add(s.getItemName());
            // 处理数据
            List<EnergyUtilizationRateInfoData> energyUtilizationRateInfoDataList = s.getEnergyUtilizationRateInfoDataList();

            Map<String, EnergyUtilizationRateInfoData> dateMap = energyUtilizationRateInfoDataList.stream()
                    .collect(Collectors.toMap(EnergyUtilizationRateInfoData::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                EnergyUtilizationRateInfoData energyUtilizationRateInfoData = dateMap.get(date);
                if (energyUtilizationRateInfoData == null) {
                    data.add("/");
                } else {
                    data.add(getConvertData(energyUtilizationRateInfoData.getRate()));
                }
            });
            // 处理周期合计
            data.add(getConvertData(s.getPeriodRate()));

            result.add(data);
        }

        return result;
    }
}
