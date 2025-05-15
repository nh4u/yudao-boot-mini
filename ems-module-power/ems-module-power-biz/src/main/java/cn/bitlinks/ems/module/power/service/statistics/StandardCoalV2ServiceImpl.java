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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

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
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        // 1.1.校验结束时间必须大于开始时间
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }

        // 1.2.时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 2.校验查看类型
        Integer queryType = paramVO.getQueryType();
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        // 2.1.查看类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsResultV2VO.class);
        }

        // 4.如果没有则去数据库查询
        StatisticsResultV2VO<StandardCoalInfo> resultVO = new StatisticsResultV2VO<>();
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
                resultVO.setDataTime(LocalDateTime.now());
                return resultVO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            standingBookIds.addAll(standingBookIdList);
        }

        // 4.4.台账id为空直接返回结果
        if (CollectionUtil.isEmpty(standingBookIds)) {
            resultVO.setDataTime(LocalDateTime.now());
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
            List<StandardCoalInfo> standardCoalInfos = queryByEnergy(energyList, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(standardCoalInfos);

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

            List<StandardCoalInfo> standardCoalInfos = queryByLabel(grouped, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(standardCoalInfos);

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

            List<StandardCoalInfo> statisticsInfoV2s = queryDefault(grouped, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(statisticsInfoV2s);
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
    public StatisticsResultV2VO<StatisticsInfoV2> standardCoalAnalysisChart(StatisticsParamVO paramVO) {
        return null;
    }


    public List<StandardCoalInfo> queryDefault(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
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

                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = energyUsageMap.get(labelInfo.getStandingbookId());
                    if (usageList == null || usageList.isEmpty()) {
                        return; // 计量器具没有数据，跳过
                    }

                    // 用量数据按能源分组
                    Map<Long, List<UsageCostData>> energyUsageCostMap = usageList
                            .stream()
                            .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

                    energyUsageCostMap.forEach((energyId, usageCostList) -> {

                        // 获取能源数据
                        EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);

                        // 聚合数据 转换成 StandardCoalInfoData
                        List<StandardCoalInfoData> dataList = usageList.stream()
                                .map(usage -> new StandardCoalInfoData(
                                        //DateUtil.format(usage.getTime(), dataType.getFormat()),
                                        usage.getTime(),
                                        usage.getCurrentTotalUsage(),
                                        usage.getTotalStandardCoalEquivalent()
                                ))
                                .collect(Collectors.toList());

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
                        info.setStandardCoalInfoDataList(dataList);
                        info.setSumEnergyConsumption(totalConsumption);
                        info.setSumEnergyStandardCoal(totalCost);

                        resultList.add(info);
                    });
                });
            });
        });

        return resultList;


    }


    public List<StandardCoalInfo> queryByLabel(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
                                               List<UsageCostData> usageCostDataList, DataTypeEnum dataType) {

        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        List<StandardCoalInfo> resultList = new ArrayList<>();

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

                    List<StandardCoalInfoData> dataList = usageList.stream()
                            .map(usage -> new StandardCoalInfoData(
                                    //DateUtil.format(usage.getTime(), dataType.getFormat()),
                                    usage.getTime(),
                                    usage.getCurrentTotalUsage(),
                                    usage.getTotalStandardCoalEquivalent()
                            ))
                            .collect(Collectors.toList());

                    BigDecimal totalConsumption = dataList.stream()
                            .map(StandardCoalInfoData::getConsumption)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalStandardCoal = dataList.stream()
                            .map(StandardCoalInfoData::getStandardCoal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    StandardCoalInfo info = new StandardCoalInfo();
                    info.setLabel1(topLabel.getLabelName());
                    info.setLabel2(label2Name);
                    info.setLabel3(label3Name);
                    info.setStandardCoalInfoDataList(dataList);
                    info.setSumEnergyConsumption(totalConsumption);
                    info.setSumEnergyStandardCoal(totalStandardCoal);

                    resultList.add(info);
                });
            });
        });

        return resultList;
    }

    /**
     * 根据能源查看
     */
    public List<StandardCoalInfo> queryByEnergy(List<EnergyConfigurationDO> energyList, List<UsageCostData> usageCostDataList, DataTypeEnum dataType) {
        // 按能源ID分组
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        return energyList.stream()
                .filter(energy -> energyUsageMap.containsKey(energy.getId()))  // 筛选存在于map中的能源
                .map(energy -> {
                    // 获取与当前能源相关的用量数据
                    List<UsageCostData> usageCostList = energyUsageMap.get(energy.getId());
                    if (CollectionUtil.isEmpty(usageCostList)) {
                        return null; // 没有数据的不返回
                    }

                    StandardCoalInfo info = new StandardCoalInfo();
                    info.setEnergyId(energy.getId());
                    info.setEnergyName(energy.getName());

                    List<StandardCoalInfoData> infoDataV2List = usageCostList.stream()
                            .map(usageCost -> new StandardCoalInfoData(
                                    //DateUtil.format(usageCost.getTime(), dataType.getFormat()),
                                    usageCost.getTime(),
                                    usageCost.getCurrentTotalUsage(),
                                    usageCost.getTotalStandardCoalEquivalent()
                            ))
                            .collect(Collectors.toList());

                    BigDecimal sumEnergyConsumption = infoDataV2List.stream().map(StandardCoalInfoData::getConsumption).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumEnergyStandardCoal = infoDataV2List.stream().map(StandardCoalInfoData::getStandardCoal).reduce(BigDecimal.ZERO, BigDecimal::add);

                    info.setStandardCoalInfoDataList(infoDataV2List);

                    info.setSumEnergyConsumption(sumEnergyConsumption);
                    info.setSumEnergyStandardCoal(sumEnergyStandardCoal);
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


}
