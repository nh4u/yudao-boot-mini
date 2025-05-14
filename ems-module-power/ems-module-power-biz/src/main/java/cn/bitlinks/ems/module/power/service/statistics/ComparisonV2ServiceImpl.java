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


    @Override
    public StatisticsResultV2VO<ComparisonItemVO> discountAnalysisTable(StatisticsParamV2VO paramVO) {
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
        List<UsageCostDiscountData> usageCostDataList = usageCostService.getDiscountList(paramVO, startTime, endTime, standingBookIds);

        // 查询上一个周期折扣数据
        LocalDateTime[] lastRange = LocalDateTimeUtils.getPreviousRange(rangeOrigin, dataTypeEnum);
        List<UsageCostDiscountData> lastUsageCostDataList = usageCostService.getDiscountList(paramVO, lastRange[0], lastRange[1], standingBookIds);

        List<ComparisonItemVO> statisticsInfoList = new ArrayList<>();

        LocalDateTime lastTime = usageCostService.getLastTime(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], standingBookIds);

        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // 按能源查看，无需构建标签分组
            statisticsInfoList.addAll(queryByEnergy(energyList, usageCostDataList, lastUsageCostDataList,dataTypeEnum));
        } else {
            // 构建标签分组结构：一级标签名 -> 二级/三级值 -> 对应标签列表
            Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped = standingbookIdsByLabel.stream()
                    .filter(s -> standingBookIds.contains(s.getStandingbookId()))
                    .collect(Collectors.groupingBy(
                            StandingbookLabelInfoDO::getName,
                            Collectors.groupingBy(StandingbookLabelInfoDO::getValue)));

            if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
                // 按标签查看
                statisticsInfoList.addAll(queryByLabel(grouped, usageCostDataList, lastUsageCostDataList,dataTypeEnum));
            } else {
                // 综合默认查看
                statisticsInfoList.addAll(queryDefault(grouped, usageCostDataList, lastUsageCostDataList,dataTypeEnum));
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
                                                 List<UsageCostDiscountData> usageCostDataList,
                                                 List<UsageCostDiscountData> lastUsageCostDataList,
                                                 DataTypeEnum dataTypeEnum) {
        // 按能源ID分组当前周期数据
        Map<Long, List<UsageCostDiscountData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostDiscountData::getEnergyId));

        // 上期数据以 energyId + time 为key构建map，便于查找
        Map<String, UsageCostDiscountData> lastDataMap = lastUsageCostDataList.stream()
                .collect(Collectors.toMap(
                        d -> d.getEnergyId() + "_" + d.getTime(),
                        Function.identity(),
                        (a, b) -> a
                ));

        return energyList.stream()
                .filter(energy -> energyUsageMap.containsKey(energy.getId()))
                .map(energy -> {
                    List<UsageCostDiscountData> usageList = energyUsageMap.get(energy.getId());
                    if (CollectionUtil.isEmpty(usageList)) return null;

                    // 构造环比详情数据列表
                    List<ComparisonDetailVO> detailList = usageList.stream()
                            .map(current -> {
                                // 使用当前时间推算上期时间来构建 key
                                String lastTime = LocalDateTimeUtils.getPreviousTime(current.getTime(), dataTypeEnum);
                                String key = current.getEnergyId() + "_" + lastTime;
                                UsageCostDiscountData previous = lastDataMap.get(key);
                                BigDecimal now = current.getTotalDiscount();
                                BigDecimal last = previous != null ? previous.getTotalDiscount() : null;
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
    private List<ComparisonItemVO> queryByLabel(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped, List<UsageCostDiscountData> usageCostDataList,
                                                List<UsageCostDiscountData> lastUsageCostDataList, DataTypeEnum dateTypeEnum) {
        // 当前周期数据按 standingbookId 分组
        Map<Long, List<UsageCostDiscountData>> currentMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostDiscountData::getStandingbookId));

        // 上期数据以 standingbookId + time 为key 构建map
        Map<String, UsageCostDiscountData> lastMap = lastUsageCostDataList.stream()
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
                    List<UsageCostDiscountData> usageList = currentMap.get(labelInfo.getStandingbookId());
                    if (CollectionUtil.isEmpty(usageList)) return;

                    // 构造环比详情列表
                    List<ComparisonDetailVO> dataList = usageList.stream()
                            .map(current -> {
                                String previousTime = LocalDateTimeUtils.getPreviousTime(current.getTime(), dateTypeEnum);
                                String key = current.getStandingbookId() + "_" + previousTime;
                                UsageCostDiscountData previous = lastMap.get(key);
                                BigDecimal now = current.getTotalDiscount();
                                BigDecimal last = previous != null ? previous.getTotalDiscount() : null;
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
    private List<ComparisonItemVO> queryDefault(Map<String, Map<String, List<StandingbookLabelInfoDO>>> grouped, List<UsageCostDiscountData> usageCostDataList,
                                                List<UsageCostDiscountData> lastUsageCostDataList, DataTypeEnum dateTypeEnum) {
        // 提取所有能源ID
        Set<Long> energyIdSet = usageCostDataList.stream().map(UsageCostDiscountData::getEnergyId).collect(Collectors.toSet());
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(energyIdSet, null);
        Map<Long, EnergyConfigurationDO> energyMap = energyList.stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        // 查询所有标签配置
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig().stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        // 当前周期数据按 standingbookId 分组
        Map<Long, List<UsageCostDiscountData>> energyUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostDiscountData::getStandingbookId));

        // 上期数据构建 key = standingbookId_energyId_time 的 map
        Map<String, UsageCostDiscountData> lastMap = lastUsageCostDataList.stream()
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
                    List<UsageCostDiscountData> usageList = energyUsageMap.get(labelInfo.getStandingbookId());
                    if (CollectionUtil.isEmpty(usageList)) return;

                    // 当前计量器具下按 energyId 再分组
                    Map<Long, List<UsageCostDiscountData>> energyUsageCostMap = usageList.stream()
                            .collect(Collectors.groupingBy(UsageCostDiscountData::getEnergyId));

                    energyUsageCostMap.forEach((energyId, usageCostList) -> {
                        EnergyConfigurationDO energyConfigurationDO = energyMap.get(energyId);
                        if (energyConfigurationDO == null) return;

                        // 构造明细列表
                        List<ComparisonDetailVO> dataList = usageCostList.stream()
                                .map(current -> {
                                    String previousTime = LocalDateTimeUtils.getPreviousTime(current.getTime(), dateTypeEnum);
                                    String key = current.getStandingbookId() + "_" + energyId + "_" + previousTime;
                                    UsageCostDiscountData previous = lastMap.get(key);
                                    BigDecimal now = current.getTotalDiscount();
                                    BigDecimal last = previous != null ? previous.getTotalDiscount() : null;
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
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return now.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

}
