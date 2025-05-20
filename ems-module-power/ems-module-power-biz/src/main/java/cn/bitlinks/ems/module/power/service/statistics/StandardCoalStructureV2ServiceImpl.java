package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.pojo.StatsResult;
import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.LABEL_NAME_PREFIX;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.*;

/**
 * @Title: ydme-doublecarbon
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/14 17:10
 **/
@Service
@Validated
@Slf4j
public class StandardCoalStructureV2ServiceImpl implements StandardCoalStructureV2Service {

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
    public StatisticsResultV2VO<StructureInfo> standardCoalStructureAnalysisTable(StatisticsParamV2VO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_STANDARD_COAL_STRUCTURE_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsResultV2VO.class);
        }

        // 4.如果没有则去数据库查询
        StatisticsResultV2VO<StructureInfo> resultVO = new StatisticsResultV2VO<>();
        resultVO.setDataTime(LocalDateTime.now());

        // 4.1.表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultVO.setHeader(tableHeader);

        // 4.2.能源id处理
        List<EnergyConfigurationDO> energyList = energyConfigurationService
                .getByEnergyClassify(
                        CollectionUtil.isNotEmpty(paramVO.getEnergyIds()) ? new HashSet<>(paramVO.getEnergyIds()) : new HashSet<>(),
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
        if (CollectionUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel
                    .stream()
                    .map(StandingbookLabelInfoDO::getStandingbookId)
                    .collect(Collectors.toList());

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
        if (CollectionUtil.isEmpty(standingBookIds)) {
            return resultVO;
        }

        // 4.5.根据台账和其他条件从数据库里拿出折标煤数据
        // 4.5.1.根据台账ID查询用量和折标煤
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                paramVO,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);

        List<StructureInfo> statisticsInfoList = new ArrayList<>();
        // 1、按能源查看
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            List<StructureInfo> structureInfos = queryByEnergy(energyList, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(structureInfos);

        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 2、按标签查看
            // 标签查询条件处理
            //根据能源ID分组
            // 使用 Collectors.groupingBy 根据 name 和 value 分组
            Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                    .filter(s -> standingBookIds.contains(s.getStandingbookId()))
                    .collect(Collectors.groupingBy(
                            // 第一个分组条件：按 name
                            StandingbookLabelInfoDO::getName,
                            // 第二个分组条件：按 value
                            Collectors.groupingBy(StandingbookLabelInfoDO::getValue)
                    ));

            List<StructureInfo> structureInfos = queryByLabel(grouped, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(structureInfos);

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            //根据能源ID分组
            // 使用 Collectors.groupingBy 根据 name 和 value 分组
            Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                    .filter(s -> standingBookIds.contains(s.getStandingbookId()))
                    .collect(Collectors.groupingBy(
                            // 第一个分组条件：按 name
                            StandingbookLabelInfoDO::getName,
                            // 第二个分组条件：按 value
                            Collectors.groupingBy(StandingbookLabelInfoDO::getValue)
                    ));

            List<StructureInfo> structureInfos = queryDefault(grouped, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(structureInfos);
        }

        resultVO.setStatisticsInfoList(statisticsInfoList);

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
    public StatisticsChartResultV2VO standardCoalStructureAnalysisChart(StatisticsParamV2VO paramVO) {
        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_STANDARD_COAL_STRUCTURE_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
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
                        CollectionUtil.isNotEmpty(paramVO.getEnergyIds()) ? new HashSet<>(paramVO.getEnergyIds()) : new HashSet<>(),
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
        if (CollectionUtil.isNotEmpty(standingbookIdsByLabel)) {
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
        if (CollectionUtil.isEmpty(standingBookIds)) {
            return resultV2VO;
        }

        // 4.5.根据台账和其他条件从数据库里拿出折标煤数据
        // 4.5.1.根据台账ID查询用量和折标煤
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                paramVO,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);


        // TODO: 2025/5/19 明天开始开发 图占比 


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
                                    StandardCoalChartYData vo = new StandardCoalChartYData();
                                    vo.setStandardCoal(timeCostMap.getOrDefault(time, null));
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
                    BigDecimal standardCoal = timeCostMap.getOrDefault(x, BigDecimal.ZERO);
                    StandardCoalChartYData vo = new StandardCoalChartYData();
                    vo.setStandardCoal(standardCoal.compareTo(BigDecimal.ZERO) > 0 ? standardCoal : null);
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
            Map<String, StatsResult> statsResultMap = CalculateUtil.calculateGroupStats(
                    usageCostDataList,
                    UsageCostData::getTime,
                    UsageCostData::getTotalStandardCoalEquivalent);

            List<StatisticsChartYInfoV2VO> ydata = new ArrayList<>();
            xdata.forEach(s -> {
                StatsResult statsResult = statsResultMap.get(s);
                if (Objects.nonNull(statsResult)) {
                    StatisticsChartYInfoV2VO<StandardCoalChartYData> yInfoV2VO = new StatisticsChartYInfoV2VO<>();
                    StandardCoalChartYData dataV2VO = new StandardCoalChartYData();
                    dataV2VO.setAvg(statsResult.getAvg());
                    dataV2VO.setMax(statsResult.getMax());
                    dataV2VO.setMin(statsResult.getMin());
                    dataV2VO.setStandardCoal(statsResult.getSum());
                    yInfoV2VO.setData(Collections.singletonList(dataV2VO));
                    ydata.add(yInfoV2VO);
                } else {
                    ydata.add(null);
                }
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


    public List<StructureInfo> queryDefault(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
                                            List<UsageCostData> usageCostDataList,
                                            DataTypeEnum dataType) {

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
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        List<StructureInfo> resultList = new ArrayList<>();

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

                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = energyUsageMap.get(labelInfo.getStandingbookId());
                    if (usageList == null || usageList.isEmpty()) {
                        return; // 计量器具没有数据，跳过
                    }

                    // 用量数据按能源分组
                    Map<Long, List<UsageCostData>> energyUsageCostMap = usageList
                            .stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

                    List<StructureInfo> tempResultList = new ArrayList<>();
                    energyUsageCostMap.forEach((energyId, usageCostList) -> {

                        // 获取能源数据
                        EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);

                        // 聚合数据 转换成 StandardCoalInfoData
                        List<StructureInfoData> dataList = usageCostList.stream()
                                .map(usage -> new StructureInfoData(
                                        //DateUtil.format(usage.getTime(), dataType.getFormat()),
                                        usage.getTime(),
                                        usage.getTotalStandardCoalEquivalent(),
                                        null
                                ))
                                .collect(Collectors.toList());

                        // 折标煤数据求和
                        BigDecimal totalNum = dataList.stream()
                                .map(StructureInfoData::getNum)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);


                        StructureInfo info = new StructureInfo();
                        info.setEnergyId(energyId);
                        info.setEnergyName(energyConfigurationDO.getEnergyName());
                        info.setLabel1(topLabel.getLabelName());
                        info.setLabel2(label2Name);
                        info.setLabel3(label3Name);
                        info.setStructureInfoDataList(dataList);
                        info.setSumNum(totalNum);
                        info.setSumProportion(null);

                        tempResultList.add(info);
                    });
                    List<StructureInfo> structureResultList = getStructureResultList(tempResultList);
                    resultList.addAll(structureResultList);
                });
            });
        });

        return resultList;


    }


    public List<StructureInfo> queryByLabel(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
                                            List<UsageCostData> usageCostDataList,
                                            DataTypeEnum dataType) {

        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        List<StructureInfo> resultList = new ArrayList<>();

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

                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = energyUsageMap.get(labelInfo.getStandingbookId());
                    if (usageList == null || usageList.isEmpty()) {
                        return; // 计量器具没有数据，跳过
                    }

                    List<StructureInfoData> dataList = usageList.stream()
                            .map(usage -> new StructureInfoData(
                                    //DateUtil.format(usage.getTime(), dataType.getFormat()),
                                    usage.getTime(),
                                    usage.getTotalStandardCoalEquivalent(),
                                    null
                            ))
                            .collect(Collectors.toList());

                    BigDecimal totalNum = dataList.stream()
                            .map(StructureInfoData::getNum)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    StructureInfo info = new StructureInfo();
                    info.setLabel1(topLabel.getLabelName());
                    info.setLabel2(label2Name);
                    info.setLabel3(label3Name);
                    info.setStructureInfoDataList(dataList);
                    info.setSumNum(totalNum);
                    info.setSumProportion(null);

                    resultList.add(info);
                });
            });
        });

        return getStructureResultList(resultList);
    }

    /**
     * 根据能源查看
     */
    public List<StructureInfo> queryByEnergy(List<EnergyConfigurationDO> energyList,
                                             List<UsageCostData> usageCostDataList,
                                             DataTypeEnum dataType) {
        // 按能源ID分组
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        List<StructureInfo> collect = energyList.stream()
                // 筛选存在于map中的能源
                .filter(energy -> energyUsageMap.containsKey(energy.getId()))
                .map(energy -> {
                    // 获取与当前能源相关的用量数据
                    List<UsageCostData> usageCostList = energyUsageMap.get(energy.getId());
                    if (CollectionUtil.isEmpty(usageCostList)) {
                        // 没有数据的不返回
                        return null;
                    }

                    StructureInfo info = new StructureInfo();
                    info.setEnergyId(energy.getId());
                    info.setEnergyName(energy.getName());

                    List<StructureInfoData> structureDataList = usageCostList.stream()
                            .map(usageCost -> new StructureInfoData(
                                    //DateUtil.format(usageCost.getTime(), dataType.getFormat()),
                                    usageCost.getTime(),
                                    usageCost.getTotalStandardCoalEquivalent(),
                                    null
                            ))
                            .collect(Collectors.toList());

                    // 横向折标煤总和
                    BigDecimal sumNum = structureDataList
                            .stream()
                            .map(StructureInfoData::getNum)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    info.setStructureInfoDataList(structureDataList);

                    info.setSumNum(sumNum);
                    info.setSumProportion(null);
                    return info;

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return getStructureResultList(collect);
    }


    /**
     * 处理占比问题
     *
     * @param list 对应list
     * @return
     */
    private List<StructureInfo> getStructureResultList(List<StructureInfo> list) {

        // 获取纵向总和map
        Map<String, BigDecimal> sumMap = getSumMap(list);
        // 获取合计
        for (StructureInfo structureInfo : list) {

            List<StructureInfoData> statisticsStructureDataList = structureInfo.getStructureInfoDataList();
            statisticsStructureDataList.forEach(s -> {
                s.setProportion(getProportion(s.getNum(), sumMap.get(s.getDate())));
            });

            structureInfo.setSumProportion(getProportion(structureInfo.getSumNum(), sumMap.get("sumNum")));
        }

        return list;
    }

    /**
     * 按时间为key 得到map  纵向综合map
     *
     * @param list 对应list
     * @return
     */
    private Map<String, BigDecimal> getSumMap(List<StructureInfo> list) {

        Map<String, BigDecimal> sumMap = new HashMap<>();

        list.forEach(l -> {
            List<StructureInfoData> structureInfoDataList = l.getStructureInfoDataList();
            structureInfoDataList.forEach(s -> {
                sumMap.put(s.getDate(), addBigDecimal(sumMap.get(s.getDate()), s.getNum()));
            });
            sumMap.put("sumNum", addBigDecimal(sumMap.get("sumNum"), l.getSumNum()));
        });

        return sumMap;
    }

    /**
     * 计算占比
     *
     * @param now   当前
     * @param total 总计
     * @return
     */
    private BigDecimal getProportion(BigDecimal now, BigDecimal total) {

        if (now == null || total == null) {
            return null;
        }
        BigDecimal proportion = BigDecimal.ZERO;
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            proportion = now.divide(total, 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return proportion;
    }

    /**
     * 两个数据相加
     *
     * @param first  1
     * @param second 2
     * @return add
     */
    private BigDecimal addBigDecimal(BigDecimal first, BigDecimal second) {

        if (Objects.isNull(first)) {
            return second;
        } else {
            return first.add(second);
        }

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
