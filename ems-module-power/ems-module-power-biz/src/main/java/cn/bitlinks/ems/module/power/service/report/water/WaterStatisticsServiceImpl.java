package cn.bitlinks.ems.module.power.service.report.water;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
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
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.WATER_STATISTICS;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.WATER_CHART;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.WATER_TABLE;
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
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
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
            return defaultNullData(tableHeader);
        }

        // 4.5.根据台账和其他条件从数据库里拿出用量数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                paramVO,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);

        if (CollUtil.isEmpty(usageCostDataList)) {
            return defaultNullData(tableHeader);
        }

        List<StatisticsInfoV2> statisticsInfoList = new ArrayList<>();

        // 加了 ENERGY_REVIEW 分支，其它分支和原来保持一致
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // 1、按能源查看
            List<StatisticsInfoV2> list =
                    queryByEnergy(energyList, usageCostDataList);
            statisticsInfoList.addAll(list);

        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 2、按标签查看
            List<StatisticsInfoV2> list =
                    queryByLabel(topLabel, childLabels, standingbookIdsByLabel, usageCostDataList);
            statisticsInfoList.addAll(list);

        } else {
            // 0、综合查看（默认）
            List<StatisticsInfoV2> list =
                    queryDefault(topLabel, childLabels, standingbookIdsByLabel, usageCostDataList);
            statisticsInfoList.addAll(list);
        }

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
                        standardCoalInfoData.setConsumption(null);
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

    public List<StatisticsInfoV2> queryByEnergy(List<EnergyConfigurationDO> energyList,
                                                List<UsageCostData> usageCostDataList) {
        // 按能源ID分组
        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getEnergyId));

        return energyList.stream()
                // 只要有数据的能源
                .filter(energy -> energyUsageMap.containsKey(energy.getId()))
                .map(energy -> {
                    List<UsageCostData> usageCostList = energyUsageMap.get(energy.getId());
                    if (CollUtil.isEmpty(usageCostList)) {
                        return null;
                    }

                    StatisticsInfoV2 info = new StatisticsInfoV2();
                    info.setEnergyId(energy.getId());
                    info.setEnergyName(energy.getEnergyName());

                    // 按能源查看时，标签维度统一给个“一级计量 / / / /”即可，和你综合查看的风格一致
                    info.setLabel1("一级计量");
                    info.setLabel2("/");
                    info.setLabel3("/");
                    info.setLabel4("/");
                    info.setLabel5("/");

                    // 原始时间粒度直接用 UsageCostData::getTime（和 queryByTopLabel / queryDefault 一致）
                    List<StatisticInfoDataV2> dataList = usageCostList.stream()
                            .map(usageCost -> new StatisticInfoDataV2(
                                    usageCost.getTime(),
                                    usageCost.getCurrentTotalUsage(),
                                    null
                            ))
                            .collect(Collectors.toList());

                    BigDecimal sumEnergyConsumption = dataList.stream()
                            .map(StatisticInfoDataV2::getConsumption)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal::add)
                            .orElse(null);

                    // 水这块你之前是用 scale 处理小数位的，这里保持一致
                    dataList = dataList.stream().peek(i -> {
                        i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
                    }).collect(Collectors.toList());

                    info.setStatisticsDateDataList(dataList);
                    info.setSumEnergyConsumption(dealBigDecimalScale(sumEnergyConsumption, scale));

                    return info;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
                                        .filter(Objects::nonNull)
                                        .reduce(BigDecimal::add).orElse(null);
                                return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, null);
                            }
                    )
            )).values());

            BigDecimal totalConsumption = dataList.stream()
                    .map(StatisticInfoDataV2::getConsumption)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal::add).orElse(null);

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
                                                .filter(Objects::nonNull)
                                                .reduce(BigDecimal::add).orElse(null);
                                        return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, null);
                                    }
                            )
                    )).values());

                    BigDecimal totalConsumption = dataList.stream()
                            .map(StatisticInfoDataV2::getConsumption)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal::add).orElse(null);

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

    public List<StatisticsInfoV2> queryByLabel(String topLabel,
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
    public List<StatisticsInfoV2> queryByTopLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                  Map<Long, LabelConfigDO> labelMap,
                                                  List<StandingbookLabelInfoDO> standingbookIdsByLabel) {

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

        // 由于数采数据是按 台账 日期能源进行分组的 而一个标签关联多个台账，那么标签同一个日期就会有多条不同台账的数据，所以要按日期进行合并
        List<StatisticInfoDataV2> dataList = new ArrayList<>(labelUsageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getTime,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    BigDecimal totalConsumption = list.stream()
                                            .map(UsageCostData::getCurrentTotalUsage)
                                            .filter(Objects::nonNull)
                                            .reduce(BigDecimal::add).orElse(null);
                                    return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, null);
                                }
                        )
                )).values());

        //按标签统计时候 用量不用合计
        BigDecimal totalConsumption = dataList.stream()
                .map(StatisticInfoDataV2::getConsumption)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);
//        BigDecimal totalStandardCoal = dataList.stream()
//                .map(StandardCoalInfoData::getStandardCoal)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StatisticsInfoV2 info = new StatisticsInfoV2();
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
            i.setConsumption(dealBigDecimalScale(i.getConsumption(), DEFAULT_SCALE));
        }).collect(Collectors.toList());

        info.setStatisticsDateDataList(dataList);
        info.setSumEnergyConsumption(dealBigDecimalScale(null, DEFAULT_SCALE));

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
    public List<StatisticsInfoV2> queryBySubLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
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


                // 由于数采数据是按 台账 日期能源进行分组的 而一个标签关联多个台账，那么标签同一个日期就会有多条不同台账的数据，所以要按日期进行合并
                List<StatisticInfoDataV2> dataList = new ArrayList<>(labelUsageCostDataList.stream()
                        .collect(Collectors.groupingBy(
                                UsageCostData::getTime,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            BigDecimal totalConsumption = list.stream()
                                                    .map(UsageCostData::getCurrentTotalUsage)
                                                    .filter(Objects::nonNull)
                                                    .reduce(BigDecimal::add).orElse(null);
                                            return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, null);
                                        }
                                )
                        )).values());

                //按标签统计时候 用量不用合计
                BigDecimal totalConsumption = dataList.stream()
                        .map(StatisticInfoDataV2::getConsumption)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal::add).orElse(null);

                StatisticsInfoV2 info = new StatisticsInfoV2();
                info.setLabel1(topLabel.getLabelName());
                info.setLabel2(label2Name);
                info.setLabel3(label3Name);
                info.setLabel4(label4Name);
                info.setLabel5(label5Name);

                dataList = dataList.stream().peek(i -> {
                    i.setConsumption(dealBigDecimalScale(i.getConsumption(), DEFAULT_SCALE));
                }).collect(Collectors.toList());

                info.setStatisticsDateDataList(dataList);
                info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, DEFAULT_SCALE));

                resultList.add(info);
            });
        });
        return resultList;
    }

    @Override
    public FeeChartResultVO<FeeChartYInfo> waterStatisticsChart(StatisticsParamV2VO paramVO) {

        // 1. 校验时间范围、查看类型、时间类型
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        Integer queryType = validateQueryType(paramVO.getQueryType());
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        // 2. 查询缓存
        String cacheKey = WATER_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<FeeChartResultVO<FeeChartYInfo>>() {
            });
        }

        // 3. 如果没有则去数据库查询
        FeeChartResultVO<FeeChartYInfo> resultVO = new FeeChartResultVO<>();
        resultVO.setDataTime(LocalDateTime.now());

        // 3.1 X 轴：时间列表
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultVO.setXdata(xdata);

        // 3.2 能源 ID 处理（保持与 waterStatisticsTable 相同逻辑）
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
            energyList = energyConfigurationService.getByEnergyGroup(energyGroup.getId());
        }
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 3.3 台账 ID 处理（保持与 waterStatisticsTable 相同逻辑）
        List<Long> standingBookIds = new ArrayList<>();
        // 3.3.1 根据能源 id 查询台账
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        List<Long> standingBookIdList = standingbookIdsByEnergy
                .stream()
                .map(StandingbookDO::getId)
                .collect(Collectors.toList());

        // 3.3.2 根据标签 id 查询
        String topLabel = paramVO.getTopLabel();
        String childLabels = paramVO.getChildLabels();
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService
                .getStandingbookIdsByLabel(topLabel, childLabels);

        // 3.3.3 能源台账 ids 和标签台账 ids 取交集
        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
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

        // 3.4 台账 ID 为空，直接返回空结果
        if (CollUtil.isEmpty(standingBookIds)) {
            return resultVO;
        }

        // 3.5 查询用量数据
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                paramVO,
                rangeOrigin[0],
                rangeOrigin[1],
                standingBookIds);

        if (CollUtil.isEmpty(usageCostDataList)) {
            return resultVO;
        }

        // 4. 根据查询维度构建 Y 轴数据
        Map<String, BigDecimal> sumConsumptionMap = new HashMap<>();
        List<FeeChartYInfo> yInfoList = new ArrayList<>();

        // 4.1 按能源查看
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {

            // (energyId, time) -> 用量
            Map<Long, Map<String, BigDecimal>> energyTimeConsumptionMap = usageCostDataList.stream()
                    .collect(Collectors.groupingBy(
                            UsageCostData::getEnergyId,
                            Collectors.groupingBy(
                                    UsageCostData::getTime,
                                    Collectors.mapping(
                                            UsageCostData::getCurrentTotalUsage,
                                            Collectors.reducing(null, (v1, v2) -> {
                                                if (v1 == null) return v2;
                                                if (v2 == null) return v1;
                                                return v1.add(v2);
                                            })
                                    )
                            )
                    ));

            Map<Long, EnergyConfigurationDO> energyMap = energyList
                    .stream()
                    .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

            energyTimeConsumptionMap.forEach((energyId, timeCostMap) -> {
                EnergyConfigurationDO energy = energyMap.get(energyId);
                if (energy == null) {
                    return;
                }

                List<BigDecimal> data = new ArrayList<>();
                xdata.forEach(date -> {
                    BigDecimal consumption = timeCostMap.get(date);
                    if (consumption != null) {
                        sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), consumption));
                    }
                    data.add(consumption != null ? dealBigDecimalScale(consumption, DEFAULT_SCALE) : BigDecimal.ZERO);
                });

                FeeChartYInfo yInfo = new FeeChartYInfo();
                yInfo.setName(energy.getEnergyName());
                yInfo.setData(data);

                BigDecimal sum = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sum));

                yInfoList.add(yInfo);
            });

        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 4.2 按标签查看

            // 只保留当前台账范围内的标签，并按 name 分组
            Map<String, List<StandingbookLabelInfoDO>> labelGrouped = standingbookIdsByLabel.stream()
                    .filter(label -> standingBookIds.contains(label.getStandingbookId()))
                    .collect(Collectors.groupingBy(StandingbookLabelInfoDO::getName));

            // 一级标签 ID 列表
            List<Long> topLabelIds = labelGrouped.keySet().stream()
                    .map(s -> s.substring(s.indexOf("_") + 1))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            // 获取标签信息
            List<LabelConfigDO> labelList = labelConfigService.getByIds(topLabelIds);
            Map<String, LabelConfigDO> labelMap = labelList.stream()
                    .collect(Collectors.toMap(s -> LABEL_NAME_PREFIX + s.getId(), Function.identity()));

            // standingbookId -> labelKey 映射
            Map<Long, String> standingbookIdToLabel = new HashMap<>();
            labelGrouped.forEach((labelKey, list) ->
                    list.forEach(item -> standingbookIdToLabel.put(item.getStandingbookId(), labelKey))
            );

            // (labelKey, time) -> 用量
            Map<String, Map<String, BigDecimal>> labelTimeCostMap =
                    dealMap(usageCostDataList, UsageCostData::getCurrentTotalUsage, standingbookIdToLabel);

            labelTimeCostMap.forEach((labelKey, timeCostMap) -> {
                LabelConfigDO labelConfigDO = labelMap.get(labelKey);
                if (labelConfigDO == null) {
                    return;
                }

                List<BigDecimal> data = new ArrayList<>();
                xdata.forEach(date -> {
                    BigDecimal consumption = timeCostMap.get(date);
                    if (consumption != null && BigDecimal.ZERO.compareTo(consumption) != 0) {
                        sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), consumption));
                        data.add(dealBigDecimalScale(consumption, DEFAULT_SCALE));
                    } else {
                        data.add(BigDecimal.ZERO);
                    }
                });

                FeeChartYInfo yInfo = new FeeChartYInfo();
                yInfo.setName(labelConfigDO.getLabelName());
                yInfo.setData(data);

                BigDecimal sum = data.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sum));

                yInfoList.add(yInfo);
            });

        } else {
            // 4.3 综合查看：所有水耗合并为一条“汇总”曲线

            Map<String, BigDecimal> timeConsumptionMap = usageCostDataList.stream()
                    .collect(Collectors.groupingBy(
                            UsageCostData::getTime,
                            Collectors.mapping(
                                    UsageCostData::getCurrentTotalUsage,
                                    Collectors.reducing(null, (v1, v2) -> {
                                        if (v1 == null) return v2;
                                        if (v2 == null) return v1;
                                        return v1.add(v2);
                                    })
                            )
                    ));

            List<BigDecimal> data = new ArrayList<>();
            xdata.forEach(date -> {
                BigDecimal consumption = timeConsumptionMap.get(date);
                data.add(consumption != null ? dealBigDecimalScale(consumption, DEFAULT_SCALE) : BigDecimal.ZERO);
            });

            FeeChartYInfo yInfo = new FeeChartYInfo();
            yInfo.setName("汇总");
            yInfo.setData(data);
            yInfoList.add(yInfo);
        }

        // 5. 底部合计“汇总”曲线，只在能源/标签维度下添加
        if (!sumConsumptionMap.isEmpty()) {
            List<BigDecimal> summary = new ArrayList<>();
            FeeChartYInfo sumYInfo = new FeeChartYInfo();
            sumYInfo.setName("汇总");

            xdata.forEach(date -> {
                BigDecimal consumption = sumConsumptionMap.get(date);
                summary.add(consumption != null ? dealBigDecimalScale(consumption, DEFAULT_SCALE) : BigDecimal.ZERO);
            });
            sumYInfo.setData(summary);
            yInfoList.add(sumYInfo);
        }

        resultVO.setYdata(yInfoList);

        // 6. 数据更新时间（和表格保持一致）
        LocalDateTime lastTime = usageCostService.getLastTime(
                paramVO,
                rangeOrigin[0],
                rangeOrigin[1],
                standingBookIds);
        resultVO.setDataTime(lastTime);

        // 7. 写缓存
        String jsonStr = JSON.toJSONString(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        return resultVO;
    }

    /**
     * 处理
     *
     * @param usageCostDataList
     * @param valueExtractor
     * @param standingbookLabelMap
     * @return
     */
    private Map<String, Map<String, BigDecimal>> dealMap(List<UsageCostData> usageCostDataList,
                                                         Function<UsageCostData, BigDecimal> valueExtractor,
                                                         Map<Long, String> standingbookLabelMap) {
        Map<String, Map<String, BigDecimal>> map = new HashMap<>();
        for (UsageCostData data : usageCostDataList) {
            String label = standingbookLabelMap.get(data.getStandingbookId());
            if (label == null) {
                continue;
            }
            BigDecimal value = valueExtractor.apply(data);
            String time = data.getTime();
            Map<String, BigDecimal> subMap = map.get(label);
            if (CollUtil.isNotEmpty(subMap)) {
                BigDecimal oldValue = subMap.get(time);
                if (Objects.nonNull(oldValue)) {
                    oldValue = Objects.isNull(value) ? oldValue : oldValue.add(value);
                } else {
                    oldValue = Objects.isNull(value) ? null : value;
                }
                subMap.put(time, oldValue);
            } else {
                subMap = new HashMap<>();
                subMap.put(time, value);
            }

            map.put(label, subMap);
        }
        return map;
    }


    @Override
    public List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO) {
        // 1. 校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];

        // 2. 时间维度 & X 轴
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        // 3. 基础信息
        Integer queryType = validateQueryType(paramVO.getQueryType());
        String topLabel = paramVO.getTopLabel();
        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels); // 你的原工具方法
        String labelName = getLabelName(topLabel, childLabels); // 你的原方法
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);

        String sheetName = WATER_STATISTICS;

        List<List<String>> list = ListUtils.newArrayList();

        if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // =============================
            // 1）按标签查看：列 = 标签1..N + 时间列 + 周期合计（不单列能源）
            // =============================
            list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签")); // 标签1

            for (int i = 2; i <= labelDeep; i++) {
                list.add(Arrays.asList(sheetName, labelName, strTime, "标签" + i));
            }

            // 时间列
            xdata.forEach(x -> list.add(Arrays.asList(sheetName, labelName, strTime, x)));

            // 周期合计
            list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计"));

        } else if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // =============================
            // 2）按能源查看：列 = 能源 + 时间列 + 周期合计（不展示标签列）
            // =============================
            list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "能源"));

            // 时间列
            xdata.forEach(x -> list.add(Arrays.asList(sheetName, labelName, strTime, x)));

            // 周期合计
            list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计"));

        } else {
            // =============================
            // 0）综合查看（默认）：列 = 标签1..N + 能源 + 时间列 + 周期合计
            // =============================
            list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签")); // 标签1
            for (int i = 2; i <= labelDeep; i++) {
                list.add(Arrays.asList(sheetName, labelName, strTime, "标签" + i));
            }
            list.add(Arrays.asList(sheetName, labelName, strTime, "能源"));

            // 时间列
            xdata.forEach(x -> list.add(Arrays.asList(sheetName, labelName, strTime, x)));

            // 周期合计
            list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计"));
        }

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

        List<List<Object>> result = ListUtils.newArrayList();

        // 查询统计表结果（已经包含了 header + 每行的时间序列数据）
        StatisticsResultV2VO<StatisticsInfoV2> resultVO = waterStatisticsTable(paramVO);
        List<String> tableHeader = resultVO.getHeader(); // X 轴（时间段）
        List<StatisticsInfoV2> statisticsInfoList = resultVO.getStatisticsInfoList();
        if (CollUtil.isEmpty(statisticsInfoList)) {
            return result;
        }

        Integer queryType = validateQueryType(paramVO.getQueryType());
        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);

        // 底部合计用
        Map<String, BigDecimal> sumConsumptionMap = new HashMap<>();

        // =====================
        // 公共：底部合计行的“前缀文字”，根据 dateType 判断
        // =====================
        String pre = "";
        Integer dateType = paramVO.getDateType();
        if (Objects.equals(dateType, 0)) {
            pre = DAILY_STATISTICS;
        } else if (Objects.equals(dateType, 1)) {
            pre = MONTHLY_STATISTICS;
        } else if (Objects.equals(dateType, 2)) {
            pre = ANNUAL_STATISTICS;
        }

        // =============================
        // 1）按标签查看：每行 = 标签层级汇总（不拆能源）
        // =============================
        if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {

            // 1. 先按“标签路径”分组（label1|label2|...）
            Map<String, List<StatisticsInfoV2>> groupedByLabelPath = statisticsInfoList.stream()
                    .collect(Collectors.groupingBy(s -> {
                        String[] labels = {s.getLabel1(), s.getLabel2(), s.getLabel3(), s.getLabel4(), s.getLabel5()};
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < labelDeep; i++) {
                            if (i > 0) {
                                sb.append("|");
                            }
                            sb.append(Objects.toString(labels[i], ""));
                        }
                        return sb.toString();
                    }));

            // 2. 每个标签路径一行，行内对所有能源汇总
            for (Map.Entry<String, List<StatisticsInfoV2>> entry : groupedByLabelPath.entrySet()) {
                List<StatisticsInfoV2> sameLabelList = entry.getValue();
                if (CollUtil.isEmpty(sameLabelList)) {
                    continue;
                }

                // 行前面的标签列
                List<Object> row = ListUtils.newArrayList();
                StatisticsInfoV2 first = sameLabelList.get(0);
                String[] labels = {first.getLabel1(), first.getLabel2(), first.getLabel3(), first.getLabel4(), first.getLabel5()};
                for (int i = 0; i < labelDeep; i++) {
                    row.add(labels[i]);
                }

                // 构造：date -> consumption（该标签下所有能源的累加）
                Map<String, BigDecimal> labelDateSumMap = new HashMap<>();
                BigDecimal labelPeriodSum = BigDecimal.ZERO;

                for (StatisticsInfoV2 s : sameLabelList) {
                    // 时间维度数据
                    if (CollUtil.isNotEmpty(s.getStatisticsDateDataList())) {
                        for (StatisticInfoDataV2 d : s.getStatisticsDateDataList()) {
                            if (d == null) {
                                continue;
                            }
                            BigDecimal c = d.getConsumption();
                            if (c == null) {
                                continue;
                            }
                            labelDateSumMap.put(d.getDate(),
                                    addBigDecimal(labelDateSumMap.get(d.getDate()), c));
                        }
                    }

                    // 周期合计
                    if (s.getSumEnergyConsumption() != null) {
                        labelPeriodSum = addBigDecimal(labelPeriodSum, s.getSumEnergyConsumption());
                    }
                }

                // 按 header 顺序写时间列
                for (String date : tableHeader) {
                    BigDecimal c = labelDateSumMap.get(date);
                    row.add(getConvertData(c));
                    // 底部合计累计
                    sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), c));
                }

                // 周期合计列
                row.add(getConvertData(labelPeriodSum));
                sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), labelPeriodSum));

                result.add(row);
            }

            // 3. 添加底部合计行（只占标签列 + 时间列 + 周期合计）
            List<Object> bottom = ListUtils.newArrayList();
            for (int i = 0; i < labelDeep; i++) {
                bottom.add(pre);
            }
            tableHeader.forEach(date -> {
                BigDecimal c = sumConsumptionMap.get(date);
                bottom.add(getConvertData(c));
            });
            BigDecimal total = sumConsumptionMap.get("sumNum");
            bottom.add(getConvertData(total));
            result.add(bottom);

            return result;
        }

        // =============================
        // 2）按能源查看：每行 = 一种能源（忽略标签）
        // =============================
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {

            // 1. 按能源分组
            Map<String, List<StatisticsInfoV2>> groupedByEnergy = statisticsInfoList.stream()
                    .collect(Collectors.groupingBy(s -> Objects.toString(s.getEnergyName(), "")));

            for (Map.Entry<String, List<StatisticsInfoV2>> entry : groupedByEnergy.entrySet()) {
                String energyName = entry.getKey();
                List<StatisticsInfoV2> sameEnergyList = entry.getValue();
                if (CollUtil.isEmpty(sameEnergyList)) {
                    continue;
                }

                List<Object> row = ListUtils.newArrayList();
                row.add(energyName); // 第一列：能源

                Map<String, BigDecimal> energyDateSumMap = new HashMap<>();
                BigDecimal energyPeriodSum = BigDecimal.ZERO;

                for (StatisticsInfoV2 s : sameEnergyList) {
                    if (CollUtil.isNotEmpty(s.getStatisticsDateDataList())) {
                        for (StatisticInfoDataV2 d : s.getStatisticsDateDataList()) {
                            if (d == null) {
                                continue;
                            }
                            BigDecimal c = d.getConsumption();
                            if (c == null) {
                                continue;
                            }
                            energyDateSumMap.put(d.getDate(),
                                    addBigDecimal(energyDateSumMap.get(d.getDate()), c));
                        }
                    }

                    if (s.getSumEnergyConsumption() != null) {
                        energyPeriodSum = addBigDecimal(energyPeriodSum, s.getSumEnergyConsumption());
                    }
                }

                // 时间列
                for (String date : tableHeader) {
                    BigDecimal c = energyDateSumMap.get(date);
                    row.add(getConvertData(c));
                    sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), c));
                }

                // 周期合计
                row.add(getConvertData(energyPeriodSum));
                sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), energyPeriodSum));

                result.add(row);
            }

            // 2. 底部合计行：只占“能源 + 时间列 + 周期合计”
            List<Object> bottom = ListUtils.newArrayList();
            bottom.add(pre); // 能源列
            tableHeader.forEach(date -> {
                BigDecimal c = sumConsumptionMap.get(date);
                bottom.add(getConvertData(c));
            });
            BigDecimal total = sumConsumptionMap.get("sumNum");
            bottom.add(getConvertData(total));
            result.add(bottom);

            return result;
        }

        // =============================
        // 0）综合查看（默认）：保持你原来的“标签 + 能源 明细行”结构
        // =============================
        for (StatisticsInfoV2 s : statisticsInfoList) {

            List<Object> data = ListUtils.newArrayList();
            String[] labels = {s.getLabel1(), s.getLabel2(), s.getLabel3(), s.getLabel4(), s.getLabel5()};

            // 标签列
            for (int i = 0; i < labelDeep; i++) {
                data.add(labels[i]);
            }
            // 能源列
            data.add(s.getEnergyName());

            // 时间列
            List<StatisticInfoDataV2> statisticInfoDataV2List = s.getStatisticsDateDataList();
            Map<String, StatisticInfoDataV2> dateMap = CollUtil.isEmpty(statisticInfoDataV2List)
                    ? Collections.emptyMap()
                    : statisticInfoDataV2List.stream()
                    .collect(Collectors.toMap(StatisticInfoDataV2::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                StatisticInfoDataV2 d = dateMap.get(date);
                BigDecimal c = (d == null ? null : d.getConsumption());
                data.add(getConvertData(c));
                sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), c));
            });

            // 周期合计
            BigDecimal sumEnergyConsumption = s.getSumEnergyConsumption();
            data.add(getConvertData(sumEnergyConsumption));
            sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sumEnergyConsumption));

            result.add(data);
        }

        // 综合维度的底部合计：标签列 + 能源列 + 时间列 + 周期合计
        List<Object> bottom = ListUtils.newArrayList();
        for (int i = 0; i < labelDeep; i++) {
            bottom.add(pre);
        }
        bottom.add(pre); // 能源列

        tableHeader.forEach(date -> {
            BigDecimal c = sumConsumptionMap.get(date);
            bottom.add(getConvertData(c));
        });
        BigDecimal total = sumConsumptionMap.get("sumNum");
        bottom.add(getConvertData(total));
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

    private StatisticsResultV2VO<StatisticsInfoV2> defaultNullData(List<String> tableHeader) {
        StatisticsResultV2VO<StatisticsInfoV2> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);
        resultVO.setStatisticsInfoList(Collections.emptyList());
        return resultVO;
    }
}
