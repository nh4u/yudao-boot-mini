package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.USAGE_COST_STRUCTURE_CHART;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.USAGE_COST_STRUCTURE_TABLE;

/**
 * @Title: ydme-doublecarbon
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/14 17:10
 **/
@Service
@Validated
@Slf4j
public class MoneyStructureV2ServiceImpl implements MoneyStructureV2Service {

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
    public StatisticsResultV2VO<StructureInfo> moneyStructureAnalysisTable(StatisticsParamV2VO paramVO) {

        // 1.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_COST_STRUCTURE_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes,StatisticsResultV2VO.class);
        }

        // 获取结果
        StatisticsResultV2VO<StructureInfo> resultVO = dealMoneyStructureAnalysisTable(paramVO);

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        // 返回查询结果。
        return resultVO;
    }

    private StatisticsResultV2VO<StructureInfo> dealMoneyStructureAnalysisTable(StatisticsParamV2VO paramVO) {
        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        // 2.1.校验查看类型
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

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
            List<StructureInfo> structureInfos = queryByEnergy(energyList, usageCostDataList);
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

            List<StructureInfo> structureInfos = queryByLabel(grouped, usageCostDataList);
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

            List<StructureInfo> structureInfos = queryDefault(grouped, usageCostDataList);
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

        return resultVO;
    }

    @Override
    public StatisticsChartPieResultVO moneyStructureAnalysisChart(StatisticsParamV2VO paramVO) {
        Integer queryType = validateQueryType(paramVO.getQueryType());
        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_COST_STRUCTURE_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, StatisticsChartPieResultVO.class);
        }

        paramVO.setQueryType(0);
        // 复用表方法的核心逻辑
        StatisticsResultV2VO<StructureInfo> tableResult = dealMoneyStructureAnalysisTable(paramVO);

        // 获取原始数据列表
        List<StructureInfo> dataList = tableResult.getStatisticsInfoList();

        // 构建饼图结果
        StatisticsChartPieResultVO resultVO = new StatisticsChartPieResultVO();

        QueryDimensionEnum queryDimensionEnum = QueryDimensionEnum.codeOf(queryType);
        switch (queryDimensionEnum) {
            case OVERALL_REVIEW:
                resultVO.setEnergyPie(buildEnergyPie(dataList, paramVO));
                resultVO.setLabelPie(buildLabelPie(dataList, paramVO));
                break;
            case ENERGY_REVIEW:
                resultVO.setEnergyPies(buildEnergyDimensionPies(dataList, paramVO));
                break;
            case LABEL_REVIEW:
                resultVO.setLabelPies(buildLabelDimensionPies(dataList, paramVO));
                break;
            default:
                throw new IllegalArgumentException("查看类型不存在");
        }

        resultVO.setDataTime(tableResult.getDataTime());

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        // 返回查询结果。
        return resultVO;
    }


    public List<StructureInfo> queryDefault(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped,
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
                                        usage.getTotalCost(),
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
                                            List<UsageCostData> usageCostDataList) {

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
                                    usage.getTotalCost(),
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
                                             List<UsageCostData> usageCostDataList) {
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
                    info.setEnergyName(energy.getEnergyName());

                    List<StructureInfoData> structureDataList = usageCostList.stream()
                            .map(usageCost -> new StructureInfoData(
                                    //DateUtil.format(usageCost.getTime(), dataType.getFormat()),
                                    usageCost.getTime(),
                                    usageCost.getTotalCost(),
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


    /**
     * 构建能源维度饼图（综合查看）
     *
     * @param dataList
     * @param paramVO
     * @return
     */
    private PieChartVO buildEnergyPie(List<StructureInfo> dataList, StatisticsParamV2VO paramVO) {
        // 过滤出选中的能源
        Set<Long> selectedEnergyIds = new HashSet<>(paramVO.getEnergyIds());
        Map<String, BigDecimal> energyMap = dataList.stream()
                .filter(vo -> selectedEnergyIds.contains(vo.getEnergyId()))
                .collect(Collectors.groupingBy(
                        vo -> vo.getEnergyId() + "|" + vo.getEnergyName(),
                        Collectors.reducing(BigDecimal.ZERO, StructureInfo::getSumNum, BigDecimal::add)
                ));

        return createPieChart("能源用能结构", energyMap);
    }

    /**
     * 构建标签维度饼图（综合查看）
     *
     * @param dataList
     * @param paramVO
     * @return
     */
    private PieChartVO buildLabelPie(List<StructureInfo> dataList, StatisticsParamV2VO paramVO) {
        Map<String, BigDecimal> labelMap = dataList.stream()
                .collect(Collectors.groupingBy(
                        this::getFullLabelPath,
                        Collectors.reducing(BigDecimal.ZERO, StructureInfo::getSumNum, BigDecimal::add)
                ));

        return createPieChart("标签用能结构", labelMap);
    }

    /**
     * 构建能源维度饼图集合（按能源查看）
     *
     * @param dataList
     * @param paramVO
     * @return
     */
    private List<PieChartVO> buildEnergyDimensionPies(List<StructureInfo> dataList, StatisticsParamV2VO paramVO) {
        List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
        return energyList.stream().map(energy -> {
            // 按一级标签聚合数据
            Map<String, BigDecimal> labelMap = dataList.stream()
                    .filter(vo -> energy.getId().equals(vo.getEnergyId()))
                    .collect(Collectors.groupingBy(
                            StructureInfo::getLabel1, // 关键修改：使用一级标签分组
                            Collectors.reducing(BigDecimal.ZERO, StructureInfo::getSumNum, BigDecimal::add)
                    ));

            String energyName = energy.getEnergyName();

            return createPieChart(energyName, labelMap);
        }).collect(Collectors.toList());
    }

    /**
     * 能源查询条件处理
     *
     * @param paramVO
     * @return
     */
    private List<EnergyConfigurationDO> dealEnergyQueryData(StatisticsParamV2VO paramVO) {
        // 能源查询条件处理
        EnergyConfigurationPageReqVO queryVO = new EnergyConfigurationPageReqVO();

        List<Long> energyIds = paramVO.getEnergyIds();
        if (CollectionUtil.isNotEmpty(energyIds)) {
            queryVO.setEnergyIds(energyIds);
        } else {
            // 默认 外购能源全部
            queryVO.setEnergyClassify(1);
        }
        // 能源list
        return energyConfigurationService.getEnergyConfigurationList(queryVO);
    }

    /**
     * 构建标签维度饼图集合（按标签查看）
     *
     * @param dataList
     * @param paramVO
     * @return
     */
    private List<PieChartVO> buildLabelDimensionPies(List<StructureInfo> dataList, StatisticsParamV2VO paramVO) {
        // 获取所有选中的labelIds

        // 过滤出选中标签的数据
        List<StructureInfo> filteredData = new ArrayList<>(dataList);

        // 按label1分组，每个分组生成一个饼图
        Map<String, List<StructureInfo>> groupedByLabel1 = filteredData.stream()
                .collect(Collectors.groupingBy(StructureInfo::getLabel1));

        // 对每个label1生成饼图
        return groupedByLabel1.entrySet().stream().map(entry -> {
            String label1 = entry.getKey();
            List<StructureInfo> labelData = entry.getValue();

            // 按能源分组，计算总用量
            Map<String, BigDecimal> energyMap = labelData.stream()
                    .collect(Collectors.groupingBy(
                            vo -> vo.getEnergyId() + "|" + vo.getEnergyName(),
                            Collectors.reducing(BigDecimal.ZERO, StructureInfo::getSumNum, BigDecimal::add)
                    ));

            return createPieChart(label1, energyMap);
        }).collect(Collectors.toList());
    }

    /**
     * 安全创建饼图
     *
     * @param title
     * @param dataMap
     * @return
     */
    private PieChartVO createPieChart(String title, Map<String, BigDecimal> dataMap) {
        BigDecimal total = dataMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PieItemVO> items = dataMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    String name = parts.length > 1 ? parts[1] : entry.getKey();

                    return new PieItemVO(
                            name,
                            entry.getValue(),
                            calculateProportion(entry.getValue(), total)
                    );
                })
                .collect(Collectors.toList());

        return new PieChartVO(title, items, total);
    }

    /**
     * 保持与表格相同的占比计算
     *
     * @param value
     * @param total
     * @return
     */
    private BigDecimal calculateProportion(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取完整标签路径
     *
     * @param vo
     * @return
     */
    private String getFullLabelPath(StructureInfo vo) {
        return Stream.of(vo.getLabel1(), vo.getLabel2(), vo.getLabel3())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("-"));
    }
}
