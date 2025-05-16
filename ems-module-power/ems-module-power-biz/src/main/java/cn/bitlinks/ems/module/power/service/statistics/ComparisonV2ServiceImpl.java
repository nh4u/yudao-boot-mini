package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
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
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
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

    public static final String NOW = "now";
    public static final String PREVIOUS = "previous";
    public static final String RATIO = "ratio";

    public static final String DEFAULT_GROUP_NAME = "总";

    @Override
    public StatisticsResultV2VO<ComparisonItemVO> discountAnalysisTable(StatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalCost);
    }

    @Override
    public StatisticsResultV2VO<ComparisonItemVO> foldCoalAnalysisTable(StatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalCost);
    }

    public StatisticsResultV2VO<ComparisonItemVO> analysisTable(StatisticsParamV2VO paramVO, Function<UsageCostData, BigDecimal> valueExtractor) {
        // 校验时间范围合法性
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 获取查询维度类型和时间类型
        Integer queryType = paramVO.getQueryType();
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        String cacheKey = StatisticsCacheConstants.COMPARISON_DISCOUNT_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if(StrUtil.isNotEmpty(cacheRes)){
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsResultV2VO.class);
        }

        // 构建表头
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        StatisticsResultV2VO<ComparisonItemVO> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);

        // 查询能源信息
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 查询台账信息（先按能源）
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        // 查询标签信息（按标签过滤台账）
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(paramVO.getTopLabel(), paramVO.getChildLabels(), standingBookIdList);

        List<Long> standingBookIds = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream().filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            if (ArrayUtil.isEmpty(collect)) {
                resultVO.setDataTime(LocalDateTime.now());
                return resultVO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            // 如果标签为空则使用能源台账全量
            standingBookIds.addAll(standingBookIdList);
        }

        // 无台账数据直接返回
        if (CollectionUtil.isEmpty(standingBookIds)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }

        // 查询当前周期折扣数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, startTime, endTime, standingBookIds);

        // 查询上一个周期折扣数据
        LocalDateTime[] lastRange = LocalDateTimeUtils.getPreviousRange(rangeOrigin, dataTypeEnum);
        List<UsageCostData> lastUsageCostDataList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], standingBookIds);

        List<ComparisonItemVO> statisticsInfoList = new ArrayList<>();

        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);

        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // 按能源查看，无需构建标签分组
            statisticsInfoList.addAll(queryByEnergy(energyList, usageCostDataList, lastUsageCostDataList, dataTypeEnum, valueExtractor));
        } else {
            // 构建标签分组结构：一级标签名 -> 二级/三级值 -> 对应标签列表
            Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                    .filter(s -> standingBookIds.contains(s.getStandingbookId()))
                    .collect(Collectors.groupingBy(
                            StandingbookLabelInfoDO::getName,
                            Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));

            if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
                // 按标签查看
                statisticsInfoList.addAll(queryByLabel(grouped, usageCostDataList, lastUsageCostDataList, dataTypeEnum, valueExtractor));
            } else {
                // 综合默认查看
                statisticsInfoList.addAll(queryDefault(grouped, usageCostDataList, lastUsageCostDataList, dataTypeEnum, valueExtractor));
            }
        }

        // 设置最终返回值
        resultVO.setStatisticsInfoList(statisticsInfoList);
        resultVO.setDataTime(lastTime);
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
                                                 DataTypeEnum dataTypeEnum,
                                                 Function<UsageCostData, BigDecimal> valueExtractor) {
        // 按能源ID分组当前周期数据
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        // 上期数据以 energyId + time 为key构建map，便于查找
        Map<String, UsageCostData> lastDataMap = lastUsageCostDataList.stream()
                .collect(Collectors.toMap(
                        d -> d.getEnergyId() + "_" + d.getTime(),
                        Function.identity(),
                        (a, b) -> a
                ));

        return energyList.stream()
                .filter(energy -> energyUsageMap.containsKey(energy.getId()))
                .map(energy -> {
                    List<UsageCostData> usageList = energyUsageMap.get(energy.getId());
                    if (CollectionUtil.isEmpty(usageList)) return null;

                    // 构造环比详情数据列表
                    List<ComparisonDetailVO> detailList = usageList.stream()
                            .map(current -> {
                                // 使用当前时间推算上期时间来构建 key
                                String lastTime = LocalDateTimeUtils.getPreviousTime(current.getTime(), dataTypeEnum);
                                String key = current.getEnergyId() + "_" + lastTime;
                                UsageCostData previous = lastDataMap.get(key);
                                BigDecimal now = valueExtractor.apply(current);
                                BigDecimal last = previous != null ? valueExtractor.apply(previous) : null;
                                BigDecimal ratio = calculateRatio(now, last);
                                return new ComparisonDetailVO(current.getTime(), now, last, ratio);
                            })
                            .sorted(Comparator.comparing(ComparisonDetailVO::getDate))
                            .collect(Collectors.toList());

                    // 总值和环比
                    BigDecimal sumNow = detailList.stream().map(ComparisonDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumPrevious = detailList.stream().map(ComparisonDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumRatio = calculateRatio(sumNow, sumPrevious);

                    // 封装返回结果
                    ComparisonItemVO vo = new ComparisonItemVO();
                    vo.setEnergyId(energy.getId());
                    vo.setEnergyName(energy.getName());
                    vo.setStatisticsRatioDataList(detailList);
                    vo.setSumNow(sumNow);
                    vo.setSumPrevious(sumPrevious);
                    vo.setSumRatio(sumRatio);
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 按标签维度统计：以 standingbookId 和标签结构为基础构建环比对比数据
     */
    private List<ComparisonItemVO> queryByLabel(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
                                                List<UsageCostData> usageCostDataList,
                                                List<UsageCostData> lastUsageCostDataList,
                                                DataTypeEnum dateTypeEnum,
                                                Function<UsageCostData, BigDecimal> valueExtractor) {
        // 当前周期数据按 standingbookId 分组
        Map<Long, List<UsageCostData>> currentMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        // 上期数据以 standingbookId + time 为key 构建map
        Map<String, UsageCostData> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.toMap(
                        d -> d.getStandingbookId() + "_" + d.getTime(),
                        Function.identity(),
                        (a, b) -> a
                ));

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

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
                String label3Name = labelIds.length > 1 ? getLabelName(labelMap, labelIds, 1) : "/";

                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = currentMap.get(labelInfo.getStandingbookId());
                    if (CollectionUtil.isEmpty(usageList)) return;

                    // 构造环比详情列表
                    List<ComparisonDetailVO> dataList = usageList.stream()
                            .map(current -> {
                                String previousTime = LocalDateTimeUtils.getPreviousTime(current.getTime(), dateTypeEnum);
                                String key = current.getStandingbookId() + "_" + previousTime;
                                UsageCostData previous = lastMap.get(key);
                                BigDecimal now = valueExtractor.apply(current);
                                BigDecimal last = previous != null ? valueExtractor.apply(previous) : null;
                                BigDecimal ratio = calculateRatio(now, last);
                                return new ComparisonDetailVO(current.getTime(), now, last, ratio);
                            })
                            .sorted(Comparator.comparing(ComparisonDetailVO::getDate))
                            .collect(Collectors.toList());

                    // 汇总统计
                    BigDecimal sumNow = dataList.stream().map(ComparisonDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumPrevious = dataList.stream().map(ComparisonDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumRatio = calculateRatio(sumNow, sumPrevious);

                    // 构造结果对象
                    ComparisonItemVO info = new ComparisonItemVO();
                    info.setLabel1(topLabel.getLabelName());
                    info.setLabel2(label2Name);
                    info.setLabel3(label3Name);
                    info.setStatisticsRatioDataList(dataList);
                    info.setSumNow(sumNow);
                    info.setSumPrevious(sumPrevious);
                    info.setSumRatio(sumRatio);

                    resultList.add(info);
                });
            });
        });

        return resultList;
    }

    /**
     * 综合默认统计：标签 + energyId 双维度聚合构建对比数据
     */
    private List<ComparisonItemVO> queryDefault(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
                                                List<UsageCostData> usageCostDataList,
                                                List<UsageCostData> lastUsageCostDataList,
                                                DataTypeEnum dateTypeEnum,
                                                Function<UsageCostData, BigDecimal> valueExtractor) {
        // 提取所有能源ID
        Set<Long> energyIdSet = usageCostDataList.stream().map(UsageCostData::getEnergyId).collect(Collectors.toSet());
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(energyIdSet, null);
        Map<Long, EnergyConfigurationDO> energyMap = energyList.stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        // 查询所有标签配置
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        // 当前周期数据按 standingbookId 分组
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        // 上期数据构建 key = standingbookId_energyId_time 的 map
        Map<String, UsageCostData> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.toMap(
                        d -> d.getStandingbookId() + "_" + d.getEnergyId() + "_" + d.getTime(),
                        Function.identity(),
                        (a, b) -> a
                ));

        List<ComparisonItemVO> resultList = new ArrayList<>();

        // 遍历一级标签分组
        grouped.forEach((topLabelKey, labelInfoGroup) -> {
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);
            if (topLabel == null) return;

            labelInfoGroup.forEach((valueKey, labelInfoList) -> {
                String[] labelIds = valueKey.split(",");
                String label2Name = getLabelName(labelMap, labelIds, 0);
                String label3Name = labelIds.length > 1 ? getLabelName(labelMap, labelIds, 1) : "/";

                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = energyUsageMap.get(labelInfo.getStandingbookId());
                    if (CollectionUtil.isEmpty(usageList)) return;

                    // 当前计量器具下按 energyId 再分组
                    Map<Long, List<UsageCostData>> energyUsageCostMap = usageList.stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

                    energyUsageCostMap.forEach((energyId, usageCostList) -> {
                        EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);
                        if (energyConfigurationDO == null) return;

                        // 构造明细列表
                        List<ComparisonDetailVO> dataList = usageCostList.stream()
                                .map(current -> {
                                    String previousTime = LocalDateTimeUtils.getPreviousTime(current.getTime(), dateTypeEnum);
                                    String key = current.getStandingbookId() + "_" + energyId + "_" + previousTime;
                                    UsageCostData previous = lastMap.get(key);
                                    BigDecimal now = valueExtractor.apply(current);
                                    BigDecimal last = previous != null ? valueExtractor.apply(previous) : null;
                                    BigDecimal ratio = calculateRatio(now, last);
                                    return new ComparisonDetailVO(current.getTime(), now, last, ratio);
                                })
                                .sorted(Comparator.comparing(ComparisonDetailVO::getDate))
                                .collect(Collectors.toList());

                        // 汇总
                        BigDecimal sumNow = dataList.stream().map(ComparisonDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal sumPrevious = dataList.stream().map(ComparisonDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal sumRatio = calculateRatio(sumNow, sumPrevious);

                        // 构造结果对象
                        ComparisonItemVO info = new ComparisonItemVO();
                        info.setEnergyId(energyId);
                        info.setEnergyName(energyConfigurationDO.getEnergyName());
                        info.setLabel1(topLabel.getLabelName());
                        info.setLabel2(label2Name);
                        info.setLabel3(label3Name);
                        info.setStatisticsRatioDataList(dataList);
                        info.setSumNow(sumNow);
                        info.setSumPrevious(sumPrevious);
                        info.setSumRatio(sumRatio);

                        resultList.add(info);
                    });
                });
            });
        });

        return resultList;
    }

    /**
     * 获取标签名
     * @param labelMap 标签配置map
     * @param labelIds 标签id数组
     * @param index 标签索引
     */
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
        return analysisChart(paramVO, UsageCostData::getTotalCost);
    }

    @Override
    public ComparisonChartResultVO foldCoalAnalysisChart(StatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalStandardCoalEquivalent);
    }


    public ComparisonChartResultVO analysisChart(StatisticsParamV2VO paramVO,Function<UsageCostData, BigDecimal> valueExtractor) {
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
        String cacheKey = StatisticsCacheConstants.COMPARISON_DISCOUNT_CHART + SecureUtil.md5(paramVO.toString());
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
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        // 6. 查询标签信息（按标签过滤台账）
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(
                paramVO.getTopLabel(), paramVO.getChildLabels(), standingBookIdList);

        List<Long> standingBookIds = new ArrayList<>();
        if (CollectionUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream()
                    .filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            if (CollectionUtil.isEmpty(collect)) {
                ComparisonChartResultVO result = new ComparisonChartResultVO();
                result.setDataTime(LocalDateTime.now());
                result.setList(Collections.emptyList());
                return result;
            }
            standingBookIds.addAll(collect.stream().map(StandingbookDO::getId).collect(Collectors.toList()));
        } else {
            standingBookIds.addAll(standingBookIdList);
        }

        // 7. 查询当前周期与上周期的折扣数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, startTime, endTime, standingBookIds);
        LocalDateTime[] lastRange = LocalDateTimeUtils.getPreviousRange(rangeOrigin, dataTypeEnum);
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
        ComparisonChartResultVO result = new ComparisonChartResultVO();
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
                BigDecimal now = nowSeries.getOrDefault(time, null);
                String lastTime = LocalDateTimeUtils.getPreviousTime(time, dataTypeEnum);
                BigDecimal previous = lastSeries.getOrDefault(lastTime, null);
                nowList.add(now);
                lastList.add(previous);
                ratioList.add(calculateRatio(now, previous));
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
                BigDecimal now = nowSeries.getOrDefault(time, null);
                String lastTime = LocalDateTimeUtils.getPreviousTime(time, dataTypeEnum);
                BigDecimal previous = lastSeries.getOrDefault(lastTime, null);
                nowList.add(now);
                lastList.add(previous);
                ratioList.add(calculateRatio(now, previous));
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
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        Map<String, BigDecimal> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getTime,
                        Collectors.mapping(valueExtractor,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        List<BigDecimal> nowList = new ArrayList<>();
        List<BigDecimal> lastList = new ArrayList<>();
        List<BigDecimal> ratioList = new ArrayList<>();

        for (String time : xdata) {
            BigDecimal now = nowMap.getOrDefault(time, null);
            String lastTime = LocalDateTimeUtils.getPreviousTime(time, dataTypeEnum);
            BigDecimal previous = lastMap.getOrDefault(lastTime, null);
            nowList.add(now);
            lastList.add(previous);
            ratioList.add(calculateRatio(now, previous));
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

}
