package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
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
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
@Slf4j
public class BaseV2ServiceImpl implements BaseV2Service {

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
    public static final String RATIO = "定基比";
    public static final String RATIO_PERCENT = "定基比(%)";

    public static final String DEFAULT_GROUP_NAME = "总";

    @Override
    public StatisticsResultV2VO<BaseItemVO> discountAnalysisTable(BaseStatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalCost, StatisticsCacheConstants.COMPARISON_BASE_TABLE_COST);
    }

    @Override
    public StatisticsResultV2VO<BaseItemVO> foldCoalAnalysisTable(BaseStatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalStandardCoalEquivalent, StatisticsCacheConstants.COMPARISON_BASE_TABLE_COAL);
    }

    public StatisticsResultV2VO<BaseItemVO> analysisTable(BaseStatisticsParamV2VO paramVO,
                                                          Function<UsageCostData, BigDecimal> valueExtractor,
                                                          String commonType) {
        // 1.校验时间范围合法性
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        Integer benchmark = paramVO.getBenchmark();

        String cacheKey = commonType + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsResultV2VO<BaseItemVO>>() {
            });
        }

        // 构建表头
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        StatisticsResultV2VO<BaseItemVO> resultVO = new StatisticsResultV2VO<>();
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

        // 查询上一年周期折扣数据
        LocalDateTime[] lastRange = LocalDateTimeUtils.getBenchmarkRange(rangeOrigin, dataTypeEnum, benchmark);
        List<UsageCostData> lastUsageCostDataList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], standingBookIds);

        List<BaseItemVO> statisticsInfoList = new ArrayList<>();
        boolean isCrossYear = DateUtils.isCrossYear(paramVO.getRange()[0], paramVO.getRange()[1]);
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // 按能源查看，无需构建标签分组
            statisticsInfoList.addAll(queryByEnergy(
                    energyList,
                    usageCostDataList,
                    lastUsageCostDataList,
                    dataTypeEnum,
                    valueExtractor,
                    tableHeader,
                    isCrossYear,
                    benchmark));
        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 按标签查看
            Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                    .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

            if (CharSequenceUtil.isNotBlank(topLabel) && CharSequenceUtil.isBlank(childLabels)) {
                // 只有顶级标签
                statisticsInfoList.addAll(queryByTopLabel(
                        usageCostDataList,
                        lastUsageCostDataList,
                        labelMap,
                        topLabel,
                        dataTypeEnum,
                        tableHeader,
                        isCrossYear,
                        valueExtractor,
                        benchmark));
            } else {
                // 有顶级、有子集标签
                statisticsInfoList.addAll(queryBySubLabel(
                        standingbookIdsByLabel,
                        usageCostDataList,
                        lastUsageCostDataList,
                        labelMap,
                        dataTypeEnum,
                        tableHeader,
                        isCrossYear,
                        valueExtractor,
                        benchmark));
            }

        } else {
            // 综合默认查看
            statisticsInfoList.addAll(queryDefault(
                    topLabel,
                    childLabels,
                    standingbookIdsByLabel,
                    usageCostDataList,
                    lastUsageCostDataList,
                    dataTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor,
                    benchmark));
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
     * 按能源维度统计：以 energyId 为主键，构建定基比统计数据
     */
    private List<BaseItemVO> queryByEnergy(List<EnergyConfigurationDO> energyList,
                                           List<UsageCostData> usageCostDataList,
                                           List<UsageCostData> lastUsageCostDataList,
                                           DataTypeEnum dataTypeEnum,
                                           Function<UsageCostData, BigDecimal> valueExtractor,
                                           List<String> tableHeader,
                                           boolean isCrossYear,
                                           Integer benchmark) {
        // 按能源ID分组当前周期数据
        Map<Long, List<UsageCostData>> nowUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        // 同期数据以 energyId + time 为key构建map，便于查找
        Map<Long, List<UsageCostData>> lastUsageMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        List<BaseItemVO> detailList = new ArrayList<>();
        energyList.forEach(energy -> {
            List<UsageCostData> nowList = nowUsageMap.get(energy.getId());
            List<UsageCostData> lastList = lastUsageMap.get(energy.getId());
            if (CollUtil.isEmpty(nowList) && CollUtil.isEmpty(lastList)) {
                return;
            }
            BaseItemVO vo = buildBaseItemVODataList(nowList, lastList, dataTypeEnum, tableHeader, isCrossYear, benchmark, valueExtractor);
            vo.setEnergyId(energy.getId());
            vo.setEnergyName(energy.getEnergyName());
            detailList.add(vo);
        });
        return detailList;
    }


    private List<BaseItemVO> queryByTopLabel(List<UsageCostData> usageCostDataList,
                                             List<UsageCostData> lastUsageCostDataList,
                                             Map<Long, LabelConfigDO> labelMap,
                                             String topLabelKey,
                                             DataTypeEnum dateTypeEnum,
                                             List<String> tableHeader,
                                             boolean isCrossYear,
                                             Function<UsageCostData, BigDecimal> valueExtractor,
                                             Integer benchmark) {

        List<BaseItemVO> resultList = new ArrayList<>();
        BaseItemVO info = buildBaseItemVODataList(usageCostDataList, lastUsageCostDataList, dateTypeEnum, tableHeader, isCrossYear, benchmark, valueExtractor);
        // 构造结果对象
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
     * 按标签维度统计：以 standingbookId 和标签结构为基础构建定基比对比数据
     */
    private List<BaseItemVO> queryBySubLabel(List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                             List<UsageCostData> usageCostDataList,
                                             List<UsageCostData> lastUsageCostDataList,
                                             Map<Long, LabelConfigDO> labelMap,
                                             DataTypeEnum dateTypeEnum,
                                             List<String> tableHeader,
                                             boolean isCrossYear,
                                             Function<UsageCostData, BigDecimal> valueExtractor,
                                             Integer benchmark) {
        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        StandingbookLabelInfoDO::getName,
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));
        // 当前周期数据按 standingbookId 分组
        Map<Long, List<UsageCostData>> currentMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        // 同期数据以 standingbookId + time 为key 构建map
        Map<Long, List<UsageCostData>> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));


        List<BaseItemVO> resultList = new ArrayList<>();

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

                // 获取本期标签关联的台账id，并取到对应的数据
                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = currentMap.get(labelInfo.getStandingbookId());
                    if (usageList != null) {
                        labelUsageListNow.addAll(usageList);
                    }
                    List<UsageCostData> usageList1 = lastMap.get(labelInfo.getStandingbookId());
                    if (usageList1 != null) {
                        labelUsageListPrevious.addAll(usageList1);
                    }
                });

                // 构造结果对象
                BaseItemVO info = buildBaseItemVODataList(labelUsageListNow, labelUsageListPrevious, dateTypeEnum, tableHeader, isCrossYear, benchmark, valueExtractor);
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

    public List<BaseItemVO> queryDefault(String topLabel,
                                         String childLabels,
                                         List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                         List<UsageCostData> usageCostDataList,
                                         List<UsageCostData> lastUsageCostDataList,
                                         DataTypeEnum dateTypeEnum,
                                         List<String> tableHeader,
                                         boolean isCrossYear,
                                         Function<UsageCostData, BigDecimal> valueExtractor,
                                         Integer benchmark) {

        // 实际用到的能源ids
        Set<Long> energyIdSet = Stream.concat(
                usageCostDataList.stream().map(UsageCostData::getEnergyId),
                lastUsageCostDataList.stream().map(UsageCostData::getEnergyId)
        ).collect(Collectors.toSet());

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
                    labelMap,
                    energyMap,
                    topLabel,
                    dateTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor,
                    benchmark);
        } else {
            // 有顶级、有子集标签
            return queryDefaultSubLabel(
                    standingbookIdsByLabel,
                    usageCostDataList,
                    lastUsageCostDataList,
                    labelMap,
                    energyMap,
                    dateTypeEnum,
                    tableHeader,
                    isCrossYear,
                    valueExtractor,
                    benchmark);
        }
    }

    private List<BaseItemVO> queryDefaultTopLabel(List<UsageCostData> usageCostDataList,
                                                  List<UsageCostData> lastUsageCostDataList,
                                                  Map<Long, LabelConfigDO> labelMap,
                                                  Map<Long, EnergyConfigurationDO> energyMap,
                                                  String topLabelKey,
                                                  DataTypeEnum dateTypeEnum,
                                                  List<String> tableHeader,
                                                  boolean isCrossYear,
                                                  Function<UsageCostData, BigDecimal> valueExtractor,
                                                  Integer benchmark) {

        // 按能源ID分组当前周期数据
        Map<Long, List<UsageCostData>> nowUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
        Map<Long, List<UsageCostData>> lastUsageMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        List<BaseItemVO> resultList = new ArrayList<>();

        Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
        LabelConfigDO topLabel = labelMap.get(topLabelId);

        energyMap.forEach((energyId, energyConfigurationDO) -> {

            // 构造结果对象
            BaseItemVO info = buildBaseItemVODataList(
                    nowUsageMap.get(energyId),
                    lastUsageMap.get(energyId),
                    dateTypeEnum,
                    tableHeader,
                    isCrossYear,
                    benchmark,
                    valueExtractor);

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
    private List<BaseItemVO> queryDefaultSubLabel(List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                  List<UsageCostData> usageCostDataList,
                                                  List<UsageCostData> lastUsageCostDataList,
                                                  Map<Long, LabelConfigDO> labelMap,
                                                  Map<Long, EnergyConfigurationDO> energyMap,
                                                  DataTypeEnum dateTypeEnum,
                                                  List<String> tableHeader,
                                                  boolean isCrossYear,
                                                  Function<UsageCostData, BigDecimal> valueExtractor,
                                                  Integer benchmark) {
        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        StandingbookLabelInfoDO::getName,
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));

        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageNowMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        Map<Long, List<UsageCostData>> standingBookUsagePrevMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));


        List<BaseItemVO> resultList = new ArrayList<>();

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

                });

                // 按能源ID分组当前周期数据
                Map<Long, List<UsageCostData>> energyUsageCostNowMap = new HashMap<>();

                if (CollUtil.isNotEmpty(labelUsageCostDataNowList)) {
                    energyUsageCostNowMap = labelUsageCostDataNowList
                            .stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
                }

                // 按能源ID分组上期数据
                Map<Long, List<UsageCostData>> energyUsageCostPrevMap = new HashMap<>();
                if (CollUtil.isNotEmpty(labelUsageCostDataPrevList)) {
                    energyUsageCostPrevMap = labelUsageCostDataPrevList
                            .stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));
                }

                Map<Long, List<UsageCostData>> finalEnergyUsageCostNowMap = energyUsageCostNowMap;
                Map<Long, List<UsageCostData>> finalEnergyUsageCostPrevMap = energyUsageCostPrevMap;

                energyMap.forEach((energyId, energyConfigurationDO) -> {
                    if (energyConfigurationDO == null) return;

                    // 构造结果对象
                    BaseItemVO info = buildBaseItemVODataList(
                            finalEnergyUsageCostNowMap.get(energyId),
                            finalEnergyUsageCostPrevMap.get(energyId),
                            dateTypeEnum,
                            tableHeader,
                            isCrossYear,
                            benchmark,
                            valueExtractor);

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


    @Override
    public ComparisonChartResultVO discountAnalysisChart(BaseStatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalCost, StatisticsCacheConstants.COMPARISON_BASE_CHART_COST);
    }

    @Override
    public ComparisonChartResultVO foldCoalAnalysisChart(BaseStatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalStandardCoalEquivalent, StatisticsCacheConstants.COMPARISON_BASE_CHART_COAL);
    }


    public ComparisonChartResultVO analysisChart(BaseStatisticsParamV2VO paramVO,
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
        LocalDateTime[] lastRange = LocalDateTimeUtils.getBenchmarkRange(rangeOrigin, dataTypeEnum, paramVO.getBenchmark());
        List<UsageCostData> lastUsageCostDataList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], standingBookIds);

        // 8. 构建横轴时间（xdata）
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);
        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, startTime, endTime, standingBookIds);

        // 9. 根据维度类型进行聚合图表构建
        Integer queryType = paramVO.getQueryType();
        List<ComparisonChartGroupVO> groupList;

        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            groupList = buildChartByEnergy(energyList, usageCostDataList, lastUsageCostDataList, xdata, dataTypeEnum, valueExtractor, paramVO.getBenchmark());
        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            groupList = buildChartByLabel(standingbookIdsByLabel, standingBookIds, usageCostDataList, lastUsageCostDataList, xdata, dataTypeEnum, valueExtractor, paramVO.getBenchmark());
        } else {
            groupList = buildChartByDefault(usageCostDataList, lastUsageCostDataList, xdata, dataTypeEnum, valueExtractor, paramVO.getBenchmark());
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
     * 输出当前值、同期值、定基比数据三组柱状/折线图序列
     */
    private List<ComparisonChartGroupVO> buildChartByEnergy(List<EnergyConfigurationDO> energyList,
                                                            List<UsageCostData> usageCostDataList,
                                                            List<UsageCostData> lastUsageCostDataList,
                                                            List<String> xdata,
                                                            DataTypeEnum dataTypeEnum,
                                                            Function<UsageCostData, BigDecimal> valueExtractor,
                                                            Integer benchmark) {
        // 构建当前周期和上周期数据的 (energyId -> time -> cost) 映射
        Map<Long, Map<String, BigDecimal>> nowMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getEnergyId,
                        Collectors.toMap(UsageCostData::getTime, valueExtractor, BigDecimal::add)));

        Map<Long, Map<String, BigDecimal>> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getEnergyId,
                        Collectors.toMap(UsageCostData::getTime, valueExtractor, BigDecimal::add)));

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
                String lastTime = LocalDateTimeUtils.getBenchmarkTime(time, dataTypeEnum, benchmark);
                BigDecimal previous = lastSeries.get(lastTime);
                nowList.add(dealBigDecimalScale(now, DEFAULT_SCALE));
                lastList.add(dealBigDecimalScale(previous, DEFAULT_SCALE));
                ratioList.add(dealBigDecimalScale(calculateBaseRatio(now, previous), DEFAULT_SCALE));
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
     * 支持标签名还原为中文名，构建当前值、同期值、定基比对比序列
     */
    private List<ComparisonChartGroupVO> buildChartByLabel(List<StandingbookLabelInfoDO> labelList,
                                                           List<Long> validStandingbookIds,
                                                           List<UsageCostData> usageCostDataList,
                                                           List<UsageCostData> lastUsageCostDataList,
                                                           List<String> xdata,
                                                           DataTypeEnum dataTypeEnum,
                                                           Function<UsageCostData, BigDecimal> valueExtractor,
                                                           Integer benchmark) {
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
                    .merge(data.getTime(), valueExtractor.apply(data), BigDecimal::add);
        }

        // 构造 (labelKey -> time -> cost) 的二维映射（上周期）
        Map<String, Map<String, BigDecimal>> lastMap = new HashMap<>();
        for (UsageCostData data : lastUsageCostDataList) {
            String label = standingbookLabelMap.get(data.getStandingbookId());
            if (label == null) continue;
            lastMap.computeIfAbsent(label, k -> new HashMap<>())
                    .merge(data.getTime(), valueExtractor.apply(data), BigDecimal::add);
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
                String lastTime = LocalDateTimeUtils.getBenchmarkTime(time, dataTypeEnum, benchmark);
                BigDecimal previous = lastSeries.get(lastTime);
                nowList.add(dealBigDecimalScale(now, DEFAULT_SCALE));
                lastList.add(dealBigDecimalScale(previous, DEFAULT_SCALE));
                ratioList.add(dealBigDecimalScale(calculateBaseRatio(now, previous), DEFAULT_SCALE));
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
                                                             Function<UsageCostData, BigDecimal> valueExtractor,
                                                             Integer benchmark) {
        // 当前周期与上周期：时间 -> 总和映射
        Map<String, BigDecimal> nowMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getTime,
                        Collectors.mapping(valueExtractor,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        Map<String, BigDecimal> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getTime,
                        Collectors.mapping(valueExtractor,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        List<BigDecimal> nowList = new ArrayList<>();
        List<BigDecimal> lastList = new ArrayList<>();
        List<BigDecimal> ratioList = new ArrayList<>();

        for (String time : xdata) {
            BigDecimal now = nowMap.get(time);
            String lastTime = LocalDateTimeUtils.getBenchmarkTime(time, dataTypeEnum, benchmark);
            BigDecimal previous = lastMap.get(lastTime);
            nowList.add(dealBigDecimalScale(now, DEFAULT_SCALE));
            lastList.add(dealBigDecimalScale(previous, DEFAULT_SCALE));
            ratioList.add(dealBigDecimalScale(calculateBaseRatio(now, previous), DEFAULT_SCALE));
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
    public List<List<String>> getExcelHeader(BaseStatisticsParamV2VO paramVO, Integer flag) {

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
                    sheetName = STANDARD_COAL_BENCHMARK_ALL;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                    for (int i = 2; i <= labelDeep; i++) {
                        String subLabel = "标签" + i;
                        list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                    }
                    list.add(Arrays.asList(sheetName, labelName, strTime, "能源", "能源"));
                    break;
                case 1:
                    // 按能源
                    sheetName = STANDARD_COAL_BENCHMARK_ENERGY;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "能源", "能源"));
                    break;
                case 2:
                    // 按标签
                    sheetName = STANDARD_COAL_BENCHMARK_LABEL;
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
            // 折价
            switch (queryType) {
                case 0:
                    // 综合
                    sheetName = COST_BENCHMARK_ALL;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                    for (int i = 2; i <= labelDeep; i++) {
                        String subLabel = "标签" + i;
                        list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                    }
                    list.add(Arrays.asList(sheetName, labelName, strTime, "能源", "能源"));
                    break;
                case 1:
                    // 按能源
                    sheetName = COST_BENCHMARK_ENERGY;
                    list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "能源", "能源"));
                    break;
                case 2:
                    // 按标签
                    sheetName = COST_BENCHMARK_LABEL;
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
    public List<List<Object>> getExcelData(BaseStatisticsParamV2VO paramVO, Integer flag) {
        // 验证单位
        Integer unit = paramVO.getUnit();

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        StatisticsResultV2VO<BaseItemVO> resultVO;
        if (flag == 1) {
            // 折标煤
            resultVO = foldCoalAnalysisTable(paramVO);
        } else {
            // 折价
            resultVO = discountAnalysisTable(paramVO);
        }
        List<String> tableHeader = resultVO.getHeader();

        List<BaseItemVO> baseItemVOList = resultVO.getStatisticsInfoList();
        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);

        Integer queryType = paramVO.getQueryType();

        // 底部合计map
        Map<String, BigDecimal> sumNowMap = new HashMap<>();
        Map<String, BigDecimal> sumPreviousMap = new HashMap<>();
        Map<String, BigDecimal> sumProportionMap = new HashMap<>();

        for (BaseItemVO s : baseItemVOList) {

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
            List<BaseDetailVO> statisticsRatioDataList = s.getStatisticsRatioDataList();

            Map<String, BaseDetailVO> dateMap = statisticsRatioDataList.stream()
                    .collect(Collectors.toMap(BaseDetailVO::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                BaseDetailVO comparisonDetailVO = dateMap.get(date);
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
                    sumProportionMap.put(date, addBigDecimal(sumProportionMap.get(date), proportion));
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
            sumProportionMap.put("sumNum", addBigDecimal(sumProportionMap.get("sumNum"), sumProportion));
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
            // 定基比
            BigDecimal proportion = sumProportionMap.get(date);
            bottom.add(getConvertData(proportion));
        });

        // 底部周期合计
        // 当期
        BigDecimal sumNow = sumNowMap.get("sumNum");
        bottom.add(getConvertData(unit, flag, sumNow));
        // 同期
        BigDecimal sumPrevious = sumPreviousMap.get("sumNum");
        bottom.add(getConvertData(unit, flag, sumPrevious));
        // 定基比
        BigDecimal proportion = sumProportionMap.get("sumNum");
        bottom.add(getConvertData(proportion));

        result.add(bottom);

        return result;
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

    private void validateUnit(Integer unit) {
        if (Objects.isNull(unit)) {
            throw exception(UNIT_NOT_EMPTY);
        }
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
    private BaseItemVO buildBaseItemVODataList(
            List<UsageCostData> nowUsageList,
            List<UsageCostData> lastUsageList,
            DataTypeEnum dataTypeEnum,
            List<String> tableHeader,
            boolean isCrossYear,
            Integer benchmark,
            Function<UsageCostData, BigDecimal> valueExtractor) {
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
        List<BaseDetailVO> dataList = new ArrayList<>();
        for (String time : tableHeader) {

            // 当前
            TimeAndNumData current = nowMap.get(time);
            BigDecimal now = Optional.ofNullable(current)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            // 上期
            String previousTime = LocalDateTimeUtils.getBenchmarkTime(time, dataTypeEnum, benchmark);
            TimeAndNumData previous = previousMap.get(previousTime);
            BigDecimal last = Optional.ofNullable(previous)
                    .map(TimeAndNumData::getNum)
                    .orElse(null);

            // 定基比
            BigDecimal ratio = calculateBaseRatio(now, last);
            dataList.add(new BaseDetailVO(time, now, last, ratio));
        }

        // 汇总统计
        BigDecimal sumNow = dataList.stream().map(BaseDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null);
        BigDecimal sumPrevious = dataList.stream().map(BaseDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null);
        BigDecimal sumRatio = calculateBaseRatio(sumNow, sumPrevious);
        // 构造结果对象
        BaseItemVO vo = new BaseItemVO();
//        vo.setEnergyId(energy.getId());
//        vo.setEnergyName(energy.getEnergyName());

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
}
