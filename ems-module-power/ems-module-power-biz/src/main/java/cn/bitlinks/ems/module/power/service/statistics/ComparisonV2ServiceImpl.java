package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.EnergyClassifyEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.DateUtils;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.enums.ChartSeriesTypeEnum;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingBookStageEnum;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.*;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.COMPARISON_DISCOUNT_CHART_UTILIZATION_RATE;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.COMPARISON_DISCOUNT_TABLE_UTILIZATION_RATE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
@Slf4j
public class ComparisonV2ServiceImpl implements ComparisonV2Service {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StatisticsCommonService statisticsCommonService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    public static final String NOW = "当期";
    public static final String PREVIOUS = "上期";
    public static final String RATIO = "环比";
    public static final String RATIO_PERCENT = "环比(%)";

    public static final String DEFAULT_GROUP_NAME = "总";

    @Override
    public StatisticsResultV2VO<ComparisonItemVO> discountAnalysisTable(StatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalCost, StatisticsCacheConstants.COMPARISON_DISCOUNT_TABLE_COST);
    }

    @Override
    public StatisticsResultV2VO<ComparisonItemVO> foldCoalAnalysisTable(StatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalStandardCoalEquivalent, StatisticsCacheConstants.COMPARISON_DISCOUNT_TABLE_COAL);
    }

    public StatisticsResultV2VO<ComparisonItemVO> analysisTable(StatisticsParamV2VO paramVO,
                                                                Function<UsageCostData, BigDecimal> valueExtractor, String commonType) {
        // 1.校验时间范围合法性
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        String cacheKey = commonType + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsResultV2VO<ComparisonItemVO>>() {
            });
        }

        // 构建表头
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        StatisticsResultV2VO<ComparisonItemVO> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);

        // 查询能源信息
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getPureByEnergyClassify(new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        if (CollUtil.isEmpty(energyList)) {
            resultVO.setStatisticsInfoList(Collections.emptyList());
            return resultVO;
        }
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 查询台账信息（先按能源）
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        // 查询标签信息（按标签过滤台账）
        String topLabel = paramVO.getTopLabel();
        String childLabels = paramVO.getChildLabels();
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService
                .getStandingbookIdsByLabel(topLabel, childLabels);

        List<Long> standingBookIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream().filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            if (ArrayUtil.isEmpty(collect)) {
                resultVO.setStatisticsInfoList(Collections.emptyList());
                return resultVO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            // 如果标签为空则使用能源台账全量
            standingBookIds.addAll(standingBookIdList);
        }

        // 无台账数据直接返回
        if (CollUtil.isEmpty(standingBookIds)) {
            resultVO.setStatisticsInfoList(Collections.emptyList());
            return resultVO;
        }

        // 查询当前周期折扣数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, startTime, endTime, standingBookIds);

        // 查询上一个周期折扣数据
        LocalDateTime[] lastRange = LocalDateTimeUtils.getPreviousRangeV2(rangeOrigin, dataTypeEnum);

        List<UsageCostData> lastUsageCostDataList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], standingBookIds);

        // 用于合计 所取的数据
        LocalDateTime[] totalRange = LocalDateTimeUtils.getPreviousRange(rangeOrigin, DataTypeEnum.DAY);
        List<UsageCostData> totalUsageCostDataList = usageCostService.getList(paramVO, totalRange[0], totalRange[1], standingBookIds);


        List<ComparisonItemVO> statisticsInfoList = new ArrayList<>();
        boolean isCrossYear = DateUtils.isCrossYear(paramVO.getRange()[0], paramVO.getRange()[1]);

        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // 按能源查看，无需构建标签分组
            statisticsInfoList.addAll(queryByEnergy(
                    energyList,
                    usageCostDataList,
                    lastUsageCostDataList,
                    totalUsageCostDataList,
                    dataTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor));
        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 按标签查看
            Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                    .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

            if (CharSequenceUtil.isNotBlank(topLabel) && CharSequenceUtil.isBlank(childLabels)) {
                // 只有顶级标签
                statisticsInfoList.addAll(queryByTopLabel(
                        usageCostDataList,
                        lastUsageCostDataList,
                        totalUsageCostDataList,
                        labelMap,
                        topLabel,
                        dataTypeEnum,
                        tableHeader,
                        isCrossYear,
                        valueExtractor));
            } else {
                // 有顶级、有子集标签
                statisticsInfoList.addAll(queryBySubLabel(
                        standingbookIdsByLabel,
                        usageCostDataList,
                        lastUsageCostDataList,
                        totalUsageCostDataList,
                        labelMap,
                        dataTypeEnum,
                        tableHeader,
                        isCrossYear,
                        valueExtractor));
            }
        } else {
            // 综合默认查看
            statisticsInfoList.addAll(queryDefault(
                    topLabel,
                    childLabels,
                    standingbookIdsByLabel,
                    usageCostDataList,
                    lastUsageCostDataList,
                    totalUsageCostDataList,
                    dataTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor));
        }


        // 设置最终返回值
        resultVO.setStatisticsInfoList(statisticsInfoList);
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
        return resultVO;
    }


    /**
     * 按能源维度统计：以 energyId 为主键，构建环比统计数据
     */
    private List<ComparisonItemVO> queryByEnergy(List<EnergyConfigurationDO> energyList,
                                                 List<UsageCostData> usageCostDataList,
                                                 List<UsageCostData> lastUsageCostDataList,
                                                 List<UsageCostData> totalUsageCostDataList,
                                                 DataTypeEnum dataTypeEnum,
                                                 List<String> tableHeader,
                                                 boolean isCrossYear,
                                                 Function<UsageCostData, BigDecimal> valueExtractor) {
        // 按能源ID分组当前周期数据
        Map<Long, List<UsageCostData>> nowUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
        Map<Long, List<UsageCostData>> lastUsageMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
        Map<Long, List<UsageCostData>> totalUsageMap = totalUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        List<ComparisonItemVO> detailList = new ArrayList<>();

        energyList.forEach(energy -> {
            List<UsageCostData> nowList = nowUsageMap.get(energy.getId());
            List<UsageCostData> lastList = lastUsageMap.get(energy.getId());
            List<UsageCostData> totalList = totalUsageMap.get(energy.getId());
            if (CollUtil.isEmpty(nowList) && CollUtil.isEmpty(lastList) && CollUtil.isEmpty(totalList)) {
                return;
            }
            ComparisonItemVO vo = buildComparisonItemVODataList(nowList, lastList, totalList, dataTypeEnum, tableHeader, isCrossYear, valueExtractor);

            if (Objects.isNull(vo)) {
                return;
            }

            vo.setEnergyId(energy.getId());
            vo.setEnergyName(energy.getEnergyName());
            detailList.add(vo);
        });

        return detailList;
    }

    /**
     * 按标签维度统计：以 standingbookId 和标签结构为基础构建环比对比数据
     */
    private List<ComparisonItemVO> queryByTopLabel(
            List<UsageCostData> usageCostDataList,
            List<UsageCostData> lastUsageCostDataList,
            List<UsageCostData> totalUsageCostDataList,
            Map<Long, LabelConfigDO> labelMap,
            String topLabelKey,
            DataTypeEnum dateTypeEnum,
            List<String> tableHeader,
            boolean isCrossYear,
            Function<UsageCostData, BigDecimal> valueExtractor) {

        List<ComparisonItemVO> resultList = new ArrayList<>();

        // 构造结果对象
        ComparisonItemVO info = buildComparisonItemVODataList(
                usageCostDataList,
                lastUsageCostDataList,
                totalUsageCostDataList,
                dateTypeEnum,
                tableHeader,
                isCrossYear,
                valueExtractor);

        if (Objects.isNull(info)) {
            return Collections.emptyList();
        }

        Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
        LabelConfigDO topLabel = labelMap.get(topLabelId);
        info.setLabel1(topLabel.getLabelName());

        info.setLabel2(StrPool.SLASH);
        info.setLabel3(StrPool.SLASH);
        info.setLabel4(StrPool.SLASH);
        info.setLabel5(StrPool.SLASH);

        resultList.add(info);

        return resultList;
    }

    /**
     * 按标签维度统计：以 standingbookId 和标签结构为基础构建环比对比数据
     */
    private List<ComparisonItemVO> queryBySubLabel(List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                   List<UsageCostData> usageCostDataList,
                                                   List<UsageCostData> lastUsageCostDataList,
                                                   List<UsageCostData> totalUsageCostDataList,
                                                   Map<Long, LabelConfigDO> labelMap,
                                                   DataTypeEnum dateTypeEnum,
                                                   List<String> tableHeader,
                                                   boolean isCrossYear,
                                                   Function<UsageCostData, BigDecimal> valueExtractor) {

        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        StandingbookLabelInfoDO::getName,
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));

        // 当前周期数据按 standingbookId 分组
        Map<Long, List<UsageCostData>> currentMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        // 上期数据以 standingbookId
        Map<Long, List<UsageCostData>> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        Map<Long, List<UsageCostData>> totalMap = totalUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        List<ComparisonItemVO> resultList = new ArrayList<>();

        // 遍历一级标签
        grouped.forEach((topLabelKey, labelInfoGroup) -> {
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);
            if (topLabel == null) return;

            // 遍历二级标签组合
            labelInfoGroup.forEach((valueKey, labelInfoList) -> {
                String[] labelIds = valueKey.split(",");
                String label2Name = getLabelName(labelMap, labelIds, 0);
                String label3Name = labelIds.length > 1 ? getLabelName(labelMap, labelIds, 1) : StrPool.SLASH;
                String label4Name = labelIds.length > 2 ? getLabelName(labelMap, labelIds, 2) : StrPool.SLASH;
                String label5Name = labelIds.length > 3 ? getLabelName(labelMap, labelIds, 3) : StrPool.SLASH;

                List<UsageCostData> labelUsageListNow = new ArrayList<>();
                List<UsageCostData> labelUsageListPrevious = new ArrayList<>();
                List<UsageCostData> labelUsageListTotal = new ArrayList<>();
                // 获取本期标签关联的台账id，并取到对应的数据
                // 获取本期
                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = currentMap.get(labelInfo.getStandingbookId());
                    if (usageList != null) {
                        labelUsageListNow.addAll(usageList);
                    }
                    List<UsageCostData> usageList1 = lastMap.get(labelInfo.getStandingbookId());
                    if (usageList1 != null) {
                        labelUsageListPrevious.addAll(usageList1);
                    }

                    List<UsageCostData> usageList2 = totalMap.get(labelInfo.getStandingbookId());
                    if (usageList2 != null) {
                        labelUsageListTotal.addAll(usageList2);
                    }
                });

                // 构造结果对象
                ComparisonItemVO info = buildComparisonItemVODataList(
                        labelUsageListNow,
                        labelUsageListPrevious,
                        labelUsageListTotal,
                        dateTypeEnum,
                        tableHeader,
                        isCrossYear,
                        valueExtractor);
                if (Objects.isNull(info)) {
                    return;
                }
                info.setLabel1(topLabel.getLabelName());
                info.setLabel2(label2Name);
                info.setLabel3(label3Name);
                info.setLabel4(label4Name);
                info.setLabel5(label5Name);

                resultList.add(info);
            });
        });

        return resultList;
    }


    public List<ComparisonItemVO> queryDefault(String topLabel,
                                               String childLabels,
                                               List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                               List<UsageCostData> usageCostDataList,
                                               List<UsageCostData> lastUsageCostDataList,
                                               List<UsageCostData> totalUsageCostDataList,
                                               DataTypeEnum dateTypeEnum,
                                               List<String> tableHeader,
                                               boolean isCrossYear,
                                               Function<UsageCostData, BigDecimal> valueExtractor) {

        // 实际用到的能源ids
        Set<Long> energyIdSet = Stream.concat(
                        Stream.concat(usageCostDataList.stream().map(UsageCostData::getEnergyId), lastUsageCostDataList.stream().map(UsageCostData::getEnergyId)),
                        totalUsageCostDataList.stream().map(UsageCostData::getEnergyId))
                .collect(Collectors.toSet());
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getPureByEnergyClassify(energyIdSet, null);
        Map<Long, EnergyConfigurationDO> energyMap = energyList.stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        // 查询所有标签配置
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));


        if (CharSequenceUtil.isNotBlank(topLabel) && CharSequenceUtil.isBlank(childLabels)) {
            // 只有顶级标签
            return queryDefaultTopLabel(
                    usageCostDataList,
                    lastUsageCostDataList,
                    totalUsageCostDataList,
                    labelMap,
                    energyMap,
                    topLabel,
                    dateTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor);
        } else {
            // 有顶级、有子集标签
            return queryDefaultSubLabel(
                    standingbookIdsByLabel,
                    usageCostDataList,
                    lastUsageCostDataList,
                    totalUsageCostDataList,
                    labelMap,
                    energyMap,
                    dateTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor);
        }
    }


    /**
     * 综合默认统计：标签 + energyId 双维度聚合构建对比数据
     */
    private List<ComparisonItemVO> queryDefaultTopLabel(
            List<UsageCostData> usageCostDataList,
            List<UsageCostData> lastUsageCostDataList,
            List<UsageCostData> totalUsageCostDataList,
            Map<Long, LabelConfigDO> labelMap,
            Map<Long, EnergyConfigurationDO> energyMap,
            String topLabelKey,
            DataTypeEnum dateTypeEnum,
            List<String> tableHeader,
            boolean isCrossYear,
            Function<UsageCostData, BigDecimal> valueExtractor) {

        // 按能源ID分组当前周期数据
        Map<Long, List<UsageCostData>> nowUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
        Map<Long, List<UsageCostData>> lastUsageMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        Map<Long, List<UsageCostData>> totalUsageMap = totalUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));


        List<ComparisonItemVO> resultList = new ArrayList<>();

        Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
        LabelConfigDO topLabel = labelMap.get(topLabelId);

        energyMap.forEach((energyId, energyConfigurationDO) -> {

            // 构造结果对象
            ComparisonItemVO info = buildComparisonItemVODataList(
                    nowUsageMap.get(energyId),
                    lastUsageMap.get(energyId),
                    totalUsageMap.get(energyId),
                    dateTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor);

            if (Objects.isNull(info)) {
                return;
            }

            info.setEnergyId(energyId);
            info.setEnergyName(energyConfigurationDO.getEnergyName());
            info.setLabel1(topLabel.getLabelName());
            info.setLabel2(StrPool.SLASH);
            info.setLabel3(StrPool.SLASH);
            info.setLabel4(StrPool.SLASH);
            info.setLabel5(StrPool.SLASH);

            resultList.add(info);
        });

        return resultList;
    }

    /**
     * 综合默认统计：标签 + energyId 双维度聚合构建对比数据
     */
    private List<ComparisonItemVO> queryDefaultSubLabel(
            List<StandingbookLabelInfoDO> standingbookIdsByLabel,
            List<UsageCostData> usageCostDataList,
            List<UsageCostData> lastUsageCostDataList,
            List<UsageCostData> totalUsageCostDataList,
            Map<Long, LabelConfigDO> labelMap,
            Map<Long, EnergyConfigurationDO> energyMap,
            DataTypeEnum dateTypeEnum,
            List<String> tableHeader,
            boolean isCrossYear,
            Function<UsageCostData, BigDecimal> valueExtractor) {

        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        StandingbookLabelInfoDO::getName,
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));

        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageNowMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        Map<Long, List<UsageCostData>> standingBookUsagePrevMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        Map<Long, List<UsageCostData>> standingBookUsageTotalMap = totalUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        List<ComparisonItemVO> resultList = new ArrayList<>();

        // 遍历一级标签分组
        grouped.forEach((topLabelKey, labelInfoGroup) -> {
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);
            if (topLabel == null) return;

            labelInfoGroup.forEach((valueKey, labelInfoList) -> {
                String[] labelIds = valueKey.split(",");
                String label2Name = getLabelName(labelMap, labelIds, 0);
                String label3Name = labelIds.length > 1 ? getLabelName(labelMap, labelIds, 1) : StrPool.SLASH;
                String label4Name = labelIds.length > 2 ? getLabelName(labelMap, labelIds, 2) : StrPool.SLASH;
                String label5Name = labelIds.length > 3 ? getLabelName(labelMap, labelIds, 3) : StrPool.SLASH;

                List<UsageCostData> labelUsageCostDataNowList = new ArrayList<>();
                List<UsageCostData> labelUsageCostDataPrevList = new ArrayList<>();
                List<UsageCostData> labelUsageCostDataTotalList = new ArrayList<>();
                // 获取标签关联的台账id，并取到对应的数据
                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageNowList = standingBookUsageNowMap.get(labelInfo.getStandingbookId());

                    if (CollUtil.isNotEmpty(usageNowList)) {
                        labelUsageCostDataNowList.addAll(usageNowList);
                    }
                    List<UsageCostData> usagePrevList = standingBookUsagePrevMap.get(labelInfo.getStandingbookId());
                    if (CollUtil.isNotEmpty(usagePrevList)) {
                        labelUsageCostDataPrevList.addAll(usagePrevList);
                    }

                    List<UsageCostData> usageTotalList = standingBookUsageTotalMap.get(labelInfo.getStandingbookId());
                    if (CollUtil.isNotEmpty(usageTotalList)) {
                        labelUsageCostDataTotalList.addAll(usageTotalList);
                    }
                });

                // 按能源ID分组当前周期数据
                Map<Long, List<UsageCostData>> energyUsageCostNowMap = new HashMap<>();
                Map<Long, List<UsageCostData>> energyUsageCostPrevMap = new HashMap<>();
                Map<Long, List<UsageCostData>> energyUsageCostTotalMap = new HashMap<>();

                if (CollUtil.isNotEmpty(labelUsageCostDataNowList)) {
                    energyUsageCostNowMap = labelUsageCostDataNowList
                            .stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
                }
                if (CollUtil.isNotEmpty(labelUsageCostDataPrevList)) {
                    energyUsageCostPrevMap = labelUsageCostDataPrevList
                            .stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
                }
                if (CollUtil.isNotEmpty(labelUsageCostDataTotalList)) {
                    energyUsageCostTotalMap = labelUsageCostDataTotalList
                            .stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
                }

                Map<Long, List<UsageCostData>> finalEnergyUsageCostNowMap = energyUsageCostNowMap;
                Map<Long, List<UsageCostData>> finalEnergyUsageCostPrevMap = energyUsageCostPrevMap;
                Map<Long, List<UsageCostData>> finalEnergyUsageCostTotalMap = energyUsageCostTotalMap;

                energyMap.forEach((energyId, energyConfigurationDO) -> {
                    if (energyConfigurationDO == null) return;

                    // 构造结果对象
                    ComparisonItemVO info = buildComparisonItemVODataList(
                            finalEnergyUsageCostNowMap.get(energyId),
                            finalEnergyUsageCostPrevMap.get(energyId),
                            finalEnergyUsageCostTotalMap.get(energyId),
                            dateTypeEnum,
                            tableHeader,
                            isCrossYear,
                            valueExtractor);

                    if (Objects.isNull(info)) {
                        return;
                    }

                    info.setEnergyId(energyId);
                    info.setEnergyName(energyConfigurationDO.getEnergyName());
                    info.setLabel1(topLabel.getLabelName());
                    info.setLabel2(label2Name);
                    info.setLabel3(label3Name);
                    info.setLabel4(label4Name);
                    info.setLabel5(label5Name);

                    resultList.add(info);
                });
            });
        });

        return resultList;
    }

    /**
     * 根据 usageCostDataList 来获取按时间分组的数据List
     *
     * @param usageCostDataList
     * @param valueExtractor
     * @return
     */
    private List<TimeAndNumData> getTimeAndNumDataList(List<UsageCostData> usageCostDataList, Function<UsageCostData, BigDecimal> valueExtractor) {
        return new ArrayList<>(usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getTime,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    BigDecimal totalNum = list.stream()
                                            .map(valueExtractor)
                                            .filter(Objects::nonNull)
                                            .reduce(BigDecimal::add).orElse(null);
                                    return new TimeAndNumData(list.get(0).getTime(), totalNum);
                                }
                        )
                )).values());

    }

    /**
     * 根据 usageCostDataList 来获取按时间分组的数据Map
     *
     * @param usageCostDataList
     * @param valueExtractor
     * @return
     */
    private Map<String, TimeAndNumData> getTimeAndNumDataMap(List<UsageCostData> usageCostDataList, Function<UsageCostData, BigDecimal> valueExtractor) {
        // 处理上期
        return usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getTime,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    BigDecimal totalStandardCoal = list.stream()
                                            .map(valueExtractor)
                                            .filter(Objects::nonNull)
                                            .reduce(BigDecimal::add).orElse(null);
                                    return new TimeAndNumData(list.get(0).getTime(), totalStandardCoal);
                                }
                        )
                ));

    }

    /**
     * 获取标签名
     *
     * @param labelMap 标签配置map
     * @param labelIds 标签id数组
     * @param index    标签索引
     */
    private String getLabelName(Map<Long, LabelConfigDO> labelMap, String[] labelIds, int index) {
        if (index < labelIds.length) {
            LabelConfigDO label = labelMap.get(Long.valueOf(labelIds[index]));
            if (label != null) {
                return label.getLabelName();
            }
        }
        return StrPool.SLASH;
    }

    /**
     * 环比率计算（避免除零）
     */
    private BigDecimal calculateRatio(BigDecimal now, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0 || now == null) {
            return null;
        }
        return now.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }


    @Override
    public ComparisonChartResultVO discountAnalysisChart(StatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalCost, StatisticsCacheConstants.COMPARISON_DISCOUNT_CHART_COST);
    }

    @Override
    public ComparisonChartResultVO foldCoalAnalysisChart(StatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalStandardCoalEquivalent, StatisticsCacheConstants.COMPARISON_DISCOUNT_CHART_COAL);
    }


    public ComparisonChartResultVO analysisChart(StatisticsParamV2VO paramVO,
                                                 Function<UsageCostData, BigDecimal> valueExtractor, String commonType) {
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

        // 3. 尝试读取缓存（避免重复计算）
        String cacheKey = commonType + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, ComparisonChartResultVO.class);
        }

        ComparisonChartResultVO result = new ComparisonChartResultVO();

        // 4. 查询能源信息及能源ID
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getPureByEnergyClassify(
                new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        if (CollUtil.isEmpty(energyList)) {
            return result;
        }
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 5. 查询台账信息（按能源）
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        // 查询标签信息（按标签过滤台账）
        String topLabel = paramVO.getTopLabel();
        String childLabels = paramVO.getChildLabels();
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService
                .getStandingbookIdsByLabel(topLabel, childLabels);

        List<Long> standingBookIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream()
                    .filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            if (CollUtil.isEmpty(collect)) {
                result.setList(Collections.emptyList());
                return result;
            }
            standingBookIds.addAll(collect.stream().map(StandingbookDO::getId).collect(Collectors.toList()));
        } else {
            standingBookIds.addAll(standingBookIdList);
        }

        if (CollUtil.isEmpty(standingBookIds)) {
            result.setList(Collections.emptyList());
            return result;
        }

        // 7. 查询当前周期与上周期的折扣数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, startTime, endTime, standingBookIds);
        LocalDateTime[] lastRange = LocalDateTimeUtils.getPreviousRangeV2(rangeOrigin, dataTypeEnum);
        List<UsageCostData> lastUsageCostDataList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], standingBookIds);

        // 8. 构建横轴时间（xdata）
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);
        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, startTime, endTime, standingBookIds);

        // 9. 根据维度类型进行聚合图表构建
        Integer queryType = paramVO.getQueryType();
        List<ComparisonChartGroupVO> groupList;

        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            groupList = buildChartByEnergy(energyList, usageCostDataList, lastUsageCostDataList, xdata, dataTypeEnum, valueExtractor);
        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            groupList = buildChartByLabel(standingbookIdsByLabel, standingBookIds, usageCostDataList, lastUsageCostDataList, xdata, dataTypeEnum, valueExtractor);
        } else {
            groupList = buildChartByDefault(usageCostDataList, lastUsageCostDataList, xdata, dataTypeEnum, valueExtractor);
        }

        // 10. 构建最终图表结果并缓存
        result.setList(groupList);
        result.setDataTime(lastTime);

        String jsonStr = JSONUtil.toJsonStr(result);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return result;
    }


    /**
     * 构建图表数据 - 按能源维度聚合（每个能源为一组图表）
     * 将每个能源 ID 作为维度，对当前周期和上周期进行横轴聚合
     * 输出当前值、上期值、环比数据三组柱状/折线图序列
     */
    private List<ComparisonChartGroupVO> buildChartByEnergy(List<EnergyConfigurationDO> energyList,
                                                            List<UsageCostData> usageCostDataList,
                                                            List<UsageCostData> lastUsageCostDataList,
                                                            List<String> xdata,
                                                            DataTypeEnum dataTypeEnum,
                                                            Function<UsageCostData, BigDecimal> valueExtractor) {
        // 构建当前周期和上周期数据的 (energyId -> time -> cost) 映射
        Map<Long, Map<String, BigDecimal>> nowMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getEnergyId,
                        Collectors.toMap(UsageCostData::getTime,
                                // 保留原始值（可能为null）
                                valueExtractor,
                                // 合并逻辑：处理各种null情况
                                (v1, v2) -> {
                                    if (v1 == null) return v2;
                                    if (v2 == null) return v1;
                                    return v1.add(v2);
                                }
                        )
                ));

        Map<Long, Map<String, BigDecimal>> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getEnergyId,
                        Collectors.toMap(UsageCostData::getTime,
                                // 保留原始值（可能为null）
                                valueExtractor,
                                // 合并逻辑：处理各种null情况
                                (v1, v2) -> {
                                    if (v1 == null) return v2;
                                    if (v2 == null) return v1;
                                    return v1.add(v2);
                                }
                        )
                ));

        List<ComparisonChartGroupVO> result = new ArrayList<>();
        for (EnergyConfigurationDO energy : energyList) {
            Map<String, BigDecimal> nowSeries = nowMap.getOrDefault(energy.getId(), new HashMap<>());
            Map<String, BigDecimal> lastSeries = lastMap.getOrDefault(energy.getId(), new HashMap<>());

            List<BigDecimal> nowList = new ArrayList<>();
            List<BigDecimal> lastList = new ArrayList<>();
            List<BigDecimal> ratioList = new ArrayList<>();
            // 遍历横轴时间点构造每条数据序列
            for (String time : xdata) {
                BigDecimal now = nowSeries.get(time);
                String lastTime = LocalDateTimeUtils.getPreviousTime(time, dataTypeEnum);
                BigDecimal previous = lastSeries.get(lastTime);
                nowList.add(dealBigDecimalScale(now, DEFAULT_SCALE));
                lastList.add(dealBigDecimalScale(previous, DEFAULT_SCALE));
                ratioList.add(dealBigDecimalScale(calculateRatio(now, previous), DEFAULT_SCALE));
            }

            // 当期，上期都没值的情况下，则不返回该能源。
            long count1 = nowList.stream().filter(Objects::nonNull).count();
            long count2 = nowList.stream().filter(Objects::nonNull).count();
            if (count1 == 0 && count2 == 0) {
                continue;
            }

            List<ChartSeriesItemVO> ydata = Arrays.asList(
                    new ChartSeriesItemVO(NOW, ChartSeriesTypeEnum.BAR.getType(), nowList, null),
                    new ChartSeriesItemVO(PREVIOUS, ChartSeriesTypeEnum.BAR.getType(), lastList, null),
                    new ChartSeriesItemVO(RATIO, ChartSeriesTypeEnum.LINE.getType(), ratioList, 1)
            );

            ComparisonChartGroupVO group = new ComparisonChartGroupVO();
            group.setName(energy.getEnergyName());
            group.setXdata(xdata);
            group.setYdata(ydata);
            result.add(group);
        }
        return result;
    }

    /**
     * 构建图表数据 - 按标签维度聚合（每个标签为一组图表）
     * 以标签为维度，将台账 ID 归类并统计每个标签下的时间分布数据
     * 支持标签名还原为中文名，构建当前值、上期值、环比对比序列
     */
    private List<ComparisonChartGroupVO> buildChartByLabel(List<StandingbookLabelInfoDO> labelList,
                                                           List<Long> validStandingbookIds,
                                                           List<UsageCostData> usageCostDataList,
                                                           List<UsageCostData> lastUsageCostDataList,
                                                           List<String> xdata,
                                                           DataTypeEnum dataTypeEnum,
                                                           Function<UsageCostData, BigDecimal> valueExtractor) {
        // 构造 standingbookId -> labelKey（例如 "label_1"）映射
        Map<Long, String> standingbookLabelMap = labelList.stream()
                .filter(s -> validStandingbookIds.contains(s.getStandingbookId()))
                .collect(Collectors.toMap(StandingbookLabelInfoDO::getStandingbookId, StandingbookLabelInfoDO::getName, (a, b) -> a));

        // 提取一级标签 ID 并加载标签配置
        List<Long> labelIds = standingbookLabelMap.values().stream()
                .map(name -> Long.valueOf(name.substring(name.indexOf("_") + 1)))
                .distinct()
                .collect(Collectors.toList());
        Map<String, LabelConfigDO> labelMap = labelConfigService.getByIds(labelIds).stream()
                .collect(Collectors.toMap(l -> "label_" + l.getId(), Function.identity()));

        // 构造 (labelKey -> time -> cost) 的二维映射（当前周期）
        Map<String, Map<String, BigDecimal>> nowMap = new HashMap<>();
        for (UsageCostData data : usageCostDataList) {
            String label = standingbookLabelMap.get(data.getStandingbookId());
            if (label == null) continue;
            nowMap.computeIfAbsent(label, k -> new HashMap<>())
                    .merge(data.getTime(), valueExtractor.apply(data),  (v1, v2) -> {
                                if (v1 == null) return v2;
                                if (v2 == null) return v1;
                                return v1.add(v2);
                            }
                    );
        }

        // 构造 (labelKey -> time -> cost) 的二维映射（上周期）
        Map<String, Map<String, BigDecimal>> lastMap = new HashMap<>();
        for (UsageCostData data : lastUsageCostDataList) {
            String label = standingbookLabelMap.get(data.getStandingbookId());
            if (label == null) continue;
            lastMap.computeIfAbsent(label, k -> new HashMap<>())
                    .merge(data.getTime(), valueExtractor.apply(data),  (v1, v2) -> {
                                if (v1 == null) return v2;
                                if (v2 == null) return v1;
                                return v1.add(v2);
                            }
                    );
        }

        // 构造图表组数据（每个标签一个）
        List<ComparisonChartGroupVO> result = new ArrayList<>();
        for (String labelKey : nowMap.keySet()) {
            Map<String, BigDecimal> nowSeries = nowMap.get(labelKey);
            Map<String, BigDecimal> lastSeries = lastMap.getOrDefault(labelKey, Collections.emptyMap());

            List<BigDecimal> nowList = new ArrayList<>();
            List<BigDecimal> lastList = new ArrayList<>();
            List<BigDecimal> ratioList = new ArrayList<>();

            for (String time : xdata) {
                BigDecimal now = nowSeries.get(time);
                String lastTime = LocalDateTimeUtils.getPreviousTime(time, dataTypeEnum);
                BigDecimal previous = lastSeries.get(lastTime);
                nowList.add(dealBigDecimalScale(now, DEFAULT_SCALE));
                lastList.add(dealBigDecimalScale(previous, DEFAULT_SCALE));
                ratioList.add(dealBigDecimalScale(calculateRatio(now, previous), DEFAULT_SCALE));
            }

            List<ChartSeriesItemVO> ydata = Arrays.asList(
                    new ChartSeriesItemVO(NOW, ChartSeriesTypeEnum.BAR.getType(), nowList, null),
                    new ChartSeriesItemVO(PREVIOUS, ChartSeriesTypeEnum.BAR.getType(), lastList, null),
                    new ChartSeriesItemVO(RATIO, ChartSeriesTypeEnum.LINE.getType(), ratioList, 1)
            );

            ComparisonChartGroupVO group = new ComparisonChartGroupVO();
            group.setName(Optional.ofNullable(labelMap.get(labelKey)).map(LabelConfigDO::getLabelName).orElse(labelKey));
            group.setXdata(xdata);
            group.setYdata(ydata);
            result.add(group);
        }
        return result;
    }

    /**
     * 构建图表数据 - 默认（综合）维度聚合，仅返回一个图表组，名称为“总”
     * 将所有台账数据聚合统计为一个合计序列，用于展示总用量趋势
     */
    private List<ComparisonChartGroupVO> buildChartByDefault(List<UsageCostData> usageCostDataList,
                                                             List<UsageCostData> lastUsageCostDataList,
                                                             List<String> xdata,
                                                             DataTypeEnum dataTypeEnum,
                                                             Function<UsageCostData, BigDecimal> valueExtractor) {
        // 当前周期与上周期：时间 -> 总和映射
        Map<String, BigDecimal> nowMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getTime,
                        Collectors.mapping(valueExtractor,
                                // 初始值为null，自定义累加逻辑处理null情况
                                Collectors.reducing(null, (v1, v2) -> {
                                    if (v1 == null) return v2;
                                    if (v2 == null) return v1;
                                    return v1.add(v2);
                                })
                        )
                ));

        Map<String, BigDecimal> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getTime,
                        Collectors.mapping(valueExtractor,
                                // 初始值为null，自定义累加逻辑处理null情况
                                Collectors.reducing(null, (v1, v2) -> {
                                    if (v1 == null) return v2;
                                    if (v2 == null) return v1;
                                    return v1.add(v2);
                                })
                        )
                ));

        List<BigDecimal> nowList = new ArrayList<>();
        List<BigDecimal> lastList = new ArrayList<>();
        List<BigDecimal> ratioList = new ArrayList<>();

        for (String time : xdata) {
            BigDecimal now = nowMap.get(time);
            String lastTime = LocalDateTimeUtils.getPreviousTime(time, dataTypeEnum);
            BigDecimal previous = lastMap.get(lastTime);
            nowList.add(dealBigDecimalScale(now, DEFAULT_SCALE));
            lastList.add(dealBigDecimalScale(previous, DEFAULT_SCALE));
            ratioList.add(dealBigDecimalScale(calculateRatio(now, previous), DEFAULT_SCALE));
        }

        List<ChartSeriesItemVO> ydata = Arrays.asList(
                new ChartSeriesItemVO(NOW, ChartSeriesTypeEnum.BAR.getType(), nowList, null),
                new ChartSeriesItemVO(PREVIOUS, ChartSeriesTypeEnum.BAR.getType(), lastList, null),
                new ChartSeriesItemVO(RATIO, ChartSeriesTypeEnum.LINE.getType(), ratioList, 1)
        );

        ComparisonChartGroupVO group = new ComparisonChartGroupVO();
        group.setName(DEFAULT_GROUP_NAME);
        group.setXdata(xdata);
        group.setYdata(ydata);

        return Collections.singletonList(group);
    }

    @Override
    public List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO, Integer flag) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];

        // 验证单位
        Integer unit = paramVO.getUnit();
        validateUnit(unit);

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
        Integer queryType = paramVO.getQueryType();
        String sheetName;
        if (flag == 1) {
            // 折标煤
            switch (queryType) {
                case 0:
                    // 综合
                    sheetName = STANDARD_COAL_MOM_ALL;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                    for (int i = 2; i <= labelDeep; i++) {
                        String subLabel = "标签" + i;
                        list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                    }
                    list.add(Arrays.asList(sheetName, labelName, strTime, "能源", "能源"));
                    break;
                case 1:
                    // 按能源
                    sheetName = STANDARD_COAL_MOM_ENERGY;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "能源", "能源"));
                    break;
                case 2:
                    // 按标签
                    sheetName = STANDARD_COAL_MOM_LABEL;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                    for (int i = 2; i <= labelDeep; i++) {
                        String subLabel = "标签" + i;
                        list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                    }
                    break;
                default:
                    sheetName = DEFAULT;
            }
        } else {
            // 用能成本
            switch (queryType) {
                case 0:
                    // 综合
                    sheetName = COST_MOM_ALL;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                    for (int i = 2; i <= labelDeep; i++) {
                        String subLabel = "标签" + i;
                        list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                    }
                    list.add(Arrays.asList(sheetName, labelName, strTime, "能源", "能源"));
                    break;
                case 1:
                    // 按能源
                    sheetName = COST_MOM_ENERGY;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "能源", "能源"));
                    break;
                case 2:
                    // 按标签
                    sheetName = COST_MOM_LABEL;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                    for (int i = 2; i <= labelDeep; i++) {
                        String subLabel = "标签" + i;
                        list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                    }
                    break;
                default:
                    sheetName = DEFAULT;
            }
        }

        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        String finalSheetName = sheetName;
        xdata.forEach(x -> {
            list.add(Arrays.asList(finalSheetName, labelName, strTime, x, getHeaderDesc(unit, flag, NOW)));
            list.add(Arrays.asList(finalSheetName, labelName, strTime, x, getHeaderDesc(unit, flag, PREVIOUS)));
            list.add(Arrays.asList(finalSheetName, labelName, strTime, x, RATIO_PERCENT));
        });

        // 周期合计
        list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计", getHeaderDesc(unit, flag, NOW)));
        list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计", getHeaderDesc(unit, flag, PREVIOUS)));
        list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计", RATIO_PERCENT));
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
    public List<List<Object>> getExcelData(StatisticsParamV2VO paramVO, Integer flag) {
        // 验证单位
        Integer unit = paramVO.getUnit();

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        StatisticsResultV2VO<ComparisonItemVO> resultVO;
        if (flag == 1) {
            // 折标煤
            resultVO = foldCoalAnalysisTable(paramVO);
        } else {
            // 用能成本
            resultVO = discountAnalysisTable(paramVO);
        }
        List<String> tableHeader = resultVO.getHeader();

        List<ComparisonItemVO> comparisonItemVOList = resultVO.getStatisticsInfoList();
        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);

        Integer queryType = paramVO.getQueryType();

        // 底部合计map
        Map<String, BigDecimal> sumNowMap = new HashMap<>();
        Map<String, BigDecimal> sumPreviousMap = new HashMap<>();

        for (ComparisonItemVO s : comparisonItemVOList) {

            List<Object> data = ListUtils.newArrayList();
            String[] labels = {s.getLabel1(), s.getLabel2(), s.getLabel3(), s.getLabel4(), s.getLabel5()};
            switch (queryType) {
                case 0:
                    // 综合
                    // 处理标签
                    for (int i = 0; i < labelDeep; i++) {
                        data.add(labels[i]);
                    }
                    // 处理能源
                    data.add(s.getEnergyName());
                    break;
                case 1:
                    // 按能源
                    data.add(s.getEnergyName());
                    break;
                case 2:
                    // 按标签
                    // 处理标签
                    for (int i = 0; i < labelDeep; i++) {
                        data.add(labels[i]);
                    }
                    break;
                default:
            }

            // 处理数据
            List<ComparisonDetailVO> statisticsRatioDataList = s.getStatisticsRatioDataList();

            Map<String, ComparisonDetailVO> dateMap = statisticsRatioDataList.stream()
                    .collect(Collectors.toMap(ComparisonDetailVO::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                ComparisonDetailVO comparisonDetailVO = dateMap.get(date);
                if (comparisonDetailVO == null) {
                    data.add(StrPool.SLASH);
                    data.add(StrPool.SLASH);
                    data.add(StrPool.SLASH);
                } else {
                    BigDecimal now = comparisonDetailVO.getNow();
                    BigDecimal previous = comparisonDetailVO.getPrevious();
                    BigDecimal proportion = comparisonDetailVO.getRatio();
                    data.add(getConvertData(unit, flag, now));
                    data.add(getConvertData(unit, flag, previous));
                    data.add(getConvertData(proportion));

                    // 底部合计处理
                    sumNowMap.put(date, addBigDecimal(sumNowMap.get(date), now));
                    sumPreviousMap.put(date, addBigDecimal(sumPreviousMap.get(date), previous));
                }

            });

            BigDecimal sumNow = s.getSumNow();
            BigDecimal sumPrevious = s.getSumPrevious();
            BigDecimal sumProportion = s.getSumRatio();
            // 处理周期合计
            data.add(getConvertData(unit, flag, sumNow));
            data.add(getConvertData(unit, flag, sumPrevious));
            data.add(getConvertData(sumProportion));

            // 处理底部合计
            sumNowMap.put("sumNum", addBigDecimal(sumNowMap.get("sumNum"), sumNow));
            sumPreviousMap.put("sumNum", addBigDecimal(sumPreviousMap.get("sumNum"), sumPrevious));
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


        switch (queryType) {
            case 0:
                // 综合
                // 底部标签位
                for (int i = 0; i < labelDeep; i++) {
                    bottom.add(pre);
                }
                // 底部能源位
                bottom.add(pre);
                break;
            case 1:
                // 按能源
                // 底部能源位
                bottom.add(pre);
                break;
            case 2:
                // 按标签
                // 底部标签位
                for (int i = 0; i < labelDeep; i++) {
                    bottom.add(pre);
                }
                break;
            default:
        }

        // 底部数据位
        tableHeader.forEach(date -> {
            // 当期
            BigDecimal now = sumNowMap.get(date);
            bottom.add(getConvertData(unit, flag, now));
            // 同期
            BigDecimal previous = sumPreviousMap.get(date);
            bottom.add(getConvertData(unit, flag, previous));
            // 同比
            bottom.add(getConvertData(calculateRatio(now, previous)));
        });

        // 底部周期合计
        // 当期
        BigDecimal sumNow = sumNowMap.get("sumNum");
        bottom.add(getConvertData(unit, flag, sumNow));
        // 同期
        BigDecimal sumPrevious = sumPreviousMap.get("sumNum");
        bottom.add(getConvertData(unit, flag, sumPrevious));
        // 同比
        bottom.add(getConvertData(calculateRatio(sumNow, sumPrevious)));

        result.add(bottom);

        return result;
    }

    @Override
    public StatisticsResultV2VO<ComparisonItemVO> getUtilizationRateTable(StatisticsParamV2VO paramVO) {

        // 1.校验时间范围合法性
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];

        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        String cacheKey = COMPARISON_DISCOUNT_TABLE_UTILIZATION_RATE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsResultV2VO<ComparisonItemVO>>() {
            });
        }

        // 构建表头
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);
        StatisticsResultV2VO<ComparisonItemVO> resultVO = new StatisticsResultV2VO<>();
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

        // 上期时间
        LocalDateTime[] lastRange = LocalDateTimeUtils.getPreviousRangeV2(rangeOrigin, dataTypeEnum);
        // 周期合计上期时间
        LocalDateTime[] totalRange = LocalDateTimeUtils.getPreviousRange(rangeOrigin, DataTypeEnum.DAY);

        // 查询外购
        List<UsageCostData> outsourceList = new ArrayList<>();
        List<UsageCostData> lastOutsourceList = new ArrayList<>();
        List<UsageCostData> totalOutsourceList = ListUtils.newArrayList();
        if (CollUtil.isNotEmpty(outsourceSbIds)) {
            outsourceList = usageCostService.getList(paramVO, startTime, endTime, outsourceSbIds);
            lastOutsourceList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], outsourceSbIds);
            totalOutsourceList = usageCostService.getList(paramVO, totalRange[0], totalRange[1], outsourceSbIds);
        }

        // 查询园区
        List<UsageCostData> parkList = new ArrayList<>();
        List<UsageCostData> lastParkList = new ArrayList<>();
        List<UsageCostData> totalParkList = ListUtils.newArrayList();
        if (CollUtil.isNotEmpty(parkSbIds)) {
            parkList = usageCostService.getList(paramVO, startTime, endTime, parkSbIds);
            lastParkList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], parkSbIds);
            totalParkList = usageCostService.getList(paramVO, totalRange[0], totalRange[1], parkSbIds);
        }

        // 查询分子
        List<UsageCostData> numeratorList = usageCostService.getList(paramVO, startTime, endTime, sbIds);
        List<UsageCostData> lastNumeratorList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], sbIds);
        List<UsageCostData> totalNumeratorList = usageCostService.getList(paramVO, totalRange[0], totalRange[1], sbIds);
        boolean isCrossYear = DateUtils.isCrossYear(startTime, endTime);

        // 综合默认查看
        List<ComparisonItemVO> statisticsInfoList = queryList(
                outsourceList,
                parkList,
                numeratorList,
                lastOutsourceList,
                lastParkList,
                lastNumeratorList,
                totalOutsourceList,
                totalParkList,
                totalNumeratorList,
                isCrossYear,
                tableHeader);

        // 设置最终返回值
        resultVO.setStatisticsInfoList(statisticsInfoList);

        LocalDateTime lastTime1 = null;
        if (CollUtil.isNotEmpty(outsourceSbIds)) {
            lastTime1 = usageCostService.getLastTimeNoParam(
                    paramVO.getRange()[0],
                    paramVO.getRange()[1],
                    outsourceSbIds
            );
        }

        LocalDateTime lastTime2 = null;
        if (CollUtil.isNotEmpty(parkSbIds)) {
            lastTime2 = usageCostService.getLastTimeNoParam(
                    paramVO.getRange()[0],
                    paramVO.getRange()[1],
                    parkSbIds);
        }

        LocalDateTime lastTime3 = null;
        if (CollUtil.isNotEmpty(sbIds)) {
            lastTime3 = usageCostService.getLastTimeNoParam(
                    paramVO.getRange()[0],
                    paramVO.getRange()[1],
                    sbIds
            );
        }

        // 找出三个时间中的最大值（最新时间）
        LocalDateTime latestTime = Arrays.stream(new LocalDateTime[]{lastTime1, lastTime2, lastTime3})
                .filter(Objects::nonNull) // 排除null
                .max(Comparator.naturalOrder())
                .orElse(null); // 若全部为null，返回null
        resultVO.setDataTime(latestTime);
        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        return resultVO;
    }

    @Override
    public ComparisonChartResultVO getUtilizationRateChart(StatisticsParamV2VO paramVO) {

        // 1.校验时间范围合法性
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];

        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        String cacheKey = COMPARISON_DISCOUNT_CHART_UTILIZATION_RATE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<ComparisonChartResultVO>() {
            });
        }

        StatisticsResultV2VO<ComparisonItemVO> tableResult = getUtilizationRateTable(paramVO);

        ComparisonChartResultVO resultVO = new ComparisonChartResultVO();

        List<ComparisonChartGroupVO> resultVOList = new ArrayList<>();
        // x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        List<ComparisonItemVO> tableDataList = tableResult.getStatisticsInfoList();

        tableDataList.forEach(info -> {
            List<ComparisonDetailVO> dateList = info.getStatisticsRatioDataList();
            if (CollUtil.isEmpty(dateList)) {
                return;
            }

            Map<String, ComparisonDetailVO> timeMap = dateList.stream()
                    .filter(data -> data.getDate() != null)
                    .collect(Collectors.toMap(
                            ComparisonDetailVO::getDate,
                            data -> data,
                            (existing, replacement) -> replacement // 处理重复时间，保留后者
                    ));
            List<BigDecimal> nowList = new ArrayList<>();
            List<BigDecimal> lastList = new ArrayList<>();
            List<BigDecimal> ratioList = new ArrayList<>();

            for (String time : xdata) {
                ComparisonDetailVO detailVO = timeMap.get(time);
                nowList.add(dealBigDecimalScale(detailVO.getNow(), DEFAULT_SCALE));
                lastList.add(dealBigDecimalScale(detailVO.getPrevious(), DEFAULT_SCALE));
                ratioList.add(dealBigDecimalScale(calculateRatio(detailVO.getNow(), detailVO.getPrevious()), DEFAULT_SCALE));
            }

            List<ChartSeriesItemVO> ydata = Arrays.asList(
                    new ChartSeriesItemVO(NOW, ChartSeriesTypeEnum.BAR.getType(), nowList, null),
                    new ChartSeriesItemVO(PREVIOUS, ChartSeriesTypeEnum.BAR.getType(), lastList, null),
                    new ChartSeriesItemVO(RATIO, ChartSeriesTypeEnum.LINE.getType(), ratioList, 1)
            );

            ComparisonChartGroupVO group = new ComparisonChartGroupVO();
            group.setName(info.getEnergyName());
            group.setXdata(xdata);
            group.setYdata(ydata);
            resultVOList.add(group);

        });
        if (EnergyClassifyEnum.OUTSOURCED.getCode().equals(paramVO.getEnergyClassify())) {
            resultVO.setList(Collections.singletonList(resultVOList.get(0)));
        } else if (EnergyClassifyEnum.PARK.getCode().equals(paramVO.getEnergyClassify())) {
            resultVO.setList(Collections.singletonList(resultVOList.get(1)));
        } else {
            resultVO.setList(resultVOList);
        }

        resultVO.setList(resultVOList);
        resultVO.setDataTime(tableResult.getDataTime());

        String jsonStr = JSONUtil.toJsonStr(resultVOList);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

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

        list.add(Arrays.asList("表单名称", "统计周期", "", ""));
        String sheetName = USAGE_RATE_MOM_ALL;

        // 月份处理
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DataTypeEnum.codeOf(paramVO.getDateType()));
        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, strTime, x, NOW));
            list.add(Arrays.asList(sheetName, strTime, x, PREVIOUS));
            list.add(Arrays.asList(sheetName, strTime, x, RATIO_PERCENT));
        });
        list.add(Arrays.asList(sheetName, strTime, "周期利用率（环比）", NOW));
        list.add(Arrays.asList(sheetName, strTime, "周期利用率（环比）", PREVIOUS));
        list.add(Arrays.asList(sheetName, strTime, "周期利用率（环比）", RATIO_PERCENT));
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(StatisticsParamV2VO paramVO) {
        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();

        StatisticsResultV2VO<ComparisonItemVO> resultVO = getUtilizationRateTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<ComparisonItemVO> infoList = resultVO.getStatisticsInfoList();

        for (ComparisonItemVO s : infoList) {

            List<Object> data = ListUtils.newArrayList();

            data.add(s.getEnergyName());

            // 处理数据
            List<ComparisonDetailVO> detailVOS = s.getStatisticsRatioDataList();

            Map<String, ComparisonDetailVO> dateMap = detailVOS.stream()
                    .collect(Collectors.toMap(ComparisonDetailVO::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                ComparisonDetailVO baseDetailVO = dateMap.get(date);
                if (baseDetailVO == null) {
                    data.add(StrPool.SLASH);
                    data.add(StrPool.SLASH);
                    data.add(StrPool.SLASH);
                } else {
                    data.add(getConvertData(baseDetailVO.getNow()));
                    data.add(getConvertData(baseDetailVO.getPrevious()));
                    data.add(getConvertData(baseDetailVO.getRatio()));
                }
            });

            // 处理周期合计
            data.add(getConvertData(s.getSumNow()));
            data.add(getConvertData(s.getSumPrevious()));
            data.add(getConvertData(s.getSumRatio()));

            result.add(data);
        }

        return result;
    }

    /**
     * 构建返回数据列表
     *
     * @param nowUsageList
     * @param lastUsageList
     * @param dataTypeEnum
     * @param tableHeader
     * @param isCrossYear
     * @param valueExtractor
     * @return
     */
    private ComparisonItemVO buildComparisonItemVODataList(
            List<UsageCostData> nowUsageList,
            List<UsageCostData> lastUsageList,
            List<UsageCostData> totalList,
            DataTypeEnum dataTypeEnum,
            List<String> tableHeader,
            boolean isCrossYear,
            Function<UsageCostData, BigDecimal> valueExtractor) {
        if (CollUtil.isEmpty(nowUsageList) && CollUtil.isEmpty(lastUsageList) && CollUtil.isEmpty(totalList)) {
            return null;
        }
        if (CollUtil.isEmpty(nowUsageList)) {
            nowUsageList = Collections.emptyList();
        }
        if (CollUtil.isEmpty(lastUsageList)) {
            lastUsageList = Collections.emptyList();
        }
        // 1.处理当前
        Map<String, TimeAndNumData> nowMap = getTimeAndNumDataMap(nowUsageList, valueExtractor);
        Map<String, TimeAndNumData> previousMap = getTimeAndNumDataMap(lastUsageList, valueExtractor);


        // 构造同比详情列表
        List<ComparisonDetailVO> dataList = new ArrayList<>();
        for (String time : tableHeader) {
            // 当前
            TimeAndNumData current = nowMap.get(time);
            BigDecimal now = Optional.ofNullable(current)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            // 上期
            String previousTime = LocalDateTimeUtils.getPreviousTime(time, dataTypeEnum);
            TimeAndNumData previous = previousMap.get(previousTime);
            BigDecimal last = Optional.ofNullable(previous)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            // 环比
            BigDecimal ratio = calculateRatio(now, last);
            dataList.add(new ComparisonDetailVO(time, now, last, ratio));
        }

        // 汇总统计
        BigDecimal sumNow = dataList.stream().map(ComparisonDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null);

        BigDecimal sumPrevious = null;
        if (CollUtil.isNotEmpty(totalList)) {
            sumPrevious = totalList.stream().map(valueExtractor).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null);
        }

        BigDecimal sumRatio = calculateRatio(sumNow, sumPrevious);

        // 构造结果对象
        ComparisonItemVO vo = new ComparisonItemVO();

        dataList = dataList.stream().peek(i -> {
            i.setNow(dealBigDecimalScale(i.getNow(), DEFAULT_SCALE));
            i.setPrevious(dealBigDecimalScale(i.getPrevious(), DEFAULT_SCALE));
            i.setRatio(dealBigDecimalScale(i.getRatio(), DEFAULT_SCALE));
        }).collect(Collectors.toList());
        vo.setStatisticsRatioDataList(dataList);

        vo.setSumNow(dealBigDecimalScale(sumNow, DEFAULT_SCALE));

        // 当统计周期跨年时，周期合计列中同期、同比值无需进行计算，展示为“/”
        if (!isCrossYear) {
            vo.setSumPrevious(dealBigDecimalScale(sumPrevious, DEFAULT_SCALE));
            vo.setSumRatio(dealBigDecimalScale(sumRatio, DEFAULT_SCALE));
        }
        return vo;
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
     * 校验查看类型
     *
     * @param queryType
     */
    private Integer validateQueryType(Integer queryType) {

        QueryDimensionEnum queryDimensionEnum = QueryDimensionEnum.codeOf(queryType);
        // 查看类型不存在
        if (Objects.isNull(queryDimensionEnum)) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }

        return queryType;
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

    private void validateUnit(Integer unit) {
        if (Objects.isNull(unit)) {
            throw exception(UNIT_NOT_EMPTY);
        }
    }

    /**
     * 按能源维度统计：以 energyId 为主键，构建同比统计数据
     */
    private List<ComparisonItemVO> queryList(List<UsageCostData> outsourceList,
                                             List<UsageCostData> parkList,
                                             List<UsageCostData> numeratorList,
                                             List<UsageCostData> lastOutsourceList,
                                             List<UsageCostData> lastParkList,
                                             List<UsageCostData> lastNumeratorList,
                                             List<UsageCostData> totalOutsourceList,
                                             List<UsageCostData> totalParkList,
                                             List<UsageCostData> totalNumeratorList,
                                             boolean isCrossYear,
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
        if (CollUtil.isEmpty(lastOutsourceList)) {
            lastOutsourceList = Collections.emptyList();
        }
        if (CollUtil.isEmpty(lastParkList)) {
            lastParkList = Collections.emptyList();
        }
        if (CollUtil.isEmpty(lastNumeratorList)) {
            lastNumeratorList = Collections.emptyList();
        }

        // 当期
        Map<String, TimeAndNumData> outsourceMap = getTimeAndNumDataMap(outsourceList, UsageCostData::getTotalStandardCoalEquivalent);
        Map<String, TimeAndNumData> parkMap = getTimeAndNumDataMap(parkList, UsageCostData::getTotalStandardCoalEquivalent);
        Map<String, TimeAndNumData> numeratorMap = getTimeAndNumDataMap(numeratorList, UsageCostData::getTotalStandardCoalEquivalent);

        // 上期
        Map<String, TimeAndNumData> lastOutsourceMap = getTimeAndNumDataMap(lastOutsourceList, UsageCostData::getTotalStandardCoalEquivalent);
        Map<String, TimeAndNumData> lastParkMap = getTimeAndNumDataMap(lastParkList, UsageCostData::getTotalStandardCoalEquivalent);
        Map<String, TimeAndNumData> lastNumeratorMap = getTimeAndNumDataMap(lastNumeratorList, UsageCostData::getTotalStandardCoalEquivalent);

        List<ComparisonItemVO> result = new ArrayList<>();

        result.add(getUtilizationRateInfo(
                EnergyClassifyEnum.OUTSOURCED,
                outsourceMap,
                numeratorMap,
                lastOutsourceMap,
                lastNumeratorMap,
                totalOutsourceList,
                totalNumeratorList,
                isCrossYear,
                tableHeader));
        result.add(getUtilizationRateInfo(
                EnergyClassifyEnum.PARK,
                parkMap,
                numeratorMap,
                lastParkMap,
                lastNumeratorMap,
                totalParkList,
                totalNumeratorList,
                isCrossYear,
                tableHeader));
        return result;

    }

    private ComparisonItemVO getUtilizationRateInfo(EnergyClassifyEnum energyClassifyEnum,
                                                    Map<String, TimeAndNumData> denominatorMap,
                                                    Map<String, TimeAndNumData> numeratorMap,
                                                    Map<String, TimeAndNumData> lastDenominatorMap,
                                                    Map<String, TimeAndNumData> lastNumeratorMap,
                                                    List<UsageCostData> totalDenominatorList,
                                                    List<UsageCostData> totalNumeratorList,
                                                    boolean isCrossYear,
                                                    List<String> tableHeader) {

        List<ComparisonDetailVO> dataList = new ArrayList<>();
        for (String time : tableHeader) {

            TimeAndNumData numeratorData = numeratorMap.get(time);
            BigDecimal numeratorValue = Optional.ofNullable(numeratorData)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            TimeAndNumData denominatorData = denominatorMap.get(time);
            BigDecimal denominatorValue = Optional.ofNullable(denominatorData)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            BigDecimal nowRatio = safeDivide100(numeratorValue, denominatorValue);

            TimeAndNumData lastNumeratorData = lastNumeratorMap.get(time);
            BigDecimal lastNumeratorValue = Optional.ofNullable(lastNumeratorData)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            TimeAndNumData lastDenominatorData = lastDenominatorMap.get(time);
            BigDecimal lastDenominatorValue = Optional.ofNullable(lastDenominatorData)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            BigDecimal lastRatio = safeDivide100(lastNumeratorValue, lastDenominatorValue);

            BigDecimal ratio = calculateRatio(nowRatio, lastRatio);
            dataList.add(new ComparisonDetailVO(time, nowRatio, lastRatio, ratio));
        }

        // 汇总统计
        // 当期
        BigDecimal sumDenominator = denominatorMap.values().stream().filter(Objects::nonNull).map(TimeAndNumData::getNum).reduce(BigDecimal::add).orElse(null);
        BigDecimal sumNumerator = numeratorMap.values().stream().filter(Objects::nonNull).map(TimeAndNumData::getNum).reduce(BigDecimal::add).orElse(null);
        BigDecimal nowSumRadio = safeDivide100(sumNumerator, sumDenominator);

        // 上期
        BigDecimal totalSumDenominator = totalDenominatorList.stream().filter(Objects::nonNull).map(UsageCostData::getTotalStandardCoalEquivalent).reduce(BigDecimal::add).orElse(null);
        BigDecimal totalSumNumerator = totalNumeratorList.stream().filter(Objects::nonNull).map(UsageCostData::getTotalStandardCoalEquivalent).reduce(BigDecimal::add).orElse(null);
        BigDecimal totalSumRadio = safeDivide100(totalSumNumerator, totalSumDenominator);

        BigDecimal ratio = calculateRatio(nowSumRadio, totalSumRadio);

        // 构造结果对象
        ComparisonItemVO info = new ComparisonItemVO();
        info.setStatisticsRatioDataList(dataList);
        info.setEnergyName(energyClassifyEnum.getDetail() + UTILIZATION_RATE_STR);

        info.setSumNow(dealBigDecimalScale(nowSumRadio, DEFAULT_SCALE));
        if (!isCrossYear) {
            info.setSumPrevious(dealBigDecimalScale(totalSumRadio, DEFAULT_SCALE));
            info.setSumRatio(dealBigDecimalScale(ratio, DEFAULT_SCALE));
        }
        return info;
    }


    private StatisticsResultV2VO<ComparisonItemVO> defaultNullData(List<String> tableHeader) {
        StatisticsResultV2VO<ComparisonItemVO> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);
        List<ComparisonItemVO> infoList = new ArrayList<>();

        ComparisonItemVO osInfo = new ComparisonItemVO();
        osInfo.setStatisticsRatioDataList(Collections.emptyList());
        osInfo.setEnergyName(EnergyClassifyEnum.OUTSOURCED.getDetail() + UTILIZATION_RATE_STR);

        ComparisonItemVO parkInfo = new ComparisonItemVO();
        parkInfo.setStatisticsRatioDataList(Collections.emptyList());
        parkInfo.setEnergyName(EnergyClassifyEnum.PARK.getDetail() + UTILIZATION_RATE_STR);
        infoList.add(osInfo);
        infoList.add(parkInfo);
        resultVO.setStatisticsInfoList(infoList);
        return resultVO;
    }
}
