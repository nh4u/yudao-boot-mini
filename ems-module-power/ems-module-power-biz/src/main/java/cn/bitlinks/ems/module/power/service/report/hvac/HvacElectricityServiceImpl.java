package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.TimeAndNumData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energygroup.EnergyGroupDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.energygroup.EnergyGroupService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsCommonService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.REPORT_HVAC_ELECTRICITY;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.HVAC_ELECTRICITY_CHART;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.HVAC_ELECTRICITY_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

@Service
@Validated
@Slf4j
public class HvacElectricityServiceImpl implements HvacElectricityService {
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Resource
    private UsageCostService usageCostService;

    private final Integer scale = DEFAULT_SCALE;
    @Resource
    private LabelConfigService labelConfigService;
    @Resource
    private EnergyGroupService energyGroupService;
    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StatisticsCommonService statisticsCommonService;

    private final String DEFAULT_ENERGY_GROUP = "电力";
    private final String periodSumKey = "periodSum";
    /**
     * 报表统计标签，存放到字典中。
     *
     * @return
     */
    private LinkedHashMap<String, String> getItemMapping(List<String> labelCodes) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        List<String> gasSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_HVAC_ELECTRICITY);
        for (String label : gasSbLabels) {
            String labelCode = DictFrameworkUtils.parseDictDataValue(REPORT_HVAC_ELECTRICITY, label);
            if (CollUtil.isNotEmpty(labelCodes) && !labelCodes.contains(labelCode)) {
                continue;
            }
            result.put(labelCode, label);
        }
        return result;
    }

    /**
     * 表格返回空处理
     *
     * @param itemMapping
     * @param tableHeader
     * @return
     */
    private BaseReportResultVO<HvacElectricityInfo> defaultNullData(LinkedHashMap<String, String> itemMapping, List<String> tableHeader) {
        BaseReportResultVO<HvacElectricityInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
        resultVO.setDataTime(LocalDateTime.now());
        List<HvacElectricityInfo> infoList = new ArrayList<>();
        itemMapping.forEach((itemName, sbCode) -> {
            HvacElectricityInfo info = new HvacElectricityInfo();
            info.setItemName(itemName);
            info.setHvacElectricityInfoDataList(Collections.emptyList());
            infoList.add(info);
        });
        resultVO.setReportDataList(infoList);
        return resultVO;
    }

    @Override
    public BaseReportResultVO<HvacElectricityInfo> getTable(HvacElectricityParamVO paramVO) {

        // 校验参数
        validCondition(paramVO);
        String cacheKey = HVAC_ELECTRICITY_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportResultVO<HvacElectricityInfo>>() {
            });
        }

        // 表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));


        // 查询字典统计标签
        LinkedHashMap<String, String> itemMapping = getItemMapping(paramVO.getLabelCodes());

        List<LabelConfigDO> itemsLabel = labelConfigService.getByCodes(new ArrayList<>(itemMapping.keySet()));

        // 创建一个Map用于存储code到id的映射
        Map<String, String> itemCodeToIdMap = new HashMap<>();

        // 遍历列表，提取id和code放入Map
        for (LabelConfigDO labelConfig : itemsLabel) {
            String code = labelConfig.getCode();
            Long id = labelConfig.getId();
            if (code != null && id != null) {
                itemCodeToIdMap.put(code, String.valueOf(id));
            }
        }
        // 条件筛选出台账
        // 查询能源信息
        EnergyGroupDO energyGroupDO = energyGroupService.getEnergyGroupByName(DEFAULT_ENERGY_GROUP);
        if (Objects.isNull(energyGroupDO)) {
            return defaultNullData(itemMapping, tableHeader);
        }
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyGroup(energyGroupDO.getId());

        if (CollUtil.isEmpty(energyList)) {
            return defaultNullData(itemMapping, tableHeader);
        }
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 查询台账信息（先按能源）
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);

        // 查询标签信息（按标签过滤台账）
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService
                .getStandingbookIdsByDefaultLabel(new ArrayList<>(itemCodeToIdMap.values()));

        if (CollUtil.isEmpty(standingbookIdsByLabel)) {
            return defaultNullData(itemMapping, tableHeader);
        }
        List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
        List<StandingbookDO> collect = standingbookIdsByEnergy.stream().filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
        if (ArrayUtil.isEmpty(collect)) {
            return defaultNullData(itemMapping, tableHeader);
        }
        List<Long> standingBookIds = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());


        // 无台账数据直接返回
        if (CollUtil.isEmpty(standingBookIds)) {
            return defaultNullData(itemMapping, tableHeader);
        }

        // 查询 当前周期 用量
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);
        // 查询当前周期折扣数据
        List<UsageCostData> lastYearUsageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0].minusYears(1), paramVO.getRange()[1].minusYears(1), standingBookIds);

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        List<HvacElectricityInfo> hvacElectricityInfos = new ArrayList<>(queryByDefaultLabel(
                standingbookIdsByLabel,
                usageCostDataList,
                lastYearUsageCostDataList,
                labelMap,
                DataTypeEnum.codeOf(paramVO.getDateType()),
                itemMapping
        ));

        BaseReportResultVO<HvacElectricityInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
        // 设置最终返回值
        resultVO.setReportDataList(hvacElectricityInfos);
        LocalDateTime lastTime = usageCostService.getLastTimeNoParam(
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);
        resultVO.setDataTime(lastTime);

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }

//    @Override
//    public List<List<String>> getExcelHeader(HvacElectricityParamVO paramVO) {
//        validCondition(paramVO);
//
//        List<List<String>> list = ListUtils.newArrayList();
//        list.add(Arrays.asList("表单名称", "统计周期", ""));
//        String sheetName = "天然气用量";
//        // 统计周期
//        String period = getFormatTime(paramVO.getRange()[0]) + "~" + getFormatTime(paramVO.getRange()[1]);
//
//        // 月份处理
//        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
//        xdata.forEach(x -> {
//            list.add(Arrays.asList(sheetName, period, x));
//        });
//        list.add(Arrays.asList(sheetName, period, "周期合计"));
//        return list;
//    }
//
//    @Override
//    public List<List<Object>> getExcelData(HvacElectricityParamVO paramVO) {
//        // 结果list
//        List<List<Object>> result = ListUtils.newArrayList();
//
//        BaseReportResultVO<HvacElectricityInfo> resultVO = getTable(paramVO);
//        List<String> tableHeader = resultVO.getHeader();
//
//        List<HvacElectricityInfo> hvacElectricityInfos = resultVO.getReportDataList();
//
//        for (HvacElectricityInfo s : hvacElectricityInfos) {
//
//            List<Object> data = ListUtils.newArrayList();
//
//            data.add(s.getItemName());
//
//            // 处理数据
//            List<HvacElectricityInfoData> hvacElectricityInfoDatas = s.getHvacElectricityInfoDataList();
//
//            Map<String, HvacElectricityInfoData> dateMap = hvacElectricityInfoDatas.stream()
//                    .collect(Collectors.toMap(HvacElectricityInfoData::getDate, Function.identity()));
//
//            tableHeader.forEach(date -> {
//                HvacElectricityInfoData hvacElectricityInfoData = dateMap.get(date);
//                if (hvacElectricityInfoData == null) {
//                    data.add("/");
//                } else {
//                    BigDecimal consumption = hvacElectricityInfoData.get();
//                    data.add(getConvertData(consumption));
//                }
//            });
//
//            BigDecimal periodSum = s.getPeriodSum();
//            // 处理周期合计
//            data.add(getConvertData(periodSum));
//
//            result.add(data);
//        }
//
//        return result;
//    }

    private void validCondition(BaseTimeDateParamVO paramVO) {
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        //时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        //时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }
    }

    /**
     * 按标签维度统计：以 standingbookId 和标签结构为基础构建同比对比数据
     */
    private List<HvacElectricityInfo> queryByDefaultLabel(List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                          List<UsageCostData> usageCostDataList,
                                                          List<UsageCostData> lastUsageCostDataList,
                                                          Map<Long, LabelConfigDO> labelMap,
                                                          DataTypeEnum dataTypeEnum,
                                                          LinkedHashMap<String, String> itemMapping
    ) {
        //以value 分组 台账id
        Map<String, List<StandingbookLabelInfoDO>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        StandingbookLabelInfoDO::getValue)
                );

        // 当前周期数据按 standingbookId 分组
        Map<Long, List<UsageCostData>> currentMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        // 同期数据以 standingbookId + time 为key 构建map
        Map<String, UsageCostData> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.toMap(
                        d -> d.getStandingbookId() + "_" + d.getTime(),
                        Function.identity(),
                        (a, b) -> a
                ));


        List<HvacElectricityInfo> resultList = new ArrayList<>();


        grouped.forEach((valueKey, labelInfoList) -> {

            String[] labelIds = valueKey.split(",");
            LabelConfigDO label = labelMap.get(Long.valueOf(labelIds[labelIds.length - 1]));
            if (Objects.isNull(label)) {
                return;
            }
            String labelCode = label.getCode();

            List<UsageCostData> labelUsageListNow = new ArrayList<>();
            List<UsageCostData> labelUsageListPrevious = new ArrayList<>();

            // 获取本期标签关联的台账id，并取到对应的数据
            // 获取本期
            labelInfoList.forEach(labelInfo -> {
                List<UsageCostData> usageList = currentMap.get(labelInfo.getStandingbookId());
                if (usageList == null || usageList.isEmpty()) {
                    return; // 计量器具没有数据，跳过
                }
                labelUsageListNow.addAll(usageList);
            });

            // 获取去年同期
            labelUsageListNow.forEach(u -> {
                String previousTime = LocalDateTimeUtils.getYearOnYearTime(u.getTime(), dataTypeEnum);
                String key = u.getStandingbookId() + "_" + previousTime;
                UsageCostData previous = lastMap.get(key);
                if (Objects.isNull(previous)) {
                    return; // 计量器具没有数据，跳过
                }
                labelUsageListPrevious.add(previous);
            });

            // 1.处理当前
            List<TimeAndNumData> nowList = new ArrayList<>(labelUsageListNow.stream().collect(Collectors.groupingBy(
                    UsageCostData::getTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                BigDecimal totalConsumption = list.stream()
                                        .map(UsageCostData::getCurrentTotalUsage)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new TimeAndNumData(list.get(0).getTime(), totalConsumption);
                            }
                    )
            )).values());
            // 2.处理上期
            Map<String, TimeAndNumData> previousMap = labelUsageListPrevious.stream()
                    .collect(Collectors.groupingBy(
                            UsageCostData::getTime,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        BigDecimal totalStandardCoal = list.stream()
                                                .map(UsageCostData::getCurrentTotalUsage)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new TimeAndNumData(list.get(0).getTime(), totalStandardCoal);
                                    }
                            )
                    ));

            // 构造同比详情列表
            List<HvacElectricityInfoData> dataList = nowList.stream()
                    .map(current -> {
                        String previousTime = LocalDateTimeUtils.getYearOnYearTime(current.getTime(), dataTypeEnum);
                        TimeAndNumData previous = previousMap.get(previousTime);
                        BigDecimal now = Optional.ofNullable(current.getNum()).orElse(BigDecimal.ZERO);
                        BigDecimal last = previous != null ? Optional.ofNullable(previous.getNum()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
                        BigDecimal ratio = calculateYearOnYearRatio(now, last);
                        return new HvacElectricityInfoData(current.getTime(), now, last, ratio);
                    })
                    .sorted(Comparator.comparing(HvacElectricityInfoData::getDate))
                    .collect(Collectors.toList());

            // 汇总统计
            BigDecimal sumNow = dataList.stream().map(HvacElectricityInfoData::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumPrevious = dataList.stream().map(HvacElectricityInfoData::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumRatio = calculateYearOnYearRatio(sumNow, sumPrevious);

            // 构造结果对象
            HvacElectricityInfo info = new HvacElectricityInfo();
            info.setItemName(itemMapping.get(labelCode));
            info.setItemCode(labelCode);

            dataList = dataList.stream().peek(i -> {
                i.setNow(dealBigDecimalScale(i.getNow(), DEFAULT_SCALE));
                i.setPrevious(dealBigDecimalScale(i.getPrevious(), DEFAULT_SCALE));
                i.setRatio(dealBigDecimalScale(i.getRatio(), DEFAULT_SCALE));
            }).collect(Collectors.toList());
            info.setHvacElectricityInfoDataList(dataList);

            info.setPeriodNow(dealBigDecimalScale(sumNow, DEFAULT_SCALE));
            info.setPeriodPrevious(dealBigDecimalScale(sumPrevious, DEFAULT_SCALE));
            info.setPeriodRatio(dealBigDecimalScale(sumRatio, DEFAULT_SCALE));

            resultList.add(info);
        });
        // 提取有序的key列表
        List<String> orderedCodes = new ArrayList<>(itemMapping.keySet());

        // 按itemCode在orderedCodes中的索引位置排序
        return resultList.stream()
                .sorted((info1, info2) -> {
                    // 获取两个元素在有序列表中的位置
                    int index1 = orderedCodes.indexOf(info1.getItemCode());
                    int index2 = orderedCodes.indexOf(info2.getItemCode());

                    // 不在映射中的元素放后面（索引为-1）
                    index1 = index1 == -1 ? Integer.MAX_VALUE : index1;
                    index2 = index2 == -1 ? Integer.MAX_VALUE : index2;

                    return Integer.compare(index1, index2);
                })
                .collect(Collectors.toList());

    }




    @Override
    public BaseReportMultiChartResultVO<Map<String, List<BigDecimal>>> getChart(HvacElectricityParamVO paramVO) {
        /// 校验参数
        validCondition(paramVO);

        String cacheKey = HVAC_ELECTRICITY_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportMultiChartResultVO<Map<String, List<BigDecimal>>>>() {
            });
        }
        BaseReportResultVO<HvacElectricityInfo> tableResult = getTable(paramVO);
        BaseReportMultiChartResultVO<Map<String, List<BigDecimal>>> resultVO = new BaseReportMultiChartResultVO<>();
        // x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        resultVO.setXdata(xdata);
        resultVO.setDataTime(tableResult.getDataTime());

        LinkedHashMap<String, List<BigDecimal>> ydataListMap = new LinkedHashMap<>();
        LinkedHashMap<String, List<BigDecimal>> ydataNowListMap = new LinkedHashMap<>();
        LinkedHashMap<String, List<BigDecimal>> ydataPreListMap = new LinkedHashMap<>();
        List<HvacElectricityInfo> tableDataList = tableResult.getReportDataList();
        tableDataList.forEach(info -> {
            List<HvacElectricityInfoData> dateList = info.getHvacElectricityInfoDataList();
            Map<String, HvacElectricityInfoData> timeMap = dateList.stream()
                    .filter(data -> data.getDate() != null)
                    .collect(Collectors.toMap(
                            HvacElectricityInfoData::getDate,
                            data -> data,
                            (existing, replacement) -> replacement // 处理重复时间，保留后者
                    ));
            if (CollUtil.isEmpty(dateList)) {
                return;
            }
            List<BigDecimal> nowList = xdata.stream().map(time ->{
                    HvacElectricityInfoData infoData = timeMap.get(time);
                    if(Objects.isNull(infoData)){
                        return BigDecimal.ZERO;
                    }
                    return infoData.getNow();
                }

            ).collect(Collectors.toList());
            List<BigDecimal> preList = xdata.stream().map(time ->{
                        HvacElectricityInfoData infoData = timeMap.get(time);
                        if(Objects.isNull(infoData)){
                            return BigDecimal.ZERO;
                        }
                        return infoData.getPrevious();
                    }
            ).collect(Collectors.toList());

            ydataNowListMap.put(info.getItemName(), nowList);
            ydataPreListMap.put(info.getItemName() + "_同期", preList);
        });

        // 初始化汇总列表，长度和 xdata 一样，初始值为 0
        List<BigDecimal> sumNowList = new ArrayList<>(xdata.size());
        List<BigDecimal> sumPreList = new ArrayList<>(xdata.size());
        for (int i = 0; i < xdata.size(); i++) {
            sumNowList.add(BigDecimal.ZERO);
            sumPreList.add(BigDecimal.ZERO);
        }

        // 遍历每个 sbCode 的数据列表，逐项累加
        for (List<BigDecimal> sbDataList : ydataNowListMap.values()) {
            for (int i = 0; i < sbDataList.size(); i++) {
                sumNowList.set(i, sumNowList.get(i).add(sbDataList.get(i)));
            }
        }
        // 遍历每个 sbCode 的数据列表，逐项累加
        for (List<BigDecimal> sbDataList : ydataPreListMap.values()) {
            for (int i = 0; i < sbDataList.size(); i++) {
                sumPreList.set(i, sumPreList.get(i).add(sbDataList.get(i)));
            }
        }

        List<BigDecimal> scaledSumNowList = sumNowList.stream()
                .map(val -> dealBigDecimalScale(val, scale))
                .collect(Collectors.toList());
        List<BigDecimal> scaledSumPreList = sumPreList.stream()
                .map(val -> dealBigDecimalScale(val, scale))
                .collect(Collectors.toList());

        // 放入 map 中
        ydataListMap.put("汇总", scaledSumNowList);
        ydataListMap.put("汇总_同期", scaledSumPreList);
        ydataListMap.putAll(ydataNowListMap);
        ydataListMap.putAll(ydataPreListMap);

        resultVO.setYdata(ydataListMap);
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }

    @Override
    public List<List<String>> getExcelHeader(HvacElectricityParamVO paramVO) {

        validCondition(paramVO);

        List<List<String>> list = ListUtils.newArrayList();
        list.add(Arrays.asList("表单名称", "统计周期", "标签","标签"));
        String sheetName = "暖通电量";
        // 统计周期
        String period = getFormatTime(paramVO.getRange()[0]) + "~" + getFormatTime(paramVO.getRange()[1]);

        // 月份处理
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, period, x,"当期"));
            list.add(Arrays.asList(sheetName,  period,x, "同期"));
            list.add(Arrays.asList(sheetName,  period,x, "同比（%）"));
        });
        list.add(Arrays.asList(sheetName, period, "周期合计","当期"));
        list.add(Arrays.asList(sheetName, period, "周期合计","同期"));
        list.add(Arrays.asList(sheetName, period, "周期合计","同比（%）"));
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(HvacElectricityParamVO paramVO) {
        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();

        BaseReportResultVO<HvacElectricityInfo> resultVO = getTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<HvacElectricityInfo> hvacElectricityInfoList = resultVO.getReportDataList();
        // 底部合计map
        Map<String, BigDecimal> bottomNowSumMap = new HashMap<>();
        Map<String, BigDecimal> bottomPreSumMap = new HashMap<>();
        for (HvacElectricityInfo s : hvacElectricityInfoList) {

            List<Object> data = ListUtils.newArrayList();

            data.add(s.getItemName());

            // 处理数据
            List<HvacElectricityInfoData> hvacElectricityInfoDataList = s.getHvacElectricityInfoDataList();

            Map<String, HvacElectricityInfoData> dateMap = hvacElectricityInfoDataList.stream()
                    .collect(Collectors.toMap(HvacElectricityInfoData::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                HvacElectricityInfoData hvacElectricityInfoData = dateMap.get(date);
                if (hvacElectricityInfoData == null) {
                    data.add("/");
                    data.add("/");
                    data.add("/");
                } else {
                    data.add(getConvertData(hvacElectricityInfoData.getNow()));
                    data.add(getConvertData(hvacElectricityInfoData.getPrevious()));
                    data.add(getConvertData(hvacElectricityInfoData.getRatio()));
                    // 底部合计
                    bottomNowSumMap.put(date, addBigDecimal(bottomNowSumMap.get(date), hvacElectricityInfoData.getNow()));
                    bottomPreSumMap.put(date, addBigDecimal(bottomPreSumMap.get(date), hvacElectricityInfoData.getPrevious()));
                }
            });

            // 处理周期合计
            data.add(getConvertData(s.getPeriodNow()));
            data.add(getConvertData(s.getPeriodPrevious()));
            data.add(getConvertData(s.getPeriodRatio()));
            // 处理底部周期合计
            bottomNowSumMap.put(periodSumKey, addBigDecimal(bottomNowSumMap.get(periodSumKey), s.getPeriodNow()));
            bottomPreSumMap.put(periodSumKey, addBigDecimal(bottomPreSumMap.get(periodSumKey), s.getPeriodPrevious()));
            result.add(data);
        }
        // 添加底部合计数据
        List<Object> bottom = ListUtils.newArrayList();
        // 每日合计、每月合计，每年合计
        bottom.add(DataTypeEnum.getBottomSumCell(DataTypeEnum.codeOf(paramVO.getDateType())));
        // 底部数据位
        tableHeader.forEach(date -> {
            bottom.add(getConvertData(bottomNowSumMap.get(date)));
            bottom.add(getConvertData(bottomPreSumMap.get(date)));
            bottom.add(getConvertData(calculateYearOnYearRatio(bottomNowSumMap.get(date),bottomPreSumMap.get(date))));
        });
        // 底部周期合计
        bottom.add(getConvertData(bottomNowSumMap.get(periodSumKey)));
        bottom.add(getConvertData(bottomPreSumMap.get(periodSumKey)));
        bottom.add(getConvertData(calculateYearOnYearRatio(bottomNowSumMap.get(periodSumKey),bottomPreSumMap.get(periodSumKey))));
        result.add(bottom);
        return result;
    }


}
