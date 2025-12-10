package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.infra.api.config.ConfigApi;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.*;
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
import de.danielbechler.util.Strings;
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
import static cn.bitlinks.ems.module.power.enums.ConsumptionStatisticsCacheConstants.ELECTRICITY_CONSUMPTION_STATISTICS_CHART;
import static cn.bitlinks.ems.module.power.enums.ConsumptionStatisticsCacheConstants.ELECTRICITY_CONSUMPTION_STATISTICS_TABLE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.*;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * 用电量统计 Service 实现类
 *
 * @author bmqi
 */
@Service
@Validated
@Slf4j
public class ConsumptionStatisticsServiceImpl implements ConsumptionStatisticsService {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private EnergyGroupService energyGroupService;

    @Resource
    private StatisticsCommonService statisticsCommonService;


    @Resource
    private UsageCostService usageCostService;
    @Resource
    private ConfigApi configApi;
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    // 后续可能根据三目运算符来取动态的有效数字位scale
    private Integer scale = DEFAULT_SCALE;

    @Override
    public ConsumptionStatisticsResultVO<ConsumptionStatisticsInfo> consumptionStatisticsTable(ConsumptionStatisticsParamVO paramVO) {
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
        if (Strings.isEmpty(paramVO.getTopLabels())) {
            throw exception(TOP_LABELS_NOT_EXISTS);
        }

        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        //时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }
        String cacheKey = ELECTRICITY_CONSUMPTION_STATISTICS_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<ConsumptionStatisticsResultVO<ConsumptionStatisticsInfo>>() {
            });
        }

        // 表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);

        //返回结果
        ConsumptionStatisticsResultVO<ConsumptionStatisticsInfo> resultVO = new ConsumptionStatisticsResultVO<>();
        resultVO.setHeader(tableHeader);

        // 能源列表--只需要查找电力的能源
        // name为“电力”且deleted为0的能源分组，取其id值
        EnergyGroupDO energyGroup = energyGroupService.getEnergyGroup("电力");
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyGroup(energyGroup.getId());
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

//        String topLabel = paramVO.getTopLabel();
//        String childLabels = paramVO.getChildLabels();
        // 查询多级标签

        List<Map<String,String>> topLabelMapList = statisticsCommonService.splitLabels(paramVO.getTopLabels());
        Map<String,List<StandingbookLabelInfoDO>> standingbookIdsByLabelAllMap = new LinkedHashMap<>();

        Map<String,List<StandingbookLabelInfoDO>> standingbookIdsByLabelAllTopMap = new LinkedHashMap<>();
        List<StandingbookLabelInfoDO> standingbookIdsByLabelAllList = new ArrayList<>();
        for(Map<String,String> topLabelMap : topLabelMapList){
            topLabelMap.forEach((k,v)->{
                List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(k, v);
                if(Strings.isEmpty(v)){
                    standingbookIdsByLabelAllTopMap.put(k,standingbookIdsByLabel);
                }else{
                    standingbookIdsByLabelAllMap.put(k,standingbookIdsByLabel);
                }
                standingbookIdsByLabelAllList.addAll(standingbookIdsByLabel);
            });
        }

        // 全厂= 用能单位-》燕东微+ 用能单位-》
        String allFactoryLabels = configApi.getConfigValueByKey(CONFIG_POWER_LABEL_ALL).getCheckedData();
        List<Map<String,String>> allFactoryLabelsMapList = statisticsCommonService.splitLabels(allFactoryLabels);
        List<StandingbookLabelInfoDO> factoryList = new ArrayList<>();
        for(Map<String,String> allFactoryLabelsMap : allFactoryLabelsMapList) {
            allFactoryLabelsMap.forEach((k, v) -> {
                List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(k, v);
                factoryList.addAll(standingbookIdsByLabel);
            });
        }
        standingbookIdsByLabelAllList.addAll(factoryList);

        if (CollUtil.isNotEmpty(standingbookIdsByLabelAllList)) {
            List<Long> sids = standingbookIdsByLabelAllList.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
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

        //能源参数
        //根据台账ID查询用量跟成本
        List<UsageCostData> usageCostDataList = usageCostService.getList(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);
        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);

        // 0、综合查看（默认）
        List<ConsumptionStatisticsInfo> statisticsInfoList = new ArrayList<>();
        for(Map<String,String> topLabelMap : topLabelMapList) {
            topLabelMap.forEach((k, v) -> {
                List<ConsumptionStatisticsInfo> statisticsInfoV2s;
                if(Strings.isEmpty(v)){
                    statisticsInfoV2s = queryDefaultV2(k, v, standingbookIdsByLabelAllTopMap.get(k), usageCostDataList);
                }else{
                    statisticsInfoV2s = queryDefaultV2(k, v, standingbookIdsByLabelAllMap.get(k), usageCostDataList);
                }
                if (CollUtil.isNotEmpty(statisticsInfoV2s)) {
                    statisticsInfoList.addAll(statisticsInfoV2s);
                }
            });
        }
        resultVO.setStatisticsInfoList(statisticsInfoList);

        resultVO.setAllFactoryList(queryDefaultAllFactory(factoryList, usageCostDataList));
        resultVO.setDataTime(lastTime);
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }


    public List<ConsumptionStatisticsInfo> queryDefaultAllFactory(
                                                        List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                        List<UsageCostData> usageCostDataList) {

        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        return queryAllFactoryLabel(standingBookUsageMap, standingbookIdsByLabel);

    }
    public List<ConsumptionStatisticsInfo> queryDefaultV2(String topLabel,
                                                        String childLabels,
                                                        List<StandingbookLabelInfoDO> standingbookIdsByLabel,
                                                        List<UsageCostData> usageCostDataList) {

        // 标签list转换成map
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        if (CharSequenceUtil.isNotBlank(topLabel) && Strings.isEmpty(childLabels)) {
            // 只有顶级标签
            return queryDefaultTopLabel(standingBookUsageMap, labelMap, standingbookIdsByLabel);
        } else {
            // 有顶级、有子集标签
            return queryDefaultSubLabel(standingBookUsageMap, labelMap, standingbookIdsByLabel);
        }
    }
    public List<ConsumptionStatisticsInfo> queryAllFactoryLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                                List<StandingbookLabelInfoDO> standingbookIdsByLabel) {

        List<ConsumptionStatisticsInfo> resultList = new ArrayList<>();

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
        // 聚合数据 转换成 ConsumptionStatisticsInfoData
        List<ConsumptionStatisticsInfoData> dataList = new ArrayList<>(labelUsageCostDataList.stream().collect(Collectors.groupingBy(
                UsageCostData::getTime,
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            BigDecimal totalConsumption = list.stream()
                                    .map(UsageCostData::getCurrentTotalUsage)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal::add).orElse(null);
                            BigDecimal totalCost = list.stream()
                                    .map(UsageCostData::getTotalCost)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal::add).orElse(null);
                            return new ConsumptionStatisticsInfoData(list.get(0).getTime(), totalConsumption, totalCost);
                        }
                )
        )).values());

        BigDecimal totalConsumption = dataList.stream()
                .map(ConsumptionStatisticsInfoData::getConsumption)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);

        ConsumptionStatisticsInfo info = new ConsumptionStatisticsInfo();

        // 获取标签信息 - 使用第一个标签信息，因为只选择了一个顶级标签

        info.setLabel1(REPORT_ALL_FACTORY_LABEL);
        info.setLabel2(REPORT_ALL_FACTORY_LABEL);
        info.setLabel3(REPORT_ALL_FACTORY_LABEL);
        info.setLabel4(REPORT_ALL_FACTORY_LABEL);
        info.setLabel5(REPORT_ALL_FACTORY_LABEL);

        dataList = dataList.stream().peek(i -> {
            i.setMoney(dealBigDecimalScale(i.getMoney(), scale));
            i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
        }).collect(Collectors.toList());

        info.setStatisticsDateDataList(dataList);
        info.setSumEnergyConsumption(dealBigDecimalScale(totalConsumption, scale));

        resultList.add(info);

        return resultList;
    }
    public List<ConsumptionStatisticsInfo> queryDefaultTopLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                                Map<Long, LabelConfigDO> labelMap,
                                                                List<StandingbookLabelInfoDO> standingbookIdsByLabel) {

        List<ConsumptionStatisticsInfo> resultList = new ArrayList<>();

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
        // 聚合数据 转换成 ConsumptionStatisticsInfoData
        List<ConsumptionStatisticsInfoData> dataList = new ArrayList<>(labelUsageCostDataList.stream().collect(Collectors.groupingBy(
                UsageCostData::getTime,
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            BigDecimal totalConsumption = list.stream()
                                    .map(UsageCostData::getCurrentTotalUsage)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal::add).orElse(null);
                            BigDecimal totalCost = list.stream()
                                    .map(UsageCostData::getTotalCost)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal::add).orElse(null);
                            return new ConsumptionStatisticsInfoData(list.get(0).getTime(), totalConsumption, totalCost);
                        }
                )
        )).values());

        BigDecimal totalConsumption = dataList.stream()
                .map(ConsumptionStatisticsInfoData::getConsumption)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);
        BigDecimal totalCost = dataList.stream()
                .map(ConsumptionStatisticsInfoData::getMoney)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);

        ConsumptionStatisticsInfo info = new ConsumptionStatisticsInfo();

        // 获取标签信息 - 使用第一个标签信息，因为只选择了一个顶级标签
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

        resultList.add(info);

        return resultList;
    }

    public List<ConsumptionStatisticsInfo> queryDefaultSubLabel(Map<Long, List<UsageCostData>> standingBookUsageMap,
                                                                Map<Long, LabelConfigDO> labelMap,
                                                                List<StandingbookLabelInfoDO> standingbookIdsByLabel) {
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

        List<ConsumptionStatisticsInfo> resultList = new ArrayList<>();

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

                // 按日期聚合（不再按能源拆分）
                List<ConsumptionStatisticsInfoData> dataList = new ArrayList<>(labelUsageCostDataList.stream()
                        .collect(Collectors.groupingBy(
                                UsageCostData::getTime,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list -> {
                                            BigDecimal totalConsumption = list.stream()
                                                    .map(UsageCostData::getCurrentTotalUsage)
                                                    .filter(Objects::nonNull)
                                                    .reduce(BigDecimal::add).orElse(null);
                                            BigDecimal totalCost = list.stream()
                                                    .map(UsageCostData::getTotalCost)
                                                    .filter(Objects::nonNull)
                                                    .reduce(BigDecimal::add).orElse(null);
                                            return new ConsumptionStatisticsInfoData(list.get(0).getTime(), totalConsumption, totalCost);
                                        }
                                )
                        )).values());

                BigDecimal totalConsumption = dataList.stream()
                        .map(ConsumptionStatisticsInfoData::getConsumption)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal::add).orElse(null);
                BigDecimal totalCost = dataList.stream()
                        .map(ConsumptionStatisticsInfoData::getMoney)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal::add).orElse(null);

                ConsumptionStatisticsInfo info = new ConsumptionStatisticsInfo();
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
                // 为保持一致性，成本合计也计算保留（虽然该表只展示用量）
                // 如果前端不使用，可忽略该字段
                
                resultList.add(info);
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
                                            .filter(Objects::nonNull)
                                            .reduce(BigDecimal::add).orElse(null);
                                    BigDecimal totalCost = list.stream()
                                            .map(UsageCostData::getTotalCost)
                                            .filter(Objects::nonNull)
                                            .reduce(BigDecimal::add).orElse(null);
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
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add).orElse(null);

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
                                                    .filter(Objects::nonNull)
                                                    .reduce(BigDecimal::add).orElse(null);
                                            BigDecimal totalCost = list.stream()
                                                    .map(UsageCostData::getTotalCost)
                                                    .filter(Objects::nonNull)
                                                    .reduce(BigDecimal::add).orElse(null);
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
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal::add).orElse(null);

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

                    BigDecimal sumEnergyConsumption = infoDataV2List.stream().map(StatisticInfoDataV2::getConsumption).filter(Objects::nonNull)
                            .reduce(BigDecimal::add).orElse(null);
                    BigDecimal sumEnergyMoney = infoDataV2List.stream().map(StatisticInfoDataV2::getMoney).filter(Objects::nonNull)
                            .reduce(BigDecimal::add).orElse(null);

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
    public ConsumptionStatisticsChartResultVO<ConsumptionStatisticsChartYInfo> consumptionStatisticsChart(ConsumptionStatisticsParamVO paramVO) {
        paramVO.setQueryType(QueryDimensionEnum.OVERALL_REVIEW.getCode());

        // 查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = ELECTRICITY_CONSUMPTION_STATISTICS_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            // 泛型放缓存避免强转问题
            return JSON.parseObject(cacheRes, new TypeReference<ConsumptionStatisticsChartResultVO<ConsumptionStatisticsChartYInfo>>() {
            });
        }

        // 4.如果没有则去数据库查询
        ConsumptionStatisticsChartResultVO<ConsumptionStatisticsChartYInfo> resultVO = new ConsumptionStatisticsChartResultVO<>();
        resultVO.setDataTime(LocalDateTime.now());


        ConsumptionStatisticsResultVO<ConsumptionStatisticsInfo> resultTable = consumptionStatisticsTable(paramVO);

        // x轴
        List<String> xdata = resultTable.getHeader();
        resultVO.setXdata(xdata);

        List<ConsumptionStatisticsInfo> statisticsInfoList = resultTable.getStatisticsInfoList();

        // 底部合计map
        Map<String, BigDecimal> sumConsumptionMap = new HashMap<>();
        List<ConsumptionStatisticsChartYInfo> yInfoList = new ArrayList<>();
        for (ConsumptionStatisticsInfo s : statisticsInfoList) {

            ConsumptionStatisticsChartYInfo yInfo = new ConsumptionStatisticsChartYInfo();
            yInfo.setName(getName(s.getLabel1(), s.getLabel2(), s.getLabel3(), s.getLabel4(), s.getLabel5()));

            // 处理数据
            List<ConsumptionStatisticsInfoData> statisticInfoDataV2List = s.getStatisticsDateDataList();
            Map<String, ConsumptionStatisticsInfoData> dateMap = statisticInfoDataV2List.stream()
                    .collect(Collectors.toMap(ConsumptionStatisticsInfoData::getDate, Function.identity()));

            List<BigDecimal> data = ListUtils.newArrayList();
            xdata.forEach(date -> {
                ConsumptionStatisticsInfoData statisticInfoDataV2 = dateMap.get(date);
                if (statisticInfoDataV2 == null) {
                    data.add(BigDecimal.ZERO);
                } else {
                    BigDecimal consumption = statisticInfoDataV2.getConsumption();
                    data.add(!Objects.isNull(consumption) ? consumption : BigDecimal.ZERO);
                    // 底部合计处理
                    sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), consumption));
                }
            });
            yInfo.setData(data);

            // 处理底部合计
            BigDecimal sumCost = s.getSumEnergyConsumption();
            sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sumCost));

            yInfoList.add(yInfo);
        }

        // 汇总数据
        List<BigDecimal> summary = ListUtils.newArrayList();
        ConsumptionStatisticsChartYInfo yInfo = new ConsumptionStatisticsChartYInfo();
        yInfo.setName("汇总");
        xdata.forEach(date -> {
            // 折价
            BigDecimal consumption = sumConsumptionMap.get(date);
            summary.add(!Objects.isNull(consumption) ? consumption : BigDecimal.ZERO);
        });
        yInfo.setData(summary);

        yInfoList.add(yInfo);
        resultVO.setYdata(yInfoList);

        return resultVO;

    }

    private String getName(String label1, String label2, String label3, String label4, String label5) {
        if (CharSequenceUtil.isNotEmpty(label5) && !"/".equals(label5)) {
            return label5;
        }
        if (CharSequenceUtil.isNotEmpty(label4) && !"/".equals(label4)) {
            return label4;
        }
        if (CharSequenceUtil.isNotEmpty(label3) && !"/".equals(label3)) {
            return label3;
        }
        if (CharSequenceUtil.isNotEmpty(label2) && !"/".equals(label2)) {
            return label2;
        }
        if (CharSequenceUtil.isNotEmpty(label1) && !"/".equals(label1)) {
            return label1;
        }
        return null;
    }

    @Override
    public List<List<String>> getExcelHeader(ConsumptionStatisticsParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];

        // 表头数据
        List<List<String>> list = ListUtils.newArrayList();
        // 统计周期
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);

        // 统计标签
        String labelName = getLabelName(paramVO.getTopLabels());
        Integer labelDeep = getLabelDeepV2(paramVO.getTopLabels());
        // 表单名称
        Integer queryType = paramVO.getQueryType();
        String sheetName;
        switch (queryType) {
            case 0:
                // 综合
                sheetName = CONSUMPTION_STATISTICS_ALL;
                list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签"));
                for (int i = 2; i <= labelDeep; i++) {
                    String subLabel = "标签" + i;
                    list.add(Arrays.asList(sheetName, labelName, strTime, subLabel));
                }
                break;
            case 1:
                // 按能源
                sheetName = CONSUMPTION_STATISTICS_ENERGY;
                list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "能源"));
                break;
            case 2:
                // 按标签
                sheetName = CONSUMPTION_STATISTICS_LABEL;
                list.add(Arrays.asList("表单名称", "统计标签", "统计周期", "标签"));
                for (int i = 2; i <= labelDeep; i++) {
                    String subLabel = "标签" + i;
                    list.add(Arrays.asList(sheetName, labelName, strTime, subLabel));
                }
                break;
            default:
                sheetName = DEFAULT;
        }

        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, labelName, strTime, x));
        });

        // 周期合计
        list.add(Arrays.asList(sheetName, labelName, strTime, "周期合计"));
        return list;

    }

    private String getLabelName(String topLabels) {

        List<String> childLabelValues = StrSplitter.split(topLabels, "#", 0, true, true);
        List<Long> labelIds = childLabelValues.stream()
                .map(c -> StrSplitter.split(c, ",", 0, true, true))
                .flatMap(List::stream)
                .map(Long::valueOf)
                .distinct()
                .collect(Collectors.toList());

        // 获取标签数据
        List<LabelConfigDO> labels = labelConfigService.getByIds(labelIds);

        return labels.stream().map(LabelConfigDO::getLabelName).collect(Collectors.joining("、"));
    }

    @Override
    public List<List<Object>> getExcelData(ConsumptionStatisticsParamVO paramVO) {
        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        ConsumptionStatisticsResultVO<ConsumptionStatisticsInfo> resultVO = consumptionStatisticsTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<ConsumptionStatisticsInfo> statisticsInfoList = resultVO.getStatisticsInfoList();
        List<ConsumptionStatisticsInfo> allFactoryList = resultVO.getAllFactoryList();
        Integer labelDeep = getLabelDeepV2(paramVO.getTopLabels());

        Integer queryType = paramVO.getQueryType();

        // 底部合计map
        Map<String, BigDecimal> sumConsumptionMap = new HashMap<>();

        for (ConsumptionStatisticsInfo s : statisticsInfoList) {

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
                    //data.add(s.getEnergyName());
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
            List<ConsumptionStatisticsInfoData> statisticInfoDataV2List = s.getStatisticsDateDataList();

            Map<String, ConsumptionStatisticsInfoData> dateMap = statisticInfoDataV2List.stream()
                    .collect(Collectors.toMap(ConsumptionStatisticsInfoData::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                ConsumptionStatisticsInfoData statisticInfoDataV2 = dateMap.get(date);
                if (statisticInfoDataV2 == null) {
                    //data.add("/");
                    data.add("/");
                } else {
                    BigDecimal consumption = statisticInfoDataV2.getConsumption();
                    data.add(getConvertData(consumption));

                    // 底部合计处理
                    sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), consumption));
                }

            });

            BigDecimal sumEnergyConsumption = s.getSumEnergyConsumption();
            // 处理周期合计
            data.add(getConvertData(sumEnergyConsumption));

            // 处理底部合计
            sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sumEnergyConsumption));

            result.add(data);
        }

        for (ConsumptionStatisticsInfo s : allFactoryList) {

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
                    //data.add(s.getEnergyName());
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
            List<ConsumptionStatisticsInfoData> statisticInfoDataV2List = s.getStatisticsDateDataList();

            Map<String, ConsumptionStatisticsInfoData> dateMap = statisticInfoDataV2List.stream()
                    .collect(Collectors.toMap(ConsumptionStatisticsInfoData::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                ConsumptionStatisticsInfoData statisticInfoDataV2 = dateMap.get(date);
                if (statisticInfoDataV2 == null) {
                    //data.add("/");
                    data.add("/");
                } else {
                    BigDecimal consumption = statisticInfoDataV2.getConsumption();
                    data.add(getConvertData(consumption));

                    // 底部合计处理
                    sumConsumptionMap.put(date, addBigDecimal(sumConsumptionMap.get(date), consumption));
                }

            });

            BigDecimal sumEnergyConsumption = s.getSumEnergyConsumption();
            // 处理周期合计
            data.add(getConvertData(sumEnergyConsumption));

            // 处理底部合计
            sumConsumptionMap.put("sumNum", addBigDecimal(sumConsumptionMap.get("sumNum"), sumEnergyConsumption));

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
                //bottom.add(pre);
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
            BigDecimal consumption = sumConsumptionMap.get(date);
            bottom.add(getConvertData(consumption));

        });

        // 底部周期合计
        // 用量
        BigDecimal consumption = sumConsumptionMap.get("sumNum");
        bottom.add(getConvertData(consumption));
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

}