package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.pojo.StatsResult;
import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.dealStrTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.LABEL_NAME_PREFIX;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.USAGE_STANDARD_COAL_CHART;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.USAGE_STANDARD_COAL_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

/**
 * @Title: ydme-doublecarbon
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/14 17:10
 **/
@Service
@Validated
@Slf4j
public class StandardCoalV2ServiceImpl implements StandardCoalV2Service {

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

    @Override
    public StatisticsResultV2VO<StandardCoalInfo> standardCoalAnalysisTable(StatisticsParamV2VO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_STANDARD_COAL_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsResultV2VO.class);
        }

        // 4.如果没有则去数据库查询
        StatisticsResultV2VO<StandardCoalInfo> resultVO = new StatisticsResultV2VO<>();
        resultVO.setDataTime(LocalDateTime.now());

        // 4.1.表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultVO.setHeader(tableHeader);

        // 4.2.能源id处理
        List<EnergyConfigurationDO> energyList = energyConfigurationService
                .getByEnergyClassify(
                        CollUtil.isNotEmpty(paramVO.getEnergyIds()) ? new HashSet<>(paramVO.getEnergyIds()) : new HashSet<>(),
                        paramVO.getEnergyClassify());
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

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


        List<StandardCoalInfo> statisticsInfoList = new ArrayList<>();
        // 1、按能源查看
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            List<StandardCoalInfo> standardCoalInfos = queryByEnergy(energyList, usageCostDataList);
            statisticsInfoList.addAll(standardCoalInfos);

        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 2、按标签查看
            List<StandardCoalInfo> standardCoalInfos = queryByLabel(topLabel, childLabels, standingbookIdsByLabel, usageCostDataList);
            statisticsInfoList.addAll(standardCoalInfos);

        } else {
            // 0、综合查看（默认）
            List<StandardCoalInfo> statisticsInfoV2s = queryDefault(topLabel, childLabels, standingbookIdsByLabel, usageCostDataList);
            statisticsInfoList.addAll(statisticsInfoV2s);
        }

        resultVO.setStatisticsInfoList(statisticsInfoList);

        statisticsInfoList.forEach(l -> {

            List<StandardCoalInfoData> newList = new ArrayList<>();
            List<StandardCoalInfoData> oldList = l.getStandardCoalInfoDataList();
            if (tableHeader.size() != oldList.size()) {
                Map<String, List<StandardCoalInfoData>> dateMap = oldList.stream()
                        .collect(Collectors.groupingBy(StandardCoalInfoData::getDate));

                tableHeader.forEach(date -> {
                    List<StandardCoalInfoData> standardCoalInfoDataList = dateMap.get(date);
                    if (standardCoalInfoDataList == null) {
                        StandardCoalInfoData standardCoalInfoData = new StandardCoalInfoData();
                        standardCoalInfoData.setDate(date);
                        standardCoalInfoData.setStandardCoal(BigDecimal.ZERO);
                        standardCoalInfoData.setConsumption(BigDecimal.ZERO);
                        newList.add(standardCoalInfoData);
                    } else {
                        newList.add(standardCoalInfoDataList.get(0));
                    }
                });

                l.setStandardCoalInfoDataList(newList);
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

    @Override
    public StatisticsChartResultV2VO standardCoalAnalysisChart(StatisticsParamV2VO paramVO) {
        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_STANDARD_COAL_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsChartResultV2VO.class);
        }

        // 4.如果没有则去数据库查询
        StatisticsChartResultV2VO resultV2VO = new StatisticsChartResultV2VO();
        resultV2VO.setDataTime(LocalDateTime.now());

        // 4.1.x轴处理
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultV2VO.setXdata(xdata);

        // 4.2.能源id处理
        List<EnergyConfigurationDO> energyList = energyConfigurationService
                .getByEnergyClassify(
                        CollUtil.isNotEmpty(paramVO.getEnergyIds()) ? new HashSet<>(paramVO.getEnergyIds()) : new HashSet<>(),
                        paramVO.getEnergyClassify());
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());


        // 4.3.台账id处理
        List<Long> standingBookIds = new ArrayList<>();
        // 4.3.1.根据能源id查询台账
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIdList = standingbookIdsByEnergy
                .stream()
                .map(StandingbookDO::getId)
                .collect(Collectors.toList());

        // 4.3.2.根据标签id查询
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService
                .getStandingbookIdsByLabel(paramVO.getTopLabel(), paramVO.getChildLabels(), standingBookIdList);

        // 4.3.3.能源台账ids和标签台账ids是否有交集。如果有就取交集，如果没有则取能源台账ids
        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel
                    .stream()
                    .map(StandingbookLabelInfoDO::getStandingbookId)
                    .collect(Collectors.toList());

            List<StandingbookDO> collect = standingbookIdsByEnergy
                    .stream()
                    .filter(s -> sids.contains(s.getId()))
                    .collect(Collectors.toList());

            //能源管理计量器具，标签可能关联重点设备，当不存在交集时，则无需查询
            if (ArrayUtil.isEmpty(collect)) {
                return resultV2VO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            standingBookIds.addAll(standingBookIdList);
        }

        // 4.4.台账id为空直接返回结果
        if (CollUtil.isEmpty(standingBookIds)) {
            return resultV2VO;
        }

        // 4.5.根据台账和其他条件从数据库里拿出折标煤数据
        // 4.5.1.根据台账ID查询用量和折标煤
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                paramVO,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);

        // 按能源查看
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {

            Map<Long, Map<String, BigDecimal>> energyTimeStandardCoalMap = usageCostDataList.stream()
                    .collect(Collectors.groupingBy(
                            UsageCostData::getEnergyId,
                            Collectors.toMap(
                                    UsageCostData::getTime,
                                    UsageCostData::getTotalStandardCoalEquivalent)));

            Map<Long, EnergyConfigurationDO> energyMap = energyList
                    .stream()
                    .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

            List<StatisticsChartYInfoV2VO> ydata = energyMap.entrySet()
                    .stream()
                    .filter(entry -> energyTimeStandardCoalMap.containsKey(entry.getKey())) // 仅处理有数据的 energy
                    .map(entry -> {
                        Long energyId = entry.getKey();
                        EnergyConfigurationDO energy = entry.getValue();
                        Map<String, BigDecimal> timeCostMap = energyTimeStandardCoalMap.getOrDefault(energyId, Collections.emptyMap());

                        List<StandardCoalChartYData> dataList = xdata
                                .stream()
                                .map(time -> {
                                    time = dealStrTime(time);
                                    StandardCoalChartYData vo = new StandardCoalChartYData();
                                    BigDecimal standardCoal = timeCostMap.getOrDefault(time, BigDecimal.ZERO);
                                    vo.setStandardCoal(dealBigDecimalScale(standardCoal, DEFAULT_SCALE));
                                    return vo;
                                })
                                .collect(Collectors.toList());

                        StatisticsChartYInfoV2VO<StandardCoalChartYData> yInfo = new StatisticsChartYInfoV2VO<>();
                        yInfo.setId(energyId);
                        yInfo.setName(energy.getEnergyName());
                        yInfo.setData(dataList);
                        return yInfo;
                    })
                    .collect(Collectors.toList());

            resultV2VO.setYdata(ydata);

        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {//按标签
            //涉及到的标签
            //key是一级标签
            // 过滤并按标签名分组
            Map<String, List<StandingbookLabelInfoDO>> labelGrouped = standingbookIdsByLabel.stream()
                    .filter(label -> standingBookIds.contains(label.getStandingbookId()))
                    .collect(Collectors.groupingBy(StandingbookLabelInfoDO::getName));

            // 提取一级标签ID
            List<Long> topLabelIds = labelGrouped.keySet().stream()
                    .map(s -> s.substring(s.indexOf("_") + 1))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            // 获取标签信息
            List<LabelConfigDO> labelList = labelConfigService.getByIds(topLabelIds);
            Map<String, LabelConfigDO> labelMap = labelList.stream()
                    .collect(Collectors.toMap(s -> LABEL_NAME_PREFIX + s.getId(), Function.identity()));

            // 构造 standingbookId -> labelKey 映射
            Map<Long, String> standingbookIdToLabel = new HashMap<>();
            labelGrouped.forEach((labelKey, list) ->
                    list.forEach(item -> standingbookIdToLabel.put(item.getStandingbookId(), labelKey))
            );

            // 构造 (labelKey, time) -> cost 的二维映射
            Map<String, Map<String, BigDecimal>> labelTimeCostMap = new HashMap<>();
            for (UsageCostData data : usageCostDataList) {
                Long standingbookId = data.getStandingbookId();
                String time = data.getTime();
                BigDecimal standardCoal = data.getTotalStandardCoalEquivalent();

                String labelKey = standingbookIdToLabel.get(standingbookId);
                if (labelKey == null) {
                    continue;
                }

                labelTimeCostMap
                        .computeIfAbsent(labelKey, k -> new HashMap<>())
                        .merge(time, standardCoal, BigDecimal::add);
            }

            //构建结果
            List<StatisticsChartYInfoV2VO> infoV2VOS = new ArrayList<>();
            labelTimeCostMap.forEach((labelKey, timeCostMap) -> {
                LabelConfigDO labelConfigDO = labelMap.get(labelKey);
                if (labelConfigDO == null) {
                    return;
                }

                List<StandardCoalChartYData> ydata = xdata.stream().map(x -> {
                    x = dealStrTime(x);
                    BigDecimal standardCoal = timeCostMap.getOrDefault(x, BigDecimal.ZERO);
                    StandardCoalChartYData vo = new StandardCoalChartYData();
                    vo.setStandardCoal(standardCoal.compareTo(BigDecimal.ZERO) > 0 ? dealBigDecimalScale(standardCoal, DEFAULT_SCALE) : BigDecimal.ZERO);
                    return vo;
                }).collect(Collectors.toList());

                StatisticsChartYInfoV2VO<StandardCoalChartYData> yInfo = new StatisticsChartYInfoV2VO<>();
                yInfo.setId(labelConfigDO.getId());
                yInfo.setName(labelConfigDO.getLabelName());
                yInfo.setData(ydata);
                infoV2VOS.add(yInfo);
            });

            resultV2VO.setYdata(infoV2VOS);
        } else {
            //综合查看
            //根据日期计算最大 / 最小 / 平均 / 总和
            StatsResult statsResult = CalculateUtil.calculateStats(
                    usageCostDataList,
                    UsageCostData::getTotalStandardCoalEquivalent);

            List<StatisticsChartYInfoV2VO> ydata = new ArrayList<>();
            xdata.forEach(s -> {
                StatisticsChartYInfoV2VO<StandardCoalChartYData> yInfoV2VO = new StatisticsChartYInfoV2VO<>();
                StandardCoalChartYData dataV2VO = new StandardCoalChartYData();
                if (Objects.nonNull(statsResult)) {
                    dataV2VO.setAvg(dealBigDecimalScale(statsResult.getAvg(), DEFAULT_SCALE));
                    dataV2VO.setMax(dealBigDecimalScale(statsResult.getMax(), DEFAULT_SCALE));
                    dataV2VO.setMin(dealBigDecimalScale(statsResult.getMin(), DEFAULT_SCALE));
                    dataV2VO.setSum(dealBigDecimalScale(statsResult.getSum(), DEFAULT_SCALE));
                    // substring 返回 endIndex-beginIndex哥字符 因为是[ )
                    String subs = dealStrTime(s);
                    List<UsageCostData> collect = usageCostDataList.stream().filter(u -> u.getTime().equals(subs)).collect(Collectors.toList());
                    if (CollUtil.isNotEmpty(collect)) {
                        dataV2VO.setStandardCoal(dealBigDecimalScale(collect.get(0).getTotalStandardCoalEquivalent(), DEFAULT_SCALE));
                    } else {
                        dataV2VO.setStandardCoal(BigDecimal.ZERO);
                    }
                } else {
                    dataV2VO.setAvg(BigDecimal.ZERO);
                    dataV2VO.setMax(BigDecimal.ZERO);
                    dataV2VO.setMin(BigDecimal.ZERO);
                    dataV2VO.setSum(BigDecimal.ZERO);
                    dataV2VO.setStandardCoal(BigDecimal.ZERO);
                }
                yInfoV2VO.setData(Collections.singletonList(dataV2VO));
                ydata.add(yInfoV2VO);
            });
            resultV2VO.setYdata(ydata);
        }

        // 获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTime(
                paramVO,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);
        resultV2VO.setDataTime(lastTime);

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultV2VO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        // 返回查询结果。
        return resultV2VO;

    }

    public List<StandardCoalInfo> queryDefault(String topLabel,
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

    /**
     * 有顶级喝有子集标签处理
     *
     * @param standingBookUsageMap
     * @param labelMap
     * @param standingbookIdsByLabel
     * @return
     */
    public List<StandardCoalInfo> queryDefaultTopLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                       Map<Long, LabelConfigDO> labelMap,
                                                       List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                       Map<Long, EnergyConfigurationDO> energyMap) {
        List<StandardCoalInfo> resultList = new ArrayList<>();
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

            // 获取能源数据
            EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);

            // 由于数采数据是按 台账 日期能源进行分组的 而一个标签关联多个台账，那么标签同一个日期就会有多条不同台账的数据，所以要按日期进行合并
            // 聚合数据 转换成 StandardCoalInfoData
            List<StandardCoalInfoData> dataList = new ArrayList<>(usageCostList.stream().collect(Collectors.groupingBy(
                    UsageCostData::getTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                BigDecimal totalConsumption = list.stream()
                                        .map(UsageCostData::getCurrentTotalUsage)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                BigDecimal totalStandardCoal = list.stream()
                                        .map(UsageCostData::getTotalStandardCoalEquivalent)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new StandardCoalInfoData(list.get(0).getTime(), totalConsumption, totalStandardCoal);
                            }
                    )
            )).values());

            // 用量数据求和
            BigDecimal totalConsumption = dataList.stream()
                    .map(StandardCoalInfoData::getConsumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 折标煤数据求和
            BigDecimal totalCost = dataList.stream()
                    .map(StandardCoalInfoData::getStandardCoal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            StandardCoalInfo info = new StandardCoalInfo();
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
                i.setStandardCoal(dealBigDecimalScale(i.getStandardCoal(), DEFAULT_SCALE));
                i.setConsumption(dealBigDecimalScale(i.getConsumption(), DEFAULT_SCALE));
            }).collect(Collectors.toList());

            info.setStandardCoalInfoDataList(dataList);
            info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, DEFAULT_SCALE));
            info.setSumEnergyStandardCoal(dealBigDecimalScale(totalCost, DEFAULT_SCALE));

            resultList.add(info);
        });

        return resultList;
    }

    public List<StandardCoalInfo> queryDefaultSubLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
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

        List<StandardCoalInfo> resultList = new ArrayList<>();

        grouped.forEach((topLabelKey, labelInfoGroup) -> {

            // 获取顶层标签id topLabelKey值：label_133
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            LabelConfigDO topLabel = labelMap.get(topLabelId);
            if (topLabel == null) {
                return; // 如果一级标签不存在，跳过
            }

            // 获取下级标签名字
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

                    // 获取能源数据
                    EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);

                    // 因为数采数据按台账id合并并包含多个时间  但一个标签关联了多个台账 所以还需要按照日期再聚合一下 要重新考虑一下
                    //  usageCostData数据按能源、台账进行分组了，现在的分组方式 会导致用量数据无法累加。
                    // 聚合数据 转换成 StandardCoalInfoData
                    List<StandardCoalInfoData> dataList = new ArrayList<>(usageCostList.stream().collect(Collectors.groupingBy(
                            UsageCostData::getTime,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> {
                                        BigDecimal totalConsumption = list.stream()
                                                .map(UsageCostData::getCurrentTotalUsage)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        BigDecimal totalStandardCoal = list.stream()
                                                .map(UsageCostData::getTotalStandardCoalEquivalent)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new StandardCoalInfoData(list.get(0).getTime(), totalConsumption, totalStandardCoal);
                                    }
                            )
                    )).values());

                    // 用量数据求和
                    BigDecimal totalConsumption = dataList.stream()
                            .map(StandardCoalInfoData::getConsumption)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 折标煤数据求和
                    BigDecimal totalCost = dataList.stream()
                            .map(StandardCoalInfoData::getStandardCoal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);


                    StandardCoalInfo info = new StandardCoalInfo();
                    info.setEnergyId(energyId);
                    info.setEnergyName(energyConfigurationDO.getEnergyName());
                    info.setLabel1(topLabel.getLabelName());
                    info.setLabel2(label2Name);
                    info.setLabel3(label3Name);
                    info.setLabel4(label4Name);
                    info.setLabel5(label5Name);

                    dataList = dataList.stream().peek(i -> {
                        i.setStandardCoal(dealBigDecimalScale(i.getStandardCoal(), DEFAULT_SCALE));
                        i.setConsumption(dealBigDecimalScale(i.getConsumption(), DEFAULT_SCALE));
                    }).collect(Collectors.toList());

                    info.setStandardCoalInfoDataList(dataList);
                    info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, DEFAULT_SCALE));
                    info.setSumEnergyStandardCoal(dealBigDecimalScale(totalCost, DEFAULT_SCALE));

                    resultList.add(info);
                });
            });
        });

        return resultList;


    }

    public List<StandardCoalInfo> queryByLabel(String topLabel,
                                               String childLabels,
                                               List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                               List<UsageCostData> usageCostDataList) {

        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));


        if (CharSequenceUtil.isNotBlank(topLabel) && CharSequenceUtil.isBlank(childLabels)) {
            // 只有顶级标签
            return queryByTopLabel(standingBookUsageMap, labelMap, standingbookIdsByLabel);
        } else {
            // 有顶级、有子集标签
            return queryBySubLabel(standingBookUsageMap, labelMap, standingbookIdsByLabel);
        }
    }

    /**
     * 只有顶级标签处理
     *
     * @param standingBookUsageMap
     * @param labelMap
     * @param standingbookIdsByLabel
     * @return
     */
    public List<StandardCoalInfo> queryByTopLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                  Map<Long, LabelConfigDO> labelMap,
                                                  List<StandingbookLabelInfoDO> standingbookIdsByLabel) {

        List<StandardCoalInfo> resultList = new ArrayList<>();

        List<UsageCostData> labelUsageCostDataList = new ArrayList<>();
        // 获取标签关联的台账id，并取到对应的数据
        standingbookIdsByLabel.forEach(labelInfo -> {
            List<UsageCostData> usageList = standingBookUsageMap.get(labelInfo.getStandingbookId());
            if (usageList == null || usageList.isEmpty()) {
                return; // 计量器具没有数据，跳过
            }
            labelUsageCostDataList.addAll(usageList);
        });

        // 由于数采数据是按 台账 日期能源进行分组的 而一个标签关联多个台账，那么标签同一个日期就会有多条不同台账的数据，所以要按日期进行合并
        List<StandardCoalInfoData> dataList = new ArrayList<>(labelUsageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getTime,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    BigDecimal totalConsumption = list.stream()
                                            .map(UsageCostData::getCurrentTotalUsage)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    BigDecimal totalStandardCoal = list.stream()
                                            .map(UsageCostData::getTotalStandardCoalEquivalent)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    return new StandardCoalInfoData(list.get(0).getTime(), totalConsumption, totalStandardCoal);
                                }
                        )
                )).values());


        BigDecimal totalConsumption = dataList.stream()
                .map(StandardCoalInfoData::getConsumption)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalStandardCoal = dataList.stream()
                .map(StandardCoalInfoData::getStandardCoal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StandardCoalInfo info = new StandardCoalInfo();
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
            i.setStandardCoal(dealBigDecimalScale(i.getStandardCoal(), DEFAULT_SCALE));
            i.setConsumption(dealBigDecimalScale(i.getConsumption(), DEFAULT_SCALE));
        }).collect(Collectors.toList());

        info.setStandardCoalInfoDataList(dataList);
        info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, DEFAULT_SCALE));
        info.setSumEnergyStandardCoal(dealBigDecimalScale(totalStandardCoal, DEFAULT_SCALE));

        resultList.add(info);

        return resultList;
    }

    /**
     * 有顶级喝有子集标签处理
     *
     * @param standingBookUsageMap
     * @param labelMap
     * @param standingbookIdsByLabel
     * @return
     */
    public List<StandardCoalInfo> queryBySubLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                  Map<Long, LabelConfigDO> labelMap,
                                                  List<StandingbookLabelInfoDO> standingbookIdsByLabel) {
        // 标签查询条件处理
        //根据能源ID分组
        // 使用 Collectors.groupingBy 根据 name 和 value 分组
        // 此处不应该过滤，因为如果指定5个标签 因为有一个标签 和能源没有交集，则改标签在取用量数据时候，是取不到的， 而且该标签还需要展示对应数据
        // 所以不需要过滤 ：即选中五个标签 那么就要展示五个标签，如果过滤的话 那么 标签就有可能过滤掉然后不展示
        Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                .collect(Collectors.groupingBy(
                        // 第一个分组条件：按 name
                        StandingbookLabelInfoDO::getName,
                        // 第二个分组条件：按 value
                        Collectors.groupingBy(StandingbookLabelInfoDO::getValue)
                ));

        List<StandardCoalInfo> resultList = new ArrayList<>();

        log.info("==============================================================================");
        log.info("grouped: " + grouped);
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


                // 由于数采数据是按 台账 日期能源进行分组的 而一个标签关联多个台账，那么标签同一个日期就会有多条不同台账的数据，所以要按日期进行合并
                List<StandardCoalInfoData> dataList = new ArrayList<>(labelUsageCostDataList.stream()
                        .collect(Collectors.groupingBy(
                                UsageCostData::getTime,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            BigDecimal totalConsumption = list.stream()
                                                    .map(UsageCostData::getCurrentTotalUsage)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                                            BigDecimal totalStandardCoal = list.stream()
                                                    .map(UsageCostData::getTotalStandardCoalEquivalent)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                                            return new StandardCoalInfoData(list.get(0).getTime(), totalConsumption, totalStandardCoal);
                                        }
                                )
                        )).values());

                BigDecimal totalConsumption = dataList.stream()
                        .map(StandardCoalInfoData::getConsumption)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalStandardCoal = dataList.stream()
                        .map(StandardCoalInfoData::getStandardCoal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                log.info("----------------------------------------------------------------------------------");
                log.info("labelUsageCostDataList: " + labelUsageCostDataList);
                log.info("dataList: " + dataList);
                log.info("totalConsumption: " + totalConsumption);
                log.info("totalStandardCoal: " + totalStandardCoal);

                StandardCoalInfo info = new StandardCoalInfo();
                info.setLabel1(topLabel.getLabelName());
                info.setLabel2(label2Name);
                info.setLabel3(label3Name);
                info.setLabel4(label4Name);
                info.setLabel5(label5Name);

                dataList = dataList.stream().peek(i -> {
                    i.setStandardCoal(dealBigDecimalScale(i.getStandardCoal(), DEFAULT_SCALE));
                    i.setConsumption(dealBigDecimalScale(i.getConsumption(), DEFAULT_SCALE));
                }).collect(Collectors.toList());

                info.setStandardCoalInfoDataList(dataList);
                log.info("StandardCoalInfoDataList: " + info.getStandardCoalInfoDataList());
                log.info("----------------------------------------------------------------------------------");
                info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, DEFAULT_SCALE));
                info.setSumEnergyStandardCoal(dealBigDecimalScale(totalStandardCoal, DEFAULT_SCALE));

                resultList.add(info);
            });
        });
        log.info("==============================================================================");
        return resultList;
    }

    /**
     * 根据能源查看
     */
    public List<StandardCoalInfo> queryByEnergy(List<EnergyConfigurationDO> energyList,
                                                List<UsageCostData> usageCostDataList) {
        // 按能源ID分组
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        return energyList.stream()
                .filter(energy -> energyUsageMap.containsKey(energy.getId()))  // 筛选存在于map中的能源
                .map(energy -> {
                    // 获取与当前能源相关的用量数据
                    List<UsageCostData> usageCostList = energyUsageMap.get(energy.getId());
                    if (CollUtil.isEmpty(usageCostList)) {
                        return null; // 没有数据的不返回
                    }

                    StandardCoalInfo info = new StandardCoalInfo();
                    info.setEnergyId(energy.getId());
                    info.setEnergyName(energy.getEnergyName());

                    List<StandardCoalInfoData> infoDataV2List = usageCostList.stream()
                            .map(usageCost -> new StandardCoalInfoData(
                                    //DateUtil.format(usageCost.getTime(), dataType.getFormat()),
                                    usageCost.getTime(),
                                    usageCost.getCurrentTotalUsage(),
                                    usageCost.getTotalStandardCoalEquivalent()
                            ))
                            .collect(Collectors.toList());

                    BigDecimal sumEnergyConsumption = infoDataV2List
                            .stream()
                            .map(StandardCoalInfoData::getConsumption)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal sumEnergyStandardCoal = infoDataV2List
                            .stream()
                            .map(StandardCoalInfoData::getStandardCoal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    infoDataV2List = infoDataV2List.stream().peek(i -> {
                        i.setStandardCoal(dealBigDecimalScale(i.getStandardCoal(), DEFAULT_SCALE));
                        i.setConsumption(dealBigDecimalScale(i.getConsumption(), DEFAULT_SCALE));
                    }).collect(Collectors.toList());


                    info.setStandardCoalInfoDataList(infoDataV2List);

                    info.setSumEnergyConsumption(dealBigDecimalScale(sumEnergyConsumption, DEFAULT_SCALE));
                    info.setSumEnergyStandardCoal(dealBigDecimalScale(sumEnergyStandardCoal, DEFAULT_SCALE));
                    return info;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据 label id 数组和索引，安全获取 label 名称
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

}
