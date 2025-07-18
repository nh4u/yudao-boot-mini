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
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
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
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
@Slf4j
public class YoyV2ServiceImpl implements YoyV2Service {

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
    public static final String PREVIOUS = "同期";
    public static final String RATIO = "同比";

    public static final String DEFAULT_GROUP_NAME = "总";

    @Override
    public StatisticsResultV2VO<YoyItemVO> discountAnalysisTable(StatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalCost, StatisticsCacheConstants.COMPARISON_YOY_TABLE_COST);
    }

    @Override
    public StatisticsResultV2VO<YoyItemVO> foldCoalAnalysisTable(StatisticsParamV2VO paramVO) {
        return analysisTable(paramVO, UsageCostData::getTotalStandardCoalEquivalent, StatisticsCacheConstants.COMPARISON_YOY_TABLE_COAL);
    }

    public StatisticsResultV2VO<YoyItemVO> analysisTable(StatisticsParamV2VO paramVO
            , Function<UsageCostData, BigDecimal> valueExtractor, String commonType) {
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

        String cacheKey = commonType + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsResultV2VO.class);
        }

        // 构建表头
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        StatisticsResultV2VO<YoyItemVO> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);

        // 查询能源信息
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        if (CollUtil.isEmpty(energyList)) {
            resultVO.setDataTime(LocalDateTime.now());
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
        if (CollUtil.isEmpty(standingBookIds)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }

        // 查询当前周期折扣数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, startTime, endTime, standingBookIds);

        // 查询上一年周期折扣数据
        LocalDateTime[] lastRange = LocalDateTimeUtils.getSamePeriodLastYear(rangeOrigin, dataTypeEnum);
        List<UsageCostData> lastUsageCostDataList = usageCostService.getList(paramVO, lastRange[0], lastRange[1], standingBookIds);

        List<YoyItemVO> statisticsInfoList = new ArrayList<>();

        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // 按能源查看，无需构建标签分组
            statisticsInfoList.addAll(queryByEnergy(energyList, usageCostDataList, lastUsageCostDataList, dataTypeEnum, valueExtractor));
        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 按标签查看
            statisticsInfoList.addAll(queryByLabel(
                    topLabel,
                    childLabels,
                    standingbookIdsByLabel,
                    usageCostDataList,
                    lastUsageCostDataList,
                    dataTypeEnum,
                    valueExtractor));
        } else {
            // 综合默认查看
            statisticsInfoList.addAll(queryDefault(
                    topLabel,
                    childLabels,
                    standingbookIdsByLabel,
                    usageCostDataList,
                    lastUsageCostDataList,
                    dataTypeEnum,
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
     * 按能源维度统计：以 energyId 为主键，构建同比统计数据
     */
    private List<YoyItemVO> queryByEnergy(List<EnergyConfigurationDO> energyList,
                                          List<UsageCostData> usageCostDataList,
                                          List<UsageCostData> lastUsageCostDataList,
                                          DataTypeEnum dataTypeEnum,
                                          Function<UsageCostData, BigDecimal> valueExtractor) {
        // 按能源ID分组当前周期数据
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        // 同期数据以 energyId + time 为key构建map，便于查找
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
                    if (CollUtil.isEmpty(usageList)) return null;

                    // 构造同比详情数据列表
                    List<YoyDetailVO> detailList = usageList.stream()
                            .map(current -> {
                                // 使用当前时间推算同期时间来构建 key
                                String lastTime = LocalDateTimeUtils.getYearOnYearTime(current.getTime(), dataTypeEnum);
                                String key = current.getEnergyId() + "_" + lastTime;
                                UsageCostData previous = lastDataMap.get(key);
                                BigDecimal now = Optional.ofNullable(valueExtractor.apply(current)).orElse(BigDecimal.ZERO);
                                BigDecimal last = previous != null ? Optional.ofNullable(valueExtractor.apply(previous)).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
                                BigDecimal ratio = calculateYearOnYearRatio(now, last);
                                return new YoyDetailVO(current.getTime(), now, last, ratio);
                            })
                            .sorted(Comparator.comparing(YoyDetailVO::getDate))
                            .collect(Collectors.toList());

                    // 总值和同比
                    BigDecimal sumNow = detailList.stream().map(YoyDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumPrevious = detailList.stream().map(YoyDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumRatio = calculateYearOnYearRatio(sumNow, sumPrevious);

                    // 封装返回结果
                    YoyItemVO vo = new YoyItemVO();
                    vo.setEnergyId(energy.getId());
                    vo.setEnergyName(energy.getEnergyName());

                    detailList = detailList.stream().peek(i -> {
                        i.setNow(dealBigDecimalScale(i.getNow(), DEFAULT_SCALE));
                        i.setPrevious(dealBigDecimalScale(i.getPrevious(), DEFAULT_SCALE));
                        i.setRatio(dealBigDecimalScale(i.getRatio(), DEFAULT_SCALE));
                    }).collect(Collectors.toList());
                    vo.setStatisticsRatioDataList(detailList);

                    vo.setSumNow(dealBigDecimalScale(sumNow, DEFAULT_SCALE));
                    vo.setSumPrevious(dealBigDecimalScale(sumPrevious, DEFAULT_SCALE));
                    vo.setSumRatio(dealBigDecimalScale(sumRatio, DEFAULT_SCALE));
                    return vo;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<YoyItemVO> queryByLabel(String topLabel,
                                         String childLabels,
                                         List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                         List<UsageCostData> usageCostDataList,
                                         List<UsageCostData> lastUsageCostDataList,
                                         DataTypeEnum dateTypeEnum,
                                         Function<UsageCostData, BigDecimal> valueExtractor) {

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        if (CharSequenceUtil.isNotBlank(topLabel) && CharSequenceUtil.isBlank(childLabels)) {
            // 只有顶级标签
            return queryByTopLabel(usageCostDataList, lastUsageCostDataList, labelMap, topLabel, dateTypeEnum, valueExtractor);
        } else {
            // 有顶级、有子集标签
            return queryBySubLabel(standingbookIdsByLabel, usageCostDataList, lastUsageCostDataList, labelMap, dateTypeEnum, valueExtractor);
        }
    }

    private List<YoyItemVO> queryByTopLabel(List<UsageCostData> usageCostDataList,
                                            List<UsageCostData> lastUsageCostDataList,
                                            Map<Long, LabelConfigDO> labelMap,
                                            String topLabelKey,
                                            DataTypeEnum dateTypeEnum,
                                            Function<UsageCostData, BigDecimal> valueExtractor) {


        List<YoyItemVO> resultList = new ArrayList<>();

        // 1.处理当前
        List<TimeAndNumData> usageList = getTimeAndNumDataList(usageCostDataList, valueExtractor);

        // 处理上期
        Map<String, TimeAndNumData> lastMap = getTimeAndNumDataMap(lastUsageCostDataList, valueExtractor);

        // 构造同比详情列表
        List<YoyDetailVO> dataList = usageList.stream()
                .map(current -> {
                    String previousTime = LocalDateTimeUtils.getYearOnYearTime(current.getTime(), dateTypeEnum);
                    TimeAndNumData previous = lastMap.get(previousTime);
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

        Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
        LabelConfigDO topLabel = labelMap.get(topLabelId);
        info.setLabel1(topLabel.getLabelName());

        info.setLabel2("/");
        info.setLabel3("/");
        info.setLabel4("/");
        info.setLabel5("/");

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

        return resultList;
    }

    /**
     * 按标签维度统计：以 standingbookId 和标签结构为基础构建同比对比数据
     */
    private List<YoyItemVO> queryBySubLabel(List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                            List<UsageCostData> usageCostDataList,
                                            List<UsageCostData> lastUsageCostDataList,
                                            Map<Long, LabelConfigDO> labelMap,
                                            DataTypeEnum dateTypeEnum,
                                            Function<UsageCostData, BigDecimal> valueExtractor) {
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

            // 遍历二级标签组合
            labelInfoGroup.forEach((valueKey, labelInfoList) -> {
                String[] labelIds = valueKey.split(",");
                String label2Name = getLabelName(labelMap, labelIds, 0);
                String label3Name = labelIds.length > 1 ? getLabelName(labelMap, labelIds, 1) : "/";
                String label4Name = labelIds.length > 2 ? getLabelName(labelMap, labelIds, 2) : "/";
                String label5Name = labelIds.length > 3 ? getLabelName(labelMap, labelIds, 3) : "/";

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
                    String key = u.getStandingbookId() + "_" + previousTime;
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


    public List<YoyItemVO> queryDefault(String topLabel,
                                        String childLabels,
                                        List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                        List<UsageCostData> usageCostDataList,
                                        List<UsageCostData> lastUsageCostDataList,
                                        DataTypeEnum dateTypeEnum,
                                        Function<UsageCostData, BigDecimal> valueExtractor) {

        // 提取所有能源ID
        Set<Long> energyIdSet = usageCostDataList.stream().map(UsageCostData::getEnergyId).collect(Collectors.toSet());
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(energyIdSet, null);
        Map<Long, EnergyConfigurationDO> energyMap = energyList.stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        // 上期数据构建 key = standingbookId_energyId_time 的 map
        Map<String, UsageCostData> lastMap = lastUsageCostDataList.stream()
                .collect(Collectors.toMap(
                        d -> d.getStandingbookId() + "_" + d.getEnergyId() + "_" + d.getTime(),
                        Function.identity(),
                        (a, b) -> a
                ));

        // 查询所有标签配置
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));


        if (CharSequenceUtil.isNotBlank(topLabel) && CharSequenceUtil.isBlank(childLabels)) {
            // 只有顶级标签
            return queryDefaultTopLabel(usageCostDataList, lastMap, labelMap, energyMap, topLabel, dateTypeEnum, valueExtractor);
        } else {
            // 有顶级、有子集标签
            return queryDefaultSubLabel(standingbookIdsByLabel, usageCostDataList, lastMap, labelMap, energyMap, dateTypeEnum, valueExtractor);
        }
    }

    /**
     * 综合默认统计：标签 + energyId 双维度聚合构建对比数据
     */
    private List<YoyItemVO> queryDefaultTopLabel(List<UsageCostData> usageCostDataList,
                                                 Map<String, UsageCostData> lastMap,
                                                 Map<Long, LabelConfigDO> labelMap,
                                                 Map<Long, EnergyConfigurationDO> energyMap,
                                                 String topLabelKey,
                                                 DataTypeEnum dateTypeEnum,
                                                 Function<UsageCostData, BigDecimal> valueExtractor) {

        List<YoyItemVO> resultList = new ArrayList<>();

        // 当前计量器具下按 energyId 再分组
        Map<Long, List<UsageCostData>> energyUsageCostMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
        LabelConfigDO topLabel = labelMap.get(topLabelId);

        energyUsageCostMap.forEach((energyId, usageCostList) -> {
            EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);
            if (energyConfigurationDO == null) return;

            // 取数
            List<UsageCostData> previousUsageCostList = new ArrayList<>();
            // 获取上期
            usageCostList.forEach(u -> {
                String previousTime = LocalDateTimeUtils.getYearOnYearTime(u.getTime(), dateTypeEnum);
                String key = u.getStandingbookId() + "_" + energyId + "_" + previousTime;
                UsageCostData previous = lastMap.get(key);
                if (Objects.isNull(previous)) {
                    return; // 计量器具没有数据，跳过
                }
                previousUsageCostList.add(previous);
            });

            // 1.处理当前
            List<TimeAndNumData> nowList = getTimeAndNumDataList(usageCostList, valueExtractor);

            // 2.处理去年同期
            Map<String, TimeAndNumData> previousMap = getTimeAndNumDataMap(previousUsageCostList, valueExtractor);


            // 构造明细列表
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

            // 汇总
            BigDecimal sumNow = dataList.stream().map(YoyDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumPrevious = dataList.stream().map(YoyDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumRatio = calculateYearOnYearRatio(sumNow, sumPrevious);

            // 构造结果对象
            YoyItemVO info = new YoyItemVO();
            info.setEnergyId(energyId);
            info.setEnergyName(energyConfigurationDO.getEnergyName());
            info.setLabel1(topLabel.getLabelName());
            info.setLabel2("/");
            info.setLabel3("/");
            info.setLabel4("/");
            info.setLabel5("/");

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

        return resultList;
    }


    /**
     * 综合默认统计：标签 + energyId 双维度聚合构建对比数据
     */
    private List<YoyItemVO> queryDefaultSubLabel(List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                 List<UsageCostData> usageCostDataList,
                                                 Map<String, UsageCostData> lastMap,
                                                 Map<Long, LabelConfigDO> labelMap,
                                                 Map<Long, EnergyConfigurationDO> energyMap,
                                                 DataTypeEnum dateTypeEnum,
                                                 Function<UsageCostData, BigDecimal> valueExtractor) {

        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        StandingbookLabelInfoDO::getName,
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));

        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        List<YoyItemVO> resultList = new ArrayList<>();

        // 遍历一级标签分组
        grouped.forEach((topLabelKey, labelInfoGroup) -> {
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);
            if (topLabel == null) return;

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
                    if (energyConfigurationDO == null) return;

                    // 取数
                    List<UsageCostData> previousUsageCostList = new ArrayList<>();
                    // 获取上期
                    usageCostList.forEach(u -> {
                        String previousTime = LocalDateTimeUtils.getYearOnYearTime(u.getTime(), dateTypeEnum);
                        String key = u.getStandingbookId() + "_" + energyId + "_" + previousTime;
                        UsageCostData previous = lastMap.get(key);
                        if (Objects.isNull(previous)) {
                            return; // 计量器具没有数据，跳过
                        }
                        previousUsageCostList.add(previous);
                    });

                    // 1.处理当前
                    List<TimeAndNumData> nowList = getTimeAndNumDataList(usageCostList, valueExtractor);

                    // 2.处理上期
                    Map<String, TimeAndNumData> previousMap = getTimeAndNumDataMap(previousUsageCostList, valueExtractor);


                    // 构造明细列表
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

                    // 汇总
                    BigDecimal sumNow = dataList.stream().map(YoyDetailVO::getNow).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumPrevious = dataList.stream().map(YoyDetailVO::getPrevious).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumRatio = calculateYearOnYearRatio(sumNow, sumPrevious);

                    // 构造结果对象
                    YoyItemVO info = new YoyItemVO();
                    info.setEnergyId(energyId);
                    info.setEnergyName(energyConfigurationDO.getEnergyName());
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
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
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
        return "/";
    }

    /**
     * 同比率计算（避免除零）
     */
    private BigDecimal calculateYearOnYearRatio(BigDecimal now, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0 || now == null) {
            return BigDecimal.ZERO;
        }
        return now.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }


    @Override
    public ComparisonChartResultVO discountAnalysisChart(StatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalCost, StatisticsCacheConstants.COMPARISON_YOY_CHART_COST);
    }

    @Override
    public ComparisonChartResultVO foldCoalAnalysisChart(StatisticsParamV2VO paramVO) {
        return analysisChart(paramVO, UsageCostData::getTotalStandardCoalEquivalent, StatisticsCacheConstants.COMPARISON_YOY_CHART_COAL);
    }


    public ComparisonChartResultVO analysisChart(StatisticsParamV2VO paramVO
            , Function<UsageCostData, BigDecimal> valueExtractor, String commonType) {
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
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, ComparisonChartResultVO.class);
        }

        // 4. 查询能源信息及能源ID
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(
                new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        ComparisonChartResultVO result = new ComparisonChartResultVO();
        if (CollUtil.isEmpty(energyList)) {
            result.setDataTime(LocalDateTime.now());
            return result;
        }
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 5. 查询台账信息（按能源）
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        // 6. 查询标签信息（按标签过滤台账）
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(
                paramVO.getTopLabel(), paramVO.getChildLabels(), standingBookIdList);

        List<Long> standingBookIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream()
                    .filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            if (CollUtil.isEmpty(collect)) {

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
        LocalDateTime[] lastRange = LocalDateTimeUtils.getSamePeriodLastYear(rangeOrigin, dataTypeEnum);
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
     * 输出当前值、同期值、同比数据三组柱状/折线图序列
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
                BigDecimal now = nowSeries.getOrDefault(time, BigDecimal.ZERO);
                String lastTime = LocalDateTimeUtils.getYearOnYearTime(time, dataTypeEnum);
                BigDecimal previous = lastSeries.getOrDefault(lastTime, BigDecimal.ZERO);
                nowList.add(now);
                lastList.add(previous);
                ratioList.add(calculateYearOnYearRatio(now, previous));
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
     * 支持标签名还原为中文名，构建当前值、同期值、同比对比序列
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
                BigDecimal now = nowSeries.getOrDefault(time, BigDecimal.ZERO);
                String lastTime = LocalDateTimeUtils.getYearOnYearTime(time, dataTypeEnum);
                BigDecimal previous = lastSeries.getOrDefault(lastTime, BigDecimal.ZERO);
                nowList.add(now);
                lastList.add(previous);
                ratioList.add(calculateYearOnYearRatio(now, previous));
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
            BigDecimal now = nowMap.getOrDefault(time, BigDecimal.ZERO);
            String lastTime = LocalDateTimeUtils.getYearOnYearTime(time, dataTypeEnum);
            BigDecimal previous = lastMap.getOrDefault(lastTime, BigDecimal.ZERO);
            nowList.add(now);
            lastList.add(previous);
            ratioList.add(calculateYearOnYearRatio(now, previous));
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
