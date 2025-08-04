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
import static cn.bitlinks.ems.module.power.enums.ExportConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.DEFAULT;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.*;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
@Slf4j
public class StatisticsV2ServiceImpl implements StatisticsV2Service {

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

    // 后续可能根据三目运算符来取动态的有效数字位scale
    private Integer scale = DEFAULT_SCALE;

    @Override
    public StatisticsResultV2VO<StatisticsInfoV2> moneyAnalysisTable(StatisticsParamV2VO paramVO) {

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
        String cacheKey = USAGE_COST_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsResultV2VO<StatisticsInfoV2>>() {});
        }

        // 表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);

        //返回结果
        StatisticsResultV2VO<StatisticsInfoV2> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);

        //能源列表
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        if (CollUtil.isEmpty(energyList)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }
        //能源ID energyIds
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        List<Long> standingBookIds = new ArrayList<>();

        //根据能源查询台账
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);

        //根据标签查询
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());

        String topLabel = paramVO.getTopLabel();
        String childLabels = paramVO.getChildLabels();
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(topLabel, childLabels);

        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream().filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            //能源管理计量器具，标签可能关联重点设备，当不存在交集时，则无需查询
            if (ArrayUtil.isEmpty(collect)) {
                resultVO.setDataTime(LocalDateTime.now());
                return resultVO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            standingBookIds.addAll(standingBookIdList);
        }
        if (CollUtil.isEmpty(standingBookIds)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }
        //按能源查看就需要找对应的根节点待定 TODO
/*        if(QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)){
            //能源与计量器具根节点map
            Map<Long, Set<Long>> rootNodeStandingbooks = statisticsCommonService.getRootNodeStandingbooks();
            standingBookIds = energyIds.stream()
                    .filter(rootNodeStandingbooks::containsKey)
                    .flatMap(id -> rootNodeStandingbooks.get(id).stream())
                    .collect(Collectors.toList());

        }*/


        //能源参数
        //根据台账ID查询用量跟成本
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);
        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);

        List<StatisticsInfoV2> statisticsInfoList = new ArrayList<>();
        // 1、按能源查看
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            List<StatisticsInfoV2> statisticsInfoV2s = queryByEnergy(energyList, usageCostDataList);
            statisticsInfoList.addAll(statisticsInfoV2s);

        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 2、按标签查看
            List<StatisticsInfoV2> standardCoalInfos = queryByLabel(topLabel, childLabels, standingbookIdsByLabel, usageCostDataList);
            statisticsInfoList.addAll(standardCoalInfos);

        } else {
            // 0、综合查看（默认）
            List<StatisticsInfoV2> statisticsInfoV2s = queryDefault(topLabel, childLabels, standingbookIdsByLabel, usageCostDataList);
            statisticsInfoList.addAll(statisticsInfoV2s);
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
                        standardCoalInfoData.setMoney(BigDecimal.ZERO);
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

        resultVO.setDataTime(lastTime);
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
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
                                BigDecimal totalCost = list.stream()
                                        .map(UsageCostData::getTotalCost)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, totalCost);
                            }
                    )
            )).values());

            BigDecimal totalConsumption = dataList.stream()
                    .map(StatisticInfoDataV2::getConsumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCost = dataList.stream()
                    .map(StatisticInfoDataV2::getMoney)
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
                i.setMoney(dealBigDecimalScale(i.getMoney(), scale));
                i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
            }).collect(Collectors.toList());

            info.setStatisticsDateDataList(dataList);
            info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, scale));
            info.setSumEnergyMoney(dealBigDecimalScale(totalCost, scale));

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
                                        BigDecimal totalCost = list.stream()
                                                .map(UsageCostData::getTotalCost)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                                        return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, totalCost);
                                    }
                            )
                    )).values());

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
                    info.setLabel4(label4Name);
                    info.setLabel5(label5Name);

                    dataList = dataList.stream().peek(i -> {
                        i.setMoney(dealBigDecimalScale(i.getMoney(), scale));
                        i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
                    }).collect(Collectors.toList());

                    info.setStatisticsDateDataList(dataList);
                    info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, scale));
                    info.setSumEnergyMoney(dealBigDecimalScale(totalCost, scale));

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
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    BigDecimal totalCost = list.stream()
                                            .map(UsageCostData::getTotalCost)
                                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                                    return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, totalCost);
                                }
                        )
                )).values());
        //按标签统计时候 用量不用合计
//        BigDecimal totalConsumption = dataList.stream()
//                .map(StatisticInfoDataV2::getConsumption)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = dataList.stream()
                .map(StatisticInfoDataV2::getMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
            i.setMoney(dealBigDecimalScale(i.getMoney(), scale));
            i.setConsumption(dealBigDecimalScale(BigDecimal.ZERO, scale));
        }).collect(Collectors.toList());

        info.setStatisticsDateDataList(dataList);
        info.setSumEnergyConsumption(dealBigDecimalScale(BigDecimal.ZERO, scale));
        info.setSumEnergyMoney(dealBigDecimalScale(totalCost, scale));

        resultList.add(info);

        return resultList;
    }


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
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                                            BigDecimal totalCost = list.stream()
                                                    .map(UsageCostData::getTotalCost)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                                            return new StatisticInfoDataV2(list.get(0).getTime(), totalConsumption, totalCost);
                                        }
                                )
                        )).values());

                //按标签统计时候 用量不用合计
//                BigDecimal totalConsumption = dataList.stream()
//                        .map(StatisticInfoDataV2::getConsumption)
//                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalCost = dataList.stream()
                        .map(StatisticInfoDataV2::getMoney)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                StatisticsInfoV2 info = new StatisticsInfoV2();
                info.setLabel1(topLabel.getLabelName());
                info.setLabel2(label2Name);
                info.setLabel3(label3Name);
                info.setLabel4(label4Name);
                info.setLabel5(label5Name);

                dataList = dataList.stream().peek(i -> {
                    i.setMoney(dealBigDecimalScale(i.getMoney(), scale));
                    i.setConsumption(dealBigDecimalScale(BigDecimal.ZERO, scale));
                }).collect(Collectors.toList());


                info.setStatisticsDateDataList(dataList);
                info.setSumEnergyConsumption(dealBigDecimalScale(BigDecimal.ZERO, scale));
                info.setSumEnergyMoney(dealBigDecimalScale(totalCost, scale));

                resultList.add(info);
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
    public List<StatisticsInfoV2> queryByEnergy(List<EnergyConfigurationDO> energyList, List<UsageCostData> usageCostDataList) {
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

                    StatisticsInfoV2 info = new StatisticsInfoV2();
                    info.setEnergyId(energy.getId());
                    info.setEnergyName(energy.getEnergyName());


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

                    infoDataV2List = infoDataV2List.stream().peek(i -> {
                        i.setMoney(dealBigDecimalScale(i.getMoney(), scale));
                        i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
                    }).collect(Collectors.toList());

                    info.setStatisticsDateDataList(infoDataV2List);

                    info.setSumEnergyConsumption(dealBigDecimalScale(sumEnergyConsumption, scale));
                    info.setSumEnergyMoney(dealBigDecimalScale(sumEnergyMoney, scale));
                    return info;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 用能成本分析图
     *
     * @param paramVO
     */
    @Override
    public StatisticsChartResultV2VO moneyAnalysisChart(StatisticsParamV2VO paramVO) {
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

        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        //时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }
        String cacheKey = USAGE_COST_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsChartResultV2VO.class);
        }


        //能源列表
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(new HashSet<>(paramVO.getEnergyIds()), paramVO.getEnergyClassify());
        StatisticsChartResultV2VO resultV2VO = new StatisticsChartResultV2VO();
        if (CollUtil.isEmpty(energyList)) {
            resultV2VO.setDataTime(LocalDateTime.now());
            return resultV2VO;
        }
        //能源ID energyIds
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());


        //根据能源查询台账
        List<StandingbookDO> standingbookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);

        //根据标签查询
        List<Long> standingBookIdList = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(paramVO.getTopLabel(), paramVO.getChildLabels(), standingBookIdList);

        List<Long> standingBookIds = new ArrayList<>();
        if (CollUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> sids = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbookIdsByEnergy.stream().filter(s -> sids.contains(s.getId())).collect(Collectors.toList());
            //能源管理计量器具，标签可能关联重点设备，当不存在交集时，则无需查询
            if (ArrayUtil.isEmpty(collect)) {
                resultV2VO.setDataTime(LocalDateTime.now());
                return resultV2VO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            List<Long> collect = standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect);
        }


        if (CollUtil.isEmpty(standingBookIds)) {
            resultV2VO.setDataTime(LocalDateTime.now());
            return resultV2VO;
        }
        Integer queryType = paramVO.getQueryType();
        //按能源查看就需要找对应的根节点 待定  TODO
/*        if(QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)){
            //能源与计量器具根节点map
            Map<Long, Set<Long>> rootNodeStandingbooks = statisticsCommonService.getRootNodeStandingbooks();
            standingBookIds = energyIds.stream()
                    .filter(rootNodeStandingbooks::containsKey)
                    .flatMap(id -> rootNodeStandingbooks.get(id).stream())
                    .collect(Collectors.toList());

        }*/
        //能源参数
        //根据台账ID查询用量跟成本
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);
        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);


        // x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultV2VO.setXdata(xdata);

        resultV2VO.setDataTime(lastTime);

        // 按能源查看
        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            Map<Long, Map<String, BigDecimal>> energyTimeCostMap = usageCostDataList.stream()
                    .collect(Collectors.groupingBy(
                            UsageCostData::getEnergyId,
                            Collectors.toMap(
                                    UsageCostData::getTime,
                                    UsageCostData::getTotalCost
                            )
                    ));
            Map<Long, EnergyConfigurationDO> energyMap = energyList.stream().collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));
            List<StatisticsChartYInfoV2VO> ydata = energyMap.entrySet().stream()
                    .filter(entry -> energyTimeCostMap.containsKey(entry.getKey())) // 仅处理有数据的 energy
                    .map(entry -> {
                        Long energyId = entry.getKey();
                        EnergyConfigurationDO energy = entry.getValue();
                        Map<String, BigDecimal> timeCostMap = energyTimeCostMap.getOrDefault(energyId, Collections.emptyMap());

                        List<StatisticsChartYDataV2VO> dataList = xdata.stream().map(time -> {
                            time = dealStrTime(time);
                            StatisticsChartYDataV2VO vo = new StatisticsChartYDataV2VO();
                            vo.setCost(dealBigDecimalScale(timeCostMap.getOrDefault(time, BigDecimal.ZERO), scale));
                            return vo;
                        }).collect(Collectors.toList());

                        StatisticsChartYInfoV2VO yInfo = new StatisticsChartYInfoV2VO();
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
            List<Long> finalStandingBookIds = standingBookIds;
            Map<String, List<StandingbookLabelInfoDO>> labelGrouped = standingbookIdsByLabel.stream()
                    .filter(label -> finalStandingBookIds.contains(label.getStandingbookId()))
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
                BigDecimal cost = data.getTotalCost();

                String labelKey = standingbookIdToLabel.get(standingbookId);
                if (labelKey == null) continue;

                labelTimeCostMap
                        .computeIfAbsent(labelKey, k -> new HashMap<>())
                        .merge(time, cost, BigDecimal::add);
            }

            //构建结果
            List<StatisticsChartYInfoV2VO> infoV2VOS = new ArrayList<>();
            labelTimeCostMap.forEach((labelKey, timeCostMap) -> {
                LabelConfigDO labelConfigDO = labelMap.get(labelKey);
                if (labelConfigDO == null) return;

                List<StatisticsChartYDataV2VO> ydata = xdata.stream().map(x -> {
                    x = dealStrTime(x);
                    BigDecimal cost = timeCostMap.getOrDefault(x, BigDecimal.ZERO);
                    StatisticsChartYDataV2VO vo = new StatisticsChartYDataV2VO();
                    vo.setCost(cost.compareTo(BigDecimal.ZERO) > 0 ? cost : BigDecimal.ZERO);
                    return vo;
                }).collect(Collectors.toList());

                StatisticsChartYInfoV2VO yInfo = new StatisticsChartYInfoV2VO();
                yInfo.setId(labelConfigDO.getId());
                yInfo.setName(labelConfigDO.getLabelName());
                yInfo.setData(ydata);
                infoV2VOS.add(yInfo);
            });

            resultV2VO.setYdata(infoV2VOS);
        } else {//综合查看
            //根据日期计算最大 / 最小 / 平均 / 总和
            Map<String, StatsResult> statsResultMap = CalculateUtil.calculateGroupStats(usageCostDataList, UsageCostData::getTime, UsageCostData::getTotalCost);
            List<StatisticsChartYInfoV2VO> ydata = new ArrayList<>();
            xdata.forEach(s -> {
                // substring 返回 endIndex-beginIndex哥字符 因为是[ )
                String subs = dealStrTime(s);
                StatsResult statsResult = statsResultMap.get(subs);
                StatisticsChartYInfoV2VO yInfoV2VO = new StatisticsChartYInfoV2VO();
                StatisticsChartYDataV2VO dataV2VO = new StatisticsChartYDataV2VO();
                if (Objects.nonNull(statsResult)) {
                    dataV2VO.setAvg(dealBigDecimalScale(statsResult.getAvg(), scale));
                    dataV2VO.setMax(dealBigDecimalScale(statsResult.getMax(), scale));
                    dataV2VO.setMin(dealBigDecimalScale(statsResult.getMin(), scale));
                    dataV2VO.setCost(dealBigDecimalScale(statsResult.getSum(), scale));
                } else {
                    dataV2VO.setAvg(BigDecimal.ZERO);
                    dataV2VO.setMax(BigDecimal.ZERO);
                    dataV2VO.setMin(BigDecimal.ZERO);
                    dataV2VO.setCost(BigDecimal.ZERO);
                }
                yInfoV2VO.setData(Collections.singletonList(dataV2VO));
                ydata.add(yInfoV2VO);
            });
            resultV2VO.setYdata(ydata);
        }
        String jsonStr = JSONUtil.toJsonStr(resultV2VO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultV2VO;

    }

    @Override
    public List<List<String>> getExcelHeader(StatisticsParamV2VO paramVO) {

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
        switch (queryType) {
            case 0:
                // 综合
                sheetName = COST_ALL;
                list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                for (int i = 2; i <= labelDeep; i++) {
                    String subLabel = "标签" + i;
                    list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                }
                list.add(Arrays.asList(sheetName, labelName, strTime, "能源", "能源"));
                break;
            case 1:
                // 按能源
                sheetName = COST_ENERGY;
                list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "能源", "能源"));
                break;
            case 2:
                // 按标签
                sheetName = COST_LABEL;
                list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签", "标签"));
                for (int i = 2; i <= labelDeep; i++) {
                    String subLabel = "标签" + i;
                    list.add(Arrays.asList(sheetName, labelName, strTime, subLabel, subLabel));
                }
                break;
            default:
                sheetName = DEFAULT;
        }

        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, labelName, strTime, x, "用量"));
            list.add(Arrays.asList(sheetName, labelName, strTime, x, getHeaderDesc(unit, 2, "用能成本")));
        });

        // 周期合计
        list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计", "用量"));
        list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计", getHeaderDesc(unit, 2, "用能成本")));
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

        // 验证单位
        Integer unit = paramVO.getUnit();

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        StatisticsResultV2VO<StatisticsInfoV2> resultVO = moneyAnalysisTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<StatisticsInfoV2> statisticsInfoList = resultVO.getStatisticsInfoList();
        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);

        Integer queryType = paramVO.getQueryType();

        // 底部合计map
        Map<String, BigDecimal> sumCostMap = new HashMap<>();

        for (StatisticsInfoV2 s : statisticsInfoList) {

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
            List<StatisticInfoDataV2> statisticInfoDataV2List = s.getStatisticsDateDataList();

            Map<String, StatisticInfoDataV2> dateMap = statisticInfoDataV2List.stream()
                    .collect(Collectors.toMap(StatisticInfoDataV2::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                StatisticInfoDataV2 statisticInfoDataV2 = dateMap.get(date);
                if (statisticInfoDataV2 == null) {
                    data.add("/");
                    data.add("/");
                } else {
                    BigDecimal consumption = statisticInfoDataV2.getConsumption();
                    BigDecimal cost = statisticInfoDataV2.getMoney();
                    data.add(getConvertData(consumption));
                    data.add(getConvertData(unit, 2, cost));

                    // 底部合计处理
                    sumCostMap.put(date, addBigDecimal(sumCostMap.get(date), cost));
                }

            });

            BigDecimal sumEnergyConsumption = s.getSumEnergyConsumption();
            BigDecimal sumCost = s.getSumEnergyMoney();
            // 处理周期合计
            data.add(getConvertData(sumEnergyConsumption));
            data.add(getConvertData(unit, 2, sumCost));

            // 处理底部合计
            sumCostMap.put("sumNum", addBigDecimal(sumCostMap.get("sumNum"), sumCost));

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

            // 用量
            bottom.add("/");

            // 折价
            BigDecimal cost = sumCostMap.get(date);
            bottom.add(getConvertData(unit, 2, cost));

        });

        // 底部周期合计
        // 用量
        bottom.add("/");

        // 折价
        BigDecimal cost = sumCostMap.get("sumNum");
        bottom.add(getConvertData(unit, 2, cost));
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

    private void validateUnit(Integer unit) {
        if (Objects.isNull(unit)) {
            throw exception(UNIT_NOT_EMPTY);
        }
    }
}

