package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energygroup.EnergyGroupDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.energygroup.EnergyGroupService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsCommonService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ArrayUtil;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.REPORT_HVAC_ELECTRICITY;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.HVAC_ELECTRICITY_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

@Service
@Validated
@Slf4j
public class HvacElectricityServiceImpl implements HvacElectricityService {
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Resource
    private S standingbookService;

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

    /**
     * 报表统计标签，存放到字典中。
     *
     * @return
     */
    private LinkedHashMap<String, String> getItemMapping() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        List<String> gasSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_HVAC_ELECTRICITY);
        for (String label : gasSbLabels) {
            String sbCode = DictFrameworkUtils.parseDictDataValue(REPORT_HVAC_ELECTRICITY, label);
            result.put(label, sbCode);
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
    public BaseReportResultVO<HvacElectricityInfo> getTable(BaseTimeDateParamVO paramVO) {
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
        LinkedHashMap<String, String> itemMapping = getItemMapping();

//        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();
//
//
//        Map<String, Long> sbMapping = allStandingbookDTOList.stream()
//                .filter(dto -> itemMapping.containsValue(dto.getCode()))
//                .collect(Collectors.toMap(
//                        StandingbookDTO::getCode,
//                        StandingbookDTO::getStandingbookId
//                ));
//        // 查询不到台账信息,返回空
//        if (CollUtil.isEmpty(sbMapping)) {
//            return defaultNullData(itemMapping, tableHeader);
//        }

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
//        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        // 查询标签信息（按标签过滤台账）
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService
                .getStandingbookIdsByDefaultLabel(new ArrayList<>(itemMapping.values()));

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

        // 查询上一年周期折扣数据
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        List<HvacElectricityInfo> hvacElectricityInfos = new ArrayList<>(queryByDefaultLabel(
                standingbookIdsByLabel,
                usageCostDataList,
                lastYearUsageCostDataList,
                labelMap
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
                                            Map<Long, LabelConfigDO> labelMap
                                            ) {
        //以value 分组 台账id
        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        StandingbookLabelInfoDO::getName,
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));

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


        List<YoyItemVO> resultList = new ArrayList<>();

        // 遍历一级标签
        grouped.forEach((topLabelKey, labelInfoGroup) -> {
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);
            if (topLabel == null) return;
//
//            // 遍历二级标签组合
            labelInfoGroup.forEach((valueKey, labelInfoList) -> {
                String[] labelIds = valueKey.split(",");
//                String label2Name = getLabelName(labelMap, labelIds, 0);
//                String label3Name = labelIds.length > 1 ? getLabelName(labelMap, labelIds, 1) : "/";
//                String label4Name = labelIds.length > 2 ? getLabelName(labelMap, labelIds, 2) : "/";
//                String label5Name = labelIds.length > 3 ? getLabelName(labelMap, labelIds, 3) : "/";

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
                    String previousTime = LocalDateTimeUtils.getYearOnYearTime(u.getTime(), dateTypeEnum);
                    String key = u.getStandingbookId() + "_" + u.getTime();
                    UsageCostData previous = lastMap.get(key);
                    if (Objects.isNull(previous)) {
                        return; // 计量器具没有数据，跳过
                    }
                    labelUsageListPrevious.add(previous);
                });

                // 1.处理当前
                List<TimeAndNumData> nowList = getTimeAndNumDataList(labelUsageListNow, valueExtractor);

                // 2.处理上期
                Map<String, TimeAndNumData> previousMap = getTimeAndNumDataMap(labelUsageListPrevious, valueExtractor);

                // 构造同比详情列表
                List<YoyDetailVO> dataList = nowList.stream()
                        .map(current -> {
                            String previousTime = LocalDateTimeUtils.getYearOnYearTime(current.getTime(), dateTypeEnum);
                            TimeAndNumData previous = previousMap.get(previousTime);
                            BigDecimal now = Optional.ofNullable(current.getNum()).orElse(BigDecimal.ZERO);
                            BigDecimal last = previous != null ? Optional.ofNullable(previous.getNum()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
                            BigDecimal ratio = calculateYearOnYearRatio(now, last);
                            return new YoyDetailVO(current.getTime(), now, last, ratio);
                        })
                        .sorted(Comparator.comparing(YoyDetailVO::getDate))
                        .collect(Collectors.toList());

                // 汇总统计
                BigDecimal sumNow = dataList.stream().map(YoyDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal sumPrevious = dataList.stream().map(YoyDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal sumRatio = calculateYearOnYearRatio(sumNow, sumPrevious);

                // 构造结果对象
                YoyItemVO info = new YoyItemVO();
                info.setLabel1(topLabel.getLabelName());
                info.setLabel2(label2Name);
                info.setLabel3(label3Name);
                info.setLabel4(label4Name);
                info.setLabel5(label5Name);

                dataList = dataList.stream().peek(i -> {
                    i.setNow(dealBigDecimalScale(i.getNow(), DEFAULT_SCALE));
                    i.setPrevious(dealBigDecimalScale(i.getPrevious(), DEFAULT_SCALE));
                    i.setRatio(dealBigDecimalScale(i.getRatio(), DEFAULT_SCALE));
                }).collect(Collectors.toList());
                info.setStatisticsRatioDataList(dataList);

                info.setSumNow(dealBigDecimalScale(sumNow, DEFAULT_SCALE));
                info.setSumPrevious(dealBigDecimalScale(sumPrevious, DEFAULT_SCALE));
                info.setSumRatio(dealBigDecimalScale(sumRatio, DEFAULT_SCALE));

                resultList.add(info);
            });
        });

        return resultList;
    }

//
//    @Override
//    public BaseReportMultiChartResultVO<Map<String,List<BigDecimal>>> getChart(BaseTimeDateParamVO paramVO) {
//        // 校验参数
//        validCondition(paramVO);
//
//        String cacheKey = HVAC_ELECTRICITY_CHART + SecureUtil.md5(paramVO.toString());
//        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
//        String cacheRes = StrUtils.decompressGzip(compressed);
//        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
//            return JSON.parseObject(cacheRes, new TypeReference<BaseReportMultiChartResultVO<Map<String,List<BigDecimal>>>>() {
//            });
//        }
//        BaseReportMultiChartResultVO<Map<String,List<BigDecimal>>> resultVO = new BaseReportMultiChartResultVO<>();
//        // x轴
//        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
//        resultVO.setXdata(xdata);
//
//
//        LinkedHashMap<String, String> itemMapping = getItemMapping();
//
//        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();
//
//        Map<String, Long> sbMapping = allStandingbookDTOList.stream()
//                .filter(dto -> itemMapping.containsValue(dto.getCode()))
//                .collect(Collectors.toMap(
//                        StandingbookDTO::getCode,
//                        StandingbookDTO::getStandingbookId
//                ));
//
//
//        if (CollUtil.isEmpty(sbMapping)) {
//            resultVO.setDataTime(LocalDateTime.now());
//            resultVO.setYdata(Collections.emptyMap());
//            return resultVO;
//        }
//
//        // 查询 热力计量器具对应的用量使用情况；
//        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], new ArrayList<>(sbMapping.values()));
//
//        if (CollUtil.isEmpty(usageCostDataList)) {
//            resultVO.setDataTime(LocalDateTime.now());
//            resultVO.setYdata(Collections.emptyMap());
//            return resultVO;
//        }
//        Map<Long, Map<String, BigDecimal>> standingbookIdTimeCostMap = usageCostDataList.stream()
//                .collect(Collectors.groupingBy(
//                        UsageCostData::getStandingbookId,
//                        Collectors.toMap(
//                                UsageCostData::getTime,
//                                UsageCostData::getCurrentTotalUsage
//                        )
//                ));
//        Map<String, List<BigDecimal>> ydataListMap = new HashMap<>();
//        // 获取每个统计项的数据
//        itemMapping.values().forEach(sbCode -> {
//            Map<String, BigDecimal> sbCodeMap = standingbookIdTimeCostMap.get(sbMapping.get(sbCode));
//            if (CollUtil.isEmpty(sbCodeMap)) {
//                sbCodeMap = new HashMap<>();
//            }
//            Map<String, BigDecimal> finalSbCodeMap = sbCodeMap;
//            List<BigDecimal> sbDataList = xdata.stream().map(time -> {
//                time = dealStrTime(time);
//                return dealBigDecimalScale(finalSbCodeMap.getOrDefault(time, BigDecimal.ZERO), scale);
//            }).collect(Collectors.toList());
//            ydataListMap.put(sbCode, sbDataList);
//        });
//        // 初始化汇总列表，长度和 xdata 一样，初始值为 0
//        List<BigDecimal> sumList = new ArrayList<>(xdata.size());
//        for (int i = 0; i < xdata.size(); i++) {
//            sumList.add(BigDecimal.ZERO);
//        }
//
//        // 遍历每个 sbCode 的数据列表，逐项累加
//        for (List<BigDecimal> sbDataList : ydataListMap.values()) {
//            for (int i = 0; i < sbDataList.size(); i++) {
//                sumList.set(i, sumList.get(i).add(sbDataList.get(i)));
//            }
//        }
//
//        List<BigDecimal> scaledSumList = sumList.stream()
//                .map(val -> dealBigDecimalScale(val, scale))
//                .collect(Collectors.toList());
//
//        // 放入 map 中
//        ydataListMap.put("汇总", scaledSumList);
//
//        LinkedHashMap<String,List<BigDecimal>> map = new LinkedHashMap<>();
//        map.put("汇总",ydataListMap.get("汇总"));
//        itemMapping.forEach((k,v)->{
//            map.put(k,ydataListMap.get(v));
//        });
//
//        resultVO.setYdata(map);
//
//        LocalDateTime lastTime = getLastTime(paramVO.getRange()[0], paramVO.getRange()[1], new ArrayList<>(sbMapping.values()));
//        resultVO.setDataTime(lastTime);
//        String jsonStr = JSONUtil.toJsonStr(resultVO);
//        byte[] bytes = StrUtils.compressGzip(jsonStr);
//        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
//        return resultVO;
//    }
//
//    @Override
//    public List<List<String>> getExcelHeader(BaseTimeDateParamVO paramVO) {
//
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
//    public List<List<Object>> getExcelData(BaseTimeDateParamVO paramVO) {
//        // 结果list
//        List<List<Object>> result = ListUtils.newArrayList();
//
//        BaseReportResultVO<NaturalGasInfo> resultVO = getTable(paramVO);
//        List<String> tableHeader = resultVO.getHeader();
//
//        List<NaturalGasInfo> NaturalGasInfoList = resultVO.getReportDataList();
//
//        for (NaturalGasInfo s : NaturalGasInfoList) {
//
//            List<Object> data = ListUtils.newArrayList();
//
//            data.add(s.getItemName());
//
//            // 处理数据
//            List<NaturalGasInfoData> NaturalGasInfoDataList = s.getNaturalGasInfoDataList();
//
//            Map<String, NaturalGasInfoData> dateMap = NaturalGasInfoDataList.stream()
//                    .collect(Collectors.toMap(NaturalGasInfoData::getDate, Function.identity()));
//
//            tableHeader.forEach(date -> {
//                NaturalGasInfoData NaturalGasInfoData = dateMap.get(date);
//                if (NaturalGasInfoData == null) {
//                    data.add("/");
//                } else {
//                    BigDecimal consumption = NaturalGasInfoData.getConsumption();
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


    private LocalDateTime getLastTime(LocalDateTime start, LocalDateTime end, List<Long> standingbookIds) {
        return usageCostService.getLastTimeNoParam(start, end, standingbookIds);
    }


    private List<NaturalGasInfo> queryDefaultData(List<UsageCostData> usageCostDataList, Map<String, Long> sbMapping, LinkedHashMap<String, String> itemMapping) {
        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        List<NaturalGasInfo> resultList = new ArrayList<>();
        // 循环统计项
        itemMapping.forEach((itemName, sbCode) -> {
            NaturalGasInfo info = new NaturalGasInfo();
            info.setItemName(itemName);
            if (CollUtil.isEmpty(standingBookUsageMap)) {
                info.setNaturalGasInfoDataList(Collections.emptyList());
                resultList.add(info);
                return;
            }
            List<UsageCostData> usageCostList = standingBookUsageMap.get(sbMapping.get(sbCode));
            if (CollUtil.isEmpty(usageCostList)) {
                info.setNaturalGasInfoDataList(Collections.emptyList());
                resultList.add(info);
                return;
            }
            // 聚合数据 转换成 NaturalGasInfoData
            List<NaturalGasInfoData> dataList = new ArrayList<>(usageCostList.stream().collect(Collectors.groupingBy(
                    UsageCostData::getTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                BigDecimal totalConsumption = list.stream()
                                        .map(UsageCostData::getCurrentTotalUsage)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new NaturalGasInfoData(list.get(0).getTime(), totalConsumption);
                            }
                    )
            )).values());

            BigDecimal totalConsumption = dataList.stream()
                    .map(NaturalGasInfoData::getConsumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            dataList = dataList.stream().peek(i -> {
                i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
            }).collect(Collectors.toList());
            info.setNaturalGasInfoDataList(dataList);
            info.setPeriodSum(dealBigDecimalScale(totalConsumption, scale));

            resultList.add(info);


        });

        return resultList;
    }
}
