package cn.bitlinks.ems.module.power.service.statistics;

import com.baomidou.dynamic.datasource.annotation.DS;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticInfoDataV2;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsInfoV2;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.usagecost.UsageCostMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_RANGE_EXCEED_LIMIT;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_TYPE_NOT_EXISTS;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.END_TIME_MUST_AFTER_START_TIME;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
public class StatisticsV2ServiceImpl implements StatisticsV2Service {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StatisticsCommonService statisticsCommonService;


    @Resource
    private UsageCostService usageCostService;


    @Override
    public StatisticsResultV2VO moneyAnalysisTable(StatisticsParamV2VO paramVO) {

        // 校验时间范围是否存在
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

        Integer queryType = paramVO.getQueryType();
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        //时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        // 表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);

        //返回结果
        StatisticsResultV2VO resultVO = new StatisticsResultV2VO();
        resultVO.setHeader(tableHeader);

        //能源列表
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(new HashSet<>(paramVO.getEnergyIds()) ,paramVO.getEnergyClassify());
        //能源ID energyIds
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());


        //根据能源查询台账
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);

        //根据标签查询
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(paramVO.getTopLabel(), paramVO.getChildLabels(), standingBookIdList);

        List<Long> standingBookIds = new ArrayList<>();

        if (CollectionUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream().filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            //能源管理计量器具，标签可能关联重点设备，当不存在交集时，则无需查询
            if (ArrayUtil.isEmpty(collect)) {
                return resultVO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        }else {
            List<Long> collect = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect);
        }
        if(CollectionUtil.isEmpty(standingBookIds)){
            return resultVO;
        }


        //能源参数
        //根据台账ID查询用量跟成本
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);

        List<StatisticsInfoV2> statisticsInfoList = new ArrayList<>();
        // 1、按能源查看
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            List<StatisticsInfoV2> statisticsInfoV2s = queryByEnergy(energyList, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(statisticsInfoV2s);

        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Long> finalStandingBookIds = standingBookIds;
            //根据能源ID分组
            // 使用 Collectors.groupingBy 根据 name 和 value 分组
            Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                    .filter(s -> finalStandingBookIds.contains(s.getStandingbookId()))
                    .collect(Collectors.groupingBy(
                            StandingbookLabelInfoDO::getName,  // 第一个分组条件：按 name
                            Collectors.groupingBy(StandingbookLabelInfoDO::getValue)  // 第二个分组条件：按 value
                    ));

            List<StatisticsInfoV2> statisticsInfoV2s = queryByLabel(grouped, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(statisticsInfoV2s);

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Long> finalStandingBookIds = standingBookIds;
            //根据能源ID分组
            // 使用 Collectors.groupingBy 根据 name 和 value 分组
            Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                    .filter(s -> finalStandingBookIds.contains(s.getStandingbookId()))
                    .collect(Collectors.groupingBy(
                            StandingbookLabelInfoDO::getName,  // 第一个分组条件：按 name
                            Collectors.groupingBy(StandingbookLabelInfoDO::getValue)  // 第二个分组条件：按 value
                    ));

            List<StatisticsInfoV2> statisticsInfoV2s = queryDefault(grouped, usageCostDataList, dataTypeEnum);
            statisticsInfoList.addAll(statisticsInfoV2s);
        }
        resultVO.setStatisticsInfoList(statisticsInfoList);
        //数据最后更新时间 TODO
        resultVO.setDataTime(LocalDateTime.now());

        return resultVO;
    }

    public List<StatisticsInfoV2> queryDefault(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
                                               List<UsageCostData> usageCostDataList, DataTypeEnum dataType) {
        Set<Long> energyIdSet = usageCostDataList.stream().map(UsageCostData::getEnergyId).collect(Collectors.toSet());
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(energyIdSet, null);
        Map<Long, EnergyConfigurationDO> energyMap = energyList.stream().collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
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

                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = energyUsageMap.get(labelInfo.getStandingbookId());
                    if (usageList == null || usageList.isEmpty()) {
                        return; // 计量器具没有数据，跳过
                    }

                    Map<Long, List<UsageCostData>> energyUsageCostMap = usageList.stream().collect(Collectors.groupingBy(UsageCostData::getEnergyId));
                    energyUsageCostMap.forEach((energyId, usageCostList) -> {
                        EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);
                        List<StatisticInfoDataV2> dataList = usageList.stream()
                                .map(usage -> new StatisticInfoDataV2(
                                        //DateUtil.format(usage.getTime(), dataType.getFormat()),
                                        usage.getTime(),
                                        usage.getCurrentTotalUsage(),
                                        usage.getTotalCost()
                                ))
                                .collect(Collectors.toList());

                        BigDecimal totalConsumption = dataList.stream()
                                .map(StatisticInfoDataV2::getConsumption)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        BigDecimal totalCost = dataList.stream()
                                .map(StatisticInfoDataV2::getMoney)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        StatisticsInfoV2 info = new StatisticsInfoV2();
                        info.setEnergyId(energyId);
                        info.setEnergyName(energyConfigurationDO.getEnergyName());
                        info.setLabel1(topLabel.getLabelName());
                        info.setLabel2(label2Name);
                        info.setLabel3(label3Name);
                        info.setStatisticsDateDataList(dataList);
                        info.setSumEnergyConsumption(totalConsumption);
                        info.setSumEnergyMoney(totalCost);

                        resultList.add(info);
                    });
                });
            });
        });

        return resultList;


    }


    public List<StatisticsInfoV2> queryByLabel(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
                                               List<UsageCostData> usageCostDataList, DataTypeEnum dataType) {

        Map<Long, List<UsageCostData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

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

                labelInfoList.forEach(labelInfo -> {
                    List<UsageCostData> usageList = energyUsageMap.get(labelInfo.getStandingbookId());
                    if (usageList == null || usageList.isEmpty()) {
                        return; // 计量器具没有数据，跳过
                    }

                    List<StatisticInfoDataV2> dataList = usageList.stream()
                            .map(usage -> new StatisticInfoDataV2(
                                    //DateUtil.format(usage.getTime(), dataType.getFormat()),
                                    usage.getTime(),
                                    usage.getCurrentTotalUsage(),
                                    usage.getTotalCost()
                            ))
                            .collect(Collectors.toList());

                    BigDecimal totalConsumption = dataList.stream()
                            .map(StatisticInfoDataV2::getConsumption)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal totalCost = dataList.stream()
                            .map(StatisticInfoDataV2::getMoney)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    StatisticsInfoV2 info = new StatisticsInfoV2();
                    info.setLabel1(topLabel.getLabelName());
                    info.setLabel2(label2Name);
                    info.setLabel3(label3Name);
                    info.setStatisticsDateDataList(dataList);
                    info.setSumEnergyConsumption(totalConsumption);
                    info.setSumEnergyMoney(totalCost);

                    resultList.add(info);
                });
            });
        });

        return resultList;
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
     * 根据能源查看
     */
    public List<StatisticsInfoV2> queryByEnergy(List<EnergyConfigurationDO> energyList, List<UsageCostData> usageCostDataList, DataTypeEnum dataType) {
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

                    StatisticsInfoV2 info = new StatisticsInfoV2();
                    info.setEnergyId(energy.getId());
                    info.setEnergyName(energy.getName());


                    List<StatisticInfoDataV2> infoDataV2List = usageCostList.stream()
                            .map(usageCost -> new StatisticInfoDataV2(
                                    //DateUtil.format(usageCost.getTime(), dataType.getFormat()),
                                    usageCost.getTime(),
                                    usageCost.getCurrentTotalUsage(),
                                    usageCost.getTotalCost()
                            ))
                            .collect(Collectors.toList());

                    BigDecimal sumEnergyConsumption = infoDataV2List.stream().map(StatisticInfoDataV2::getConsumption).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal sumEnergyMoney = infoDataV2List.stream().map(StatisticInfoDataV2::getMoney).reduce(BigDecimal.ZERO, BigDecimal::add);

                    info.setStatisticsDateDataList(infoDataV2List);

                    info.setSumEnergyConsumption(sumEnergyConsumption);
                    info.setSumEnergyMoney(sumEnergyMoney);
                    return info;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}