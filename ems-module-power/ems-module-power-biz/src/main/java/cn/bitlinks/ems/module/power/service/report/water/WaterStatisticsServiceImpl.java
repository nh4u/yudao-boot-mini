package cn.bitlinks.ems.module.power.service.report.water;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartYInfo;
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
import cn.bitlinks.ems.module.power.service.statistics.StatisticsCommonService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrSplitter;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.dealStrTime;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.STATISTICS_FEE;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.*;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * @author liumingqiang
 */
@Slf4j
@Service
@Validated
public class WaterStatisticsServiceImpl implements WaterStatisticsService {

    @Resource
    private EnergyConfigurationService energyConfigurationService;
    @Resource
    private EnergyGroupService energyGroupService;
    @Resource
    private LabelConfigService labelConfigService;
    @Resource
    private StatisticsCommonService statisticsCommonService;
    @Resource
    private UsageCostService usageCostService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    private Integer scale = DEFAULT_SCALE;

    @Override
    public StatisticsResultV2VO<StatisticsInfoV2> waterStatisticsTable(StatisticsParamV2VO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());

        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = WATER_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            // 泛型放缓存避免强转问题
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsResultV2VO<StatisticsInfoV2>>() {
            });

        }

        // 4.如果没有则去数据库查询
        StatisticsResultV2VO<StatisticsInfoV2> resultVO = new StatisticsResultV2VO<>();
        resultVO.setDataTime(LocalDateTime.now());

        // 4.1.表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultVO.setHeader(tableHeader);

        // 4.2.能源id处理
        List<EnergyConfigurationDO> energyList;
        List<Long> energyIdsParam = paramVO.getEnergyIds();
        if (CollUtil.isNotEmpty(energyIdsParam)) {
            energyList = energyConfigurationService.getPureByEnergyClassify(new HashSet<>(energyIdsParam), null);
        } else {
            // 获取所有水组合能源
            EnergyGroupDO energyGroup = energyGroupService.getEnergyGroup(GROUP_WATER);
            if (Objects.isNull(energyGroup)) {
                throw exception(ENERGY_GROUP_NOT_EXISTS);
            }
            energyList = energyConfigurationService
                    .getByEnergyGroup(energyGroup.getId());
        }

        List<Long> energyIds = energyList
                .stream()
                .map(EnergyConfigurationDO::getId)
                .collect(Collectors.toList());

        // 4.3.台账id处理
        List<Long> standingBookIds = new ArrayList<>();
        // 4.3.1.根据能源id查询台账
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIdList = standingbookIdsByEnergy
                .stream()
                .map(StandingbookDO::getId)
                .collect(Collectors.toList());

        // 4.3.2.根据标签id查询
        String topLabel = paramVO.getTopLabel();
        String childLabels = paramVO.getChildLabels();
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService
                .getStandingbookIdsByLabel(topLabel, childLabels);

        // 4.3.3.能源台账ids和标签台账ids是否有交集。如果有就取交集，如果没有则取能源台账ids
        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel
                    .stream()
                    .map(StandingbookLabelInfoDO::getStandingbookId)
                    .collect(Collectors.toList());

            // 取标签台账和能源台账之间的交集
            List<StandingbookDO> collect = standingbookIdsByEnergy
                    .stream()
                    .filter(s -> sids.contains(s.getId()))
                    .collect(Collectors.toList());

            // 能源关联计量器具，标签可能关联重点设备，当不存在交集时，则无需查询
            if (ArrayUtil.isEmpty(collect)) {
                return resultVO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            standingBookIds.addAll(standingBookIdList);
        }

        // 4.4.台账id为空直接返回结果
        if (CollUtil.isEmpty(standingBookIds)) {
            return resultVO;
        }

        // 4.5.根据台账和其他条件从数据库里拿出折标煤数据
        // 4.5.1.根据台账ID查询用量和折标煤
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                paramVO,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);

        if (CollUtil.isEmpty(usageCostDataList)) {
            return resultVO;
        }

        // 0、综合查看（默认）
        List<StatisticsInfoV2> statisticsInfoList = queryDefault(topLabel, childLabels, standingbookIdsByLabel, usageCostDataList);
        resultVO.setStatisticsInfoList(statisticsInfoList);

        // 无数据的填充0
        statisticsInfoList.forEach(l -> {

            List<StatisticInfoDataV2> newList = new ArrayList<>();
            List<StatisticInfoDataV2> oldList = l.getStatisticsDateDataList();
            if (tableHeader.size() != oldList.size()) {
                Map<String, List<StatisticInfoDataV2>> dateMap = oldList.stream()
                        .collect(Collectors.groupingBy(StatisticInfoDataV2::getDate));

                tableHeader.forEach(date -> {
                    List<StatisticInfoDataV2> standardCoalInfoDataList = dateMap.get(date);
                    if (standardCoalInfoDataList == null) {
                        StatisticInfoDataV2 standardCoalInfoData = new StatisticInfoDataV2();
                        standardCoalInfoData.setDate(date);
                        standardCoalInfoData.setConsumption(BigDecimal.ZERO);
                        newList.add(standardCoalInfoData);
                    } else {
                        newList.add(standardCoalInfoDataList.get(0));
                    }
                });

                // 设置新数据list
                l.setStatisticsDateDataList(newList);
            }

        });


        // 获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTime(
                paramVO,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);
        resultVO.setDataTime(lastTime);

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        // 返回查询结果。
        return resultVO;
    }
    public List<StatisticsInfoV2> queryDefault(String topLabel,
                                               String childLabels,
                                               List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                               List<UsageCostData> usageCostDataList) {

        // 实际用到的能源ids
        Set<Long> energyIdSet = usageCostDataList
                .stream()
                .map(UsageCostData::getEnergyId)
                .collect(Collectors.toSet());

        // 获取实际用到的能源实体
        List<EnergyConfigurationDO> energyList = energyConfigurationService
                .getByEnergyClassify(energyIdSet, null);

        // 能源list转换成map
        Map<Long, EnergyConfigurationDO> energyMap = energyList
                .stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        // 标签list转换成map
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        if (CharSequenceUtil.isNotBlank(topLabel) && CharSequenceUtil.isBlank(childLabels)) {
            // 只有顶级标签
            return queryDefaultTopLabel(standingBookUsageMap, labelMap, standingbookIdsByLabel, energyMap);
        } else {
            // 有顶级、有子集标签
            return queryDefaultSubLabel(standingBookUsageMap, labelMap, standingbookIdsByLabel, energyMap);
        }
    }

    public List<StatisticsInfoV2> queryDefaultTopLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                       Map<Long, LabelConfigDO> labelMap,
                                                       List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                       Map<Long, EnergyConfigurationDO> energyMap) {

        List<StatisticsInfoV2> resultList = new ArrayList<>();

        List<UsageCostData> labelUsageCostDataList = new ArrayList<>();

        // 获取标签关联的台账id，并取到对应的数据
        standingbookIdsByLabel.forEach(labelInfo -> {
            List<UsageCostData> usageList = standingBookUsageMap.get(labelInfo.getStandingbookId());
            if (usageList == null || usageList.isEmpty()) {
                return; // 计量器具没有数据，跳过
            }
            labelUsageCostDataList.addAll(usageList);
        });

        Map<Long, List<UsageCostData>> energyUsageCostMap = labelUsageCostDataList
                .stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        energyUsageCostMap.forEach((energyId, usageCostList) -> {
            EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);
            // 由于数采数据是按 台账 日期能源进行分组的 而一个标签关联多个台账，那么标签同一个日期就会有多条不同台账的数据，所以要按日期进行合并
            // 聚合数据 转换成 StandardCoalInfoData
            List<StatisticInfoDataV2> dataList = new ArrayList<>(usageCostList.stream().collect(Collectors.groupingBy(
                    UsageCostData::getTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                BigDecimal totalConsumption = list.stream()
                                        .map(UsageCostData::getCurrentTotalUsage)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, null);
                            }
                    )
            )).values());

            BigDecimal totalConsumption = dataList.stream()
                    .map(StatisticInfoDataV2::getConsumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            StatisticsInfoV2 info = new StatisticsInfoV2();
            info.setEnergyId(energyId);
            info.setEnergyName(energyConfigurationDO.getEnergyName());

            StandingbookLabelInfoDO standingbookLabelInfoDO = standingbookIdsByLabel.get(0);
            String topLabelKey = standingbookLabelInfoDO.getName();
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);

            info.setLabel1(topLabel.getLabelName());
            info.setLabel2("/");
            info.setLabel3("/");
            info.setLabel4("/");
            info.setLabel5("/");

            dataList = dataList.stream().peek(i -> {
                i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
            }).collect(Collectors.toList());

            info.setStatisticsDateDataList(dataList);
            info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, scale));

            resultList.add(info);
        });

        return resultList;
    }

    public List<StatisticsInfoV2> queryDefaultSubLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                       Map<Long, LabelConfigDO> labelMap,
                                                       List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                       Map<Long, EnergyConfigurationDO> energyMap) {
        // 标签查询条件处理
        //根据能源ID分组
        // 使用 Collectors.groupingBy 根据 name 和 value 分组
        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        // 第一个分组条件：按 name
                        StandingbookLabelInfoDO::getName,
                        // 第二个分组条件：按 value
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)
                ));

        List<StatisticsInfoV2> resultList = new ArrayList<>();

        grouped.forEach((topLabelKey, labelInfoGroup) -> {
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);
            if (topLabel == null) {
                return; // 如果一级标签不存在，跳过
            }
            labelInfoGroup.forEach((valueKey, labelInfoList) -> {
                String[] labelIds = valueKey.split(",");
                String label2Name = getLabelName(labelMap, labelIds, 0);
                String label3Name = labelIds.length > 1 ? getLabelName(labelMap, labelIds, 1) : "/";
                String label4Name = labelIds.length > 2 ? getLabelName(labelMap, labelIds, 2) : "/";
                String label5Name = labelIds.length > 3 ? getLabelName(labelMap, labelIds, 3) : "/";

                List<UsageCostData> labelUsageCostDataList = new ArrayList<>();

                // 获取标签关联的台账id，并取到对应的数据
                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = standingBookUsageMap.get(labelInfo.getStandingbookId());
                    if (usageList == null || usageList.isEmpty()) {
                        return; // 计量器具没有数据，跳过
                    }
                    labelUsageCostDataList.addAll(usageList);
                });

                Map<Long, List<UsageCostData>> energyUsageCostMap = labelUsageCostDataList
                        .stream()
                        .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

                energyUsageCostMap.forEach((energyId, usageCostList) -> {
                    EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);

                    // 因为数采数据按台账id合并并包含多个时间  但一个标签关联了多个台账 所以还需要按照日期再聚合一下 要重新考虑一下
                    //  usageCostData数据按能源、台账进行分组了，现在的分组方式 会导致用量数据无法累加。
                    // 聚合数据 转换成 StandardCoalInfoData
                    List<StatisticInfoDataV2> dataList = new ArrayList<>(usageCostList.stream().collect(Collectors.groupingBy(
                            UsageCostData::getTime,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        BigDecimal totalConsumption = list.stream()
                                                .map(UsageCostData::getCurrentTotalUsage)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, null);
                                    }
                            )
                    )).values());

                    BigDecimal totalConsumption = dataList.stream()
                            .map(StatisticInfoDataV2::getConsumption)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    StatisticsInfoV2 info = new StatisticsInfoV2();
                    info.setEnergyId(energyId);
                    info.setEnergyName(energyConfigurationDO.getEnergyName());
                    info.setLabel1(topLabel.getLabelName());
                    info.setLabel2(label2Name);
                    info.setLabel3(label3Name);
                    info.setLabel4(label4Name);
                    info.setLabel5(label5Name);

                    dataList = dataList.stream().peek(i -> {
                        i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
                    }).collect(Collectors.toList());

                    info.setStatisticsDateDataList(dataList);
                    info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, scale));

                    resultList.add(info);
                });
            });
        });

        return resultList;
    }

    @Override
    public  FeeChartResultVO<FeeChartYInfo>  waterStatisticsChart(StatisticsParamV2VO paramVO) {

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = WATER_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            // 泛型放缓存避免强转问题
            return JSON.parseObject(cacheRes, new TypeReference<FeeChartResultVO<FeeChartYInfo>>() {
            });
        }

        // 4.如果没有则去数据库查询
        FeeChartResultVO<FeeChartYInfo> resultVO = new FeeChartResultVO<>();
        resultVO.setDataTime(LocalDateTime.now());

        StatisticsResultV2VO<StatisticsInfoV2> resultTable = waterStatisticsTable(paramVO);
        // x轴
        List<String> xdata = resultTable.getHeader();
        resultVO.setXdata(xdata);

        List<StatisticsInfoV2> statisticsInfoList = resultTable.getStatisticsInfoList();

        // 底部合计map
        Map<String, BigDecimal> sumConsumptionMap = new HashMap<>();
        List<FeeChartYInfo> yInfoList = new ArrayList<>();
        for (StatisticsInfoV2 s : statisticsInfoList) {

            FeeChartYInfo yInfo = new FeeChartYInfo();
            yInfo.setName(getName(s.getLabel1(), s.getLabel2(), s.getLabel3(), s.getLabel4(), s.getLabel5()));

            // 处理数据
            List<StatisticInfoDataV2> statisticInfoDataV2List = s.getStatisticsDateDataList();
            Map<String, StatisticInfoDataV2> dateMap = statisticInfoDataV2List.stream()
                    .collect(Collectors.toMap(StatisticInfoDataV2::getDate, Function.identity()));

            List<BigDecimal> data = ListUtils.newArrayList();
            xdata.forEach(date -> {
                StatisticInfoDataV2 statisticInfoDataV2 = dateMap.get(date);
                if (statisticInfoDataV2 == null) {
                    data.add(BigDecimal.ZERO);
                } else {
                    BigDecimal consumption = statisticInfoDataV2.getConsumption();
                    data.add(!Objects.isNull(consumption) ? consumption : BigDecimal.ZERO);
                    // 底部合计处理
                    sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), consumption));
                }
            });
            yInfo.setData(data);

            // 处理底部合计
            BigDecimal sumConsumption = s.getSumEnergyConsumption();
            sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sumConsumption));

            yInfoList.add(yInfo);
        }

        // 汇总数据
        List<BigDecimal> summary = ListUtils.newArrayList();
        FeeChartYInfo yInfo = new FeeChartYInfo();
        yInfo.setName("汇总");
        xdata.forEach(date -> {
            // 折价
            BigDecimal consumption = sumConsumptionMap.get(date);
            summary.add(!Objects.isNull(consumption) ? consumption : BigDecimal.ZERO);
        });
        yInfo.setData(summary);

        yInfoList.add(yInfo);
        resultVO.setYdata(yInfoList);

        return resultVO;
    }



    @Override
    public List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO) {
        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];
        // 表头数据
        List<List<String>> list = ListUtils.newArrayList();
        // 统计周期
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);

        // 统计标签
        String topLabel = paramVO.getTopLabel();
        String childLabels = paramVO.getChildLabels();
        String labelName = getLabelName(topLabel, childLabels);
        Integer labelDeep = getLabelDeep(childLabels);
        // 表单名称
        // 综合
        String sheetName = STATISTICS_FEE;
        list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签"));
        for (int i = 2; i <= labelDeep; i++) {
            String subLabel = "标签" + i;
            list.add(Arrays.asList(sheetName, labelName, strTime, subLabel));
        }
        list.add(Arrays.asList(sheetName, labelName, strTime, "能源"));
        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, labelName, strTime, x));
        });

        // 周期合计
        list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计"));
        return list;
    }

    private String getLabelName(String topLabel, String childLabels) {

        // 一级标签
        Long topLabelId = Long.valueOf(topLabel.substring(topLabel.indexOf("_") + 1));

        // 下级标签
        List<String> childLabelValues = StrSplitter.split(childLabels, "#", 0, true, true);
        List<Long> labelIds = childLabelValues.stream()
                .map(c -> StrSplitter.split(c, ",", 0, true, true))
                .flatMap(List::stream)
                .map(Long::valueOf)
                .distinct()
                .collect(Collectors.toList());

        labelIds.add(topLabelId);

        // 获取标签数据
        List<LabelConfigDO> labels = labelConfigService.getByIds(labelIds);

        return labels.stream().map(LabelConfigDO::getLabelName).collect(Collectors.joining("、"));
    }

    @Override
    public List<List<Object>> getExcelData(StatisticsParamV2VO paramVO) {

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        StatisticsResultV2VO<StatisticsInfoV2> resultVO = waterStatisticsTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<StatisticsInfoV2> statisticsInfoList = resultVO.getStatisticsInfoList();
        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);

        // 底部合计map
        Map<String, BigDecimal> sumConsumptionMap = new HashMap<>();

        for (StatisticsInfoV2 s : statisticsInfoList) {

            List<Object> data = ListUtils.newArrayList();
            String[] labels = {s.getLabel1(), s.getLabel2(), s.getLabel3(), s.getLabel4(), s.getLabel5()};

            // 综合
            for (int i = 0; i < labelDeep; i++) {
                data.add(labels[i]);
            }
            // 处理能源
            data.add(s.getEnergyName());
            // 处理数据
            List<StatisticInfoDataV2> statisticInfoDataV2List = s.getStatisticsDateDataList();

            Map<String, StatisticInfoDataV2> dateMap = statisticInfoDataV2List.stream()
                    .collect(Collectors.toMap(StatisticInfoDataV2::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                StatisticInfoDataV2 statisticInfoDataV2 = dateMap.get(date);
                if (statisticInfoDataV2 == null) {
                    data.add("/");
                } else {
                    BigDecimal consumption = statisticInfoDataV2.getConsumption();
                    data.add(getConvertData(consumption));

                    // 底部合计处理
                    sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), consumption));
                }
            });

            BigDecimal sumEnergyConsumption = s.getSumEnergyConsumption();
            // 处理周期合计
            data.add(getConvertData(sumEnergyConsumption));

            // 处理底部合计
            sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sumEnergyConsumption));

            result.add(data);
        }

        // 添加底部合计数据
        List<Object> bottom = ListUtils.newArrayList();
        // "时间类型 0：日；1：月；2：年；3：时。
        String pre = "";
        Integer dateType = paramVO.getDateType();
        switch (dateType) {
            case 0:
                pre = DAILY_STATISTICS;
                break;
            case 1:
                pre = MONTHLY_STATISTICS;
                break;
            case 2:
                pre = ANNUAL_STATISTICS;
                break;
            default:
                break;
        }

        // 按标签
        for (int i = 0; i < labelDeep; i++) {
            bottom.add(pre);
        }

        // 能源
        bottom.add(pre);

        // 底部数据位
        tableHeader.forEach(date -> {
            // 用量
            BigDecimal consumption = sumConsumptionMap.get(date);
            bottom.add(getConvertData(consumption));
        });

        // 底部周期合计
        // 用量
        BigDecimal consumption = sumConsumptionMap.get("sumNum");
        bottom.add(getConvertData(consumption));
        result.add(bottom);

        return result;
    }

    private String getLabelName(Map<Long, LabelConfigDO> labelMap, String[] labelIds, int index) {
        if (index < labelIds.length) {
            LabelConfigDO label = labelMap.get(Long.valueOf(labelIds[index]));
            if (label != null) {
                return label.getLabelName();
            }
        }
        return "/";
    }

    /**
     * 校验时间范围
     *
     * @param rangeOrigin
     * @return
     */
    private LocalDateTime[] validateRange(LocalDateTime[] rangeOrigin) {
        // 1.校验时间范围
        // 1.1.校验结束时间必须大于开始时间
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        // 时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        return rangeOrigin;
    }

    /**
     * 校验时间类型
     *
     * @param dateType
     */
    private DataTypeEnum validateDateType(Integer dateType) {
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(dateType);
        // 时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        return dataTypeEnum;
    }


}
