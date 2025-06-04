package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.deviceassociationconfiguration.DeviceAssociationConfigurationService;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.label.StandingbookLabelInfoService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.USAGE_STANDARD_COAL_ENERGY_FLOW_CHART;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
@Slf4j
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private DeviceAssociationConfigurationService deviceAssociationConfigurationService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Resource
    private StatisticsCommonService statisticsCommonService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    StandingbookLabelInfoService standingbookLabelInfoService;


    @Override
    public EnergyFlowResultVO energyFlowAnalysisV2(StatisticsParamV2VO paramVO) {

        // 1.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_STANDARD_COAL_ENERGY_FLOW_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (StrUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSONUtil.toBean(cacheRes, EnergyFlowResultVO.class);
        }

        // 获取结果
        EnergyFlowResultVO resultVO = dealEnergyFlowAnalysis1(paramVO);

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        // 返回查询结果。
        return resultVO;

    }

    /**
     * 方式一
     * 能源流动 根据关联关系获取
     *
     * @param paramVO
     * @return
     */
    private EnergyFlowResultVO dealEnergyFlowAnalysis(StatisticsParamV2VO paramVO) {
        // TODO: 2025/5/25  能流图处理
        //  需要注意的是：所有能源和 标签需要在传入的范围内，如果没传入，则去取默认的数据。
        //  1.获取该能源直接关联的所有计量器具，，然后逐个寻找该器具的下级计量，根据下级计量得出标签和能源数据以及该计量器具的折标煤数据 然后递归依次获取对应数据
        //  1.0 能源需要判断是外购还是园区 园区需要向前找， 外购则是向后找， 如果外购能源关联很多园区能源，而园区能源又和多个外购能源管理 即 m对n关系，
        //      这此时需要求对应计量器具的交集，则就是该外购能源流向该园区能源的总量
        //  1.1，根据传入的能源进行获取对应的所有台账id 然后根据台账id去拿对应的用量数据。还需要计算能源流失  例如 电力10kv总量100  分给高温水50  分给220v 48，则能流流失2。
        //       即：电力10kv->未知（损耗）值2
        //  2.根据能源id获取计量器具id，计算该能源该时间段的能源数据->根据计量器具找下级计量并获取标签和能源id获取对应的能源数据，然后依次推下去，只要有下级台账id就递归执行下，
        //  PS：由于需要多次查数据库，建议搞一个内存的数据库，实施同步到内存，则增删改查操作内存 会更快 并减少开销。

        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());

        // 能源类型
        Integer energyClassify = paramVO.getEnergyClassify();
        // 构建返回结果数据
        EnergyFlowResultVO resultVO = new EnergyFlowResultVO();
        resultVO.setDataTime(LocalDateTime.now());

        // 存放所有点
        List<EnergyItemData> data = new ArrayList<>();
        // 存放所有线
        List<EnergyLinkData> links = new ArrayList<>();

        // 2.1.能源id处理
        List<EnergyConfigurationDO> energyList = energyConfigurationService
                .getByEnergyClassify(
                        CollectionUtil.isNotEmpty(paramVO.getEnergyIds()) ? new HashSet<>(paramVO.getEnergyIds()) : new HashSet<>(),
                        energyClassify);
        // 2.2.没有能源报错
        if (CollectionUtil.isEmpty(energyList)) {
            throw exception(ENERGY_CONFIGURATION_NOT_EXISTS);
        }
        // 2.3.能源转换成ids
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 2.4.能源list转换成map
        Map<Long, EnergyConfigurationDO> energyMap = energyList
                .stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        // 2.5.获取所有标签
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        // 3.1.台账id处理
        // 3.1.1.根据能源id查询台账
        List<StandingbookDO> standingbooksByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        // 3.1.2.台账变成ids
        List<Long> standingBookIds = standingbooksByEnergy
                .stream()
                .map(StandingbookDO::getId)
                .collect(Collectors.toList());

        // 3.2.根据台账和其他条件从数据库里拿出折标煤数据
        // 3.2.1.根据台账ID查询用量和折标煤
        List<UsageCostData> energyStandardCoalList = usageCostService.getEnergyStandardCoal(
                rangeOrigin[0],
                rangeOrigin[1],
                standingBookIds);

        // 针对能源Map获取园区能源
        energyStandardCoalList.forEach(usageData -> {
            // 获取能源数据
            EnergyConfigurationDO energy = energyMap.get(usageData.getEnergyId());

            if (!Objects.isNull(energy)) {
                // 存入点
                data.add(new EnergyItemData()
                        .setName(energy.getEnergyName()));
            }
        });


        // TODO: 2025/5/27  判断外购 还是园区
        // 1：外购能源；2：园区能源
        if (1 == energyClassify) {
            // 1：外购能源；
            // 循环外购能源 开始逐一遍历 获取下级园区能源参数
            for (EnergyConfigurationDO energy : energyList) {
                List<StandingbookDO> standingbooks = statisticsCommonService.getStandingbookIdsByEnergy(
                        Collections.singletonList(energy.getId()));

                String source = energy.getEnergyName();

                if (CollectionUtil.isNotEmpty(standingbooks)) {

                    // 下级计量器具
                    Map<Long, List<MeasurementAssociationDO>> subSbs = standingbookService.getSubStandingbookIdsBySbIds(standingBookIds);

                    // 计算下级计量器具数据
                    if (CollUtil.isNotEmpty(subSbs)) {
                        // 分组 台账id-下级计量器具们
                        subSbs.forEach((sbId, association) -> {

                            List<Long> sbIds = association.stream()
                                    .map(MeasurementAssociationDO::getMeasurementId)
                                    .collect(Collectors.toList());

                            List<UsageCostData> energyAndSbList = usageCostService.getEnergyAndSbStandardCoal(
                                    rangeOrigin[0],
                                    rangeOrigin[1],
                                    sbIds);

                            for (UsageCostData usageData : energyAndSbList) {

                                String target = energyMap.get(usageData.getEnergyId()).getEnergyName();

                                // 存入点
                                data.add(new EnergyItemData()
                                        .setName(target));

                                // 存入link（source和target的数据不能一样）
                                if (!source.equals(target)) {
                                    links.add(new EnergyLinkData()
                                            .setSource(source)
                                            .setValue(usageData.getTotalStandardCoalEquivalent())
                                            .setTarget(target));
                                }


                                // 下级计量器具处理
                                dealSbLabel(data, links, sbIds, rangeOrigin, target, labelMap);
                            }

                        });

                    }

                }
            }

        } else if (2 == energyClassify) {
            // 2：园区能源
            // 循环园区能源 开始逐一遍历 获取上级外购能源参数
            for (EnergyConfigurationDO energy : energyList) {
                List<StandingbookDO> standingbooks = statisticsCommonService.getStandingbookIdsByEnergy(
                        Collections.singletonList(energy.getId()));

                String target = energy.getEnergyName();

                if (CollectionUtil.isNotEmpty(standingbooks)) {

                    // 上级计量器具
                    Map<Long, List<MeasurementAssociationDO>> subSbs = standingbookService.getUpStandingbookIdsBySbIds(standingBookIds);

                    // 计算上级计量器具数据
                    if (CollUtil.isNotEmpty(subSbs)) {
                        // 分组 台账id-上级计量器具们
                        subSbs.forEach((sbId, association) -> {

                            List<Long> sbIds = association.stream()
                                    .map(MeasurementAssociationDO::getMeasurementInstrumentId)
                                    .collect(Collectors.toList());

                            List<UsageCostData> energyAndSbList = usageCostService.getEnergyAndSbStandardCoal(
                                    rangeOrigin[0],
                                    rangeOrigin[1],
                                    sbIds);

                            for (UsageCostData usageData : energyAndSbList) {

                                String source = energyMap.get(usageData.getEnergyId()).getEnergyName();

                                // 存入点
                                data.add(new EnergyItemData()
                                        .setName(target));

                                // 存入link
                                if (!source.equals(target)) {
                                    links.add(new EnergyLinkData()
                                            .setSource(source)
                                            .setValue(usageData.getTotalStandardCoalEquivalent())
                                            .setTarget(target));
                                }

                                // 下级计量器具处理  standingBookIds现在是园区计量数据
                                dealSbLabel(data, links, standingBookIds, rangeOrigin, target, labelMap);
                            }
                        });
                    }
                }
            }

        } else {

        }

        // 获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTime(
                paramVO,
                rangeOrigin[0],
                rangeOrigin[1],
                standingBookIds);

        resultVO.setDataTime(lastTime);
        resultVO.setData(data);
        resultVO.setLinks(links);

        return resultVO;
    }


    /**
     * 方式二  后面的下级台账要考label来限制要在label的台账ids范围内，而不是 能源+label，因为后面几栏只跟标签有关饿了，前面2栏目跟能源有关
     * 能源流动 根据台账标签获取
     *
     * @param paramVO
     * @return
     */
    private EnergyFlowResultVO dealEnergyFlowAnalysis1(StatisticsParamV2VO paramVO) {
        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());

        // 能源类型
        Integer energyClassify = paramVO.getEnergyClassify();
        // 构建返回结果数据
        EnergyFlowResultVO resultVO = new EnergyFlowResultVO();
        resultVO.setDataTime(LocalDateTime.now());

        // 存放所有点
        List<EnergyItemData> data = new ArrayList<>();
        // 存放所有线
        List<EnergyLinkData> links = new ArrayList<>();

        // 2.1.能源id处理
        List<EnergyConfigurationDO> energyList = energyConfigurationService
                .getByEnergyClassify(
                        CollectionUtil.isNotEmpty(paramVO.getEnergyIds()) ? new HashSet<>(paramVO.getEnergyIds()) : new HashSet<>(),
                        energyClassify);
        // 2.2.没有能源报错
        if (CollectionUtil.isEmpty(energyList)) {
            throw exception(ENERGY_CONFIGURATION_NOT_EXISTS);
        }

        // 有序set
        LinkedHashSet<String> dataSet = new LinkedHashSet<>();
        // 构建data数据
        energyList.forEach(e -> {
            dataSet.add(e.getEnergyName());
        });


        // 2.3.能源转换成ids
        List<Long> energyIds = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 2.4.全能源list转换成map
        List<EnergyConfigurationDO> energyConfigurationList = energyConfigurationService.getEnergyConfigurationList(new EnergyConfigurationPageReqVO());
        Map<Long, EnergyConfigurationDO> energyMap = energyConfigurationList
                .stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getId, Function.identity()));

        // 2.5.获取所有标签
        Map<Long, LabelConfigDO> labelMap = labelConfigService.getAllLabelConfig()
                .stream()
                .collect(Collectors.toMap(LabelConfigDO::getId, Function.identity()));

        // 3.1.台账id处理
        // 3.1.1.根据能源id查询台账
        List<StandingbookDO> standingbooksByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);
        // 3.1.2.台账变成ids
        List<Long> standingBookIdList = standingbooksByEnergy
                .stream()
                .map(StandingbookDO::getId)
                .collect(Collectors.toList());
        List<Long> standingBookIds = new ArrayList<>();

        // 根据标签查询对应标签关联信息，
        // 1.只有一级Label  则会拿去改一级表现下所有的标签
        // 2.一级和下级Label都没有， 则会拿能源关联的台账id所关联的标签信息
        // 3.一级和下级标签都有的话，则拿对应选中的下级标签信息
        List<StandingbookLabelInfoDO> standingbookIdsByLabel = statisticsCommonService.getStandingbookIdsByLabel(paramVO.getTopLabel(), paramVO.getChildLabels(), standingBookIdList);

        if (CollectionUtil.isNotEmpty(standingbookIdsByLabel)) {
            List<Long> labelSbIds = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
            List<StandingbookDO> collect = standingbooksByEnergy.stream().filter(s -> labelSbIds.contains(s.getId())).collect(Collectors.toList());
            //能源管理计量器具，标签可能关联重点设备，当不存在交集时，则无需查询
            if (ArrayUtil.isEmpty(collect)) {
                // 获得datas
                resultVO.setData(getDataList(dataSet));
                return resultVO;
            }
            List<Long> collect1 = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
            standingBookIds.addAll(collect1);
        } else {
            standingBookIds.addAll(standingBookIdList);
        }
        if (CollectionUtil.isEmpty(standingBookIds)) {
            // 获得datas
            resultVO.setData(getDataList(dataSet));
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }


        // TODO: 2025/5/27  判断外购 还是园区
        // 1：外购能源；2：园区能源
        if (1 == energyClassify) {
            // 1：外购能源；
            // 循环外购能源 开始逐一遍历 获取下级园区能源参数
            for (EnergyConfigurationDO energy : energyList) {
                List<StandingbookDO> standingbooks = statisticsCommonService.getStandingbookIdsByEnergy(
                        Collections.singletonList(energy.getId()));

                // standingbooks 要在大前提下
                List<StandingbookDO> collect = standingbooks.stream().filter(s -> standingBookIds.contains(s.getId())).collect(Collectors.toList());


                String source = energy.getEnergyName();

                if (CollectionUtil.isNotEmpty(collect)) {

                    List<Long> energySbIds = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());
                    // 下级计量器具
                    Map<Long, List<MeasurementAssociationDO>> subSbs = standingbookService.getSubStandingbookIdsBySbIds(energySbIds);

                    // 计算下级计量器具数据
                    if (CollUtil.isNotEmpty(subSbs)) {
                        // 分组 台账id-下级计量器具们
                        subSbs.forEach((sbId, association) -> {

                            List<Long> sbIds = association.stream()
                                    .map(MeasurementAssociationDO::getMeasurementId)
                                    .collect(Collectors.toList());

                            if (CollectionUtil.isNotEmpty(sbIds)) {
                                List<UsageCostData> energyAndSbList = usageCostService.getEnergyAndSbStandardCoal(
                                        rangeOrigin[0],
                                        rangeOrigin[1],
                                        sbIds);

                                for (UsageCostData usageData : energyAndSbList) {

                                    String target = energyMap.get(usageData.getEnergyId()).getEnergyName();

                                    // 存入点
                                    dataSet.add(target);

                                    // 存入link
                                    if (!source.equals(target)) {
                                        links.add(new EnergyLinkData()
                                                .setSource(source)
                                                .setValue(usageData.getTotalStandardCoalEquivalent())
                                                .setTarget(target));
                                    }

                                    List<Long> labelSbIds = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
                                    // 下级计量器具处理
                                    dealSbLabel1(dataSet, links, sbIds, rangeOrigin, target, labelMap, labelSbIds);
                                }
                            }
                        });

                    }

                }
            }

        } else if (2 == energyClassify) {
            // 2：园区能源
            // 循环园区能源 开始逐一遍历 获取上级外购能源参数
            for (EnergyConfigurationDO energy : energyList) {
                List<StandingbookDO> standingbooks = statisticsCommonService.getStandingbookIdsByEnergy(
                        Collections.singletonList(energy.getId()));

                // standingbooks 要在大前提下
                List<StandingbookDO> collect = standingbooks.stream().filter(s -> standingBookIds.contains(s.getId()))
                        .collect(Collectors.toList());


                String target = energy.getEnergyName();

                if (CollectionUtil.isNotEmpty(collect)) {

                    List<Long> energySbIds = collect.stream().map(StandingbookDO::getId).collect(Collectors.toList());

                    // 上级计量器具
                    Map<Long, List<MeasurementAssociationDO>> subSbs = standingbookService.getUpStandingbookIdsBySbIds(energySbIds);

                    // 计算上级计量器具数据
                    if (CollUtil.isNotEmpty(subSbs)) {
                        // 分组 台账id-上级计量器具们
                        subSbs.forEach((sbId, association) -> {

                            List<Long> sbIds = association.stream()
                                    .map(MeasurementAssociationDO::getMeasurementInstrumentId)
                                    .collect(Collectors.toList());

                            List<Long> sbIdsCollect = sbIds.stream().filter(standingBookIds::contains).collect(Collectors.toList());

                            List<UsageCostData> energyAndSbList = usageCostService.getEnergyAndSbStandardCoal(
                                    rangeOrigin[0],
                                    rangeOrigin[1],
                                    sbIdsCollect);

                            for (UsageCostData usageData : energyAndSbList) {

                                String source = energyMap.get(usageData.getEnergyId()).getEnergyName();

                                // 存入点
                                dataSet.add(target);

                                // 存入link
                                if (!source.equals(target)) {
                                    links.add(new EnergyLinkData()
                                            .setSource(source)
                                            .setValue(usageData.getTotalStandardCoalEquivalent())
                                            .setTarget(target));
                                }
                                List<Long> labelSbIds = standingbookIdsByLabel.stream().map(StandingbookLabelInfoDO::getStandingbookId).collect(Collectors.toList());
                                // 下级计量器具处理  standingBookIds现在是园区计量数据
                                dealSbLabel1(dataSet, links, standingBookIds, rangeOrigin, target, labelMap, labelSbIds);
                            }
                        });
                    }
                }
            }

        } else {

        }

        // 获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTime(
                paramVO,
                rangeOrigin[0],
                rangeOrigin[1],
                standingBookIds);

        // dataSet 转换成data
        dataSet.forEach(d -> {
            data.add(new EnergyItemData().setName(d));
        });

        resultVO.setData(data);
        resultVO.setLinks(links);
        resultVO.setDataTime(lastTime);

        return resultVO;
    }

    private List<EnergyItemData> getDataList(LinkedHashSet<String> dataSet) {

        return dataSet.stream().map(s ->
                new EnergyItemData().setName(s)
        ).collect(Collectors.toList());
    }


    /**
     * 直接填充label的 data 和link
     *
     * @param data
     * @param links
     * @param sbIds
     * @param rangeOrigin
     * @param source
     * @param labelMap
     */
    private void dealSbLabel1(LinkedHashSet<String> data,
                              List<EnergyLinkData> links,
                              List<Long> sbIds,
                              LocalDateTime[] rangeOrigin,
                              String source,
                              Map<Long, LabelConfigDO> labelMap,
                              List<Long> standingBookIds) {
        // 下级计量
        Map<Long, List<MeasurementAssociationDO>> sbs = standingbookService.getSubStandingbookIdsBySbIds(sbIds);

        // 根据sbId获取对应的标签
        // 计算下级计量器具数据
        if (CollUtil.isNotEmpty(sbs)) {
            // 分组 台账id-下级计量器具们
            sbs.forEach((sbId, association) -> {

                List<Long> subsbIds = association.stream()
                        .map(MeasurementAssociationDO::getMeasurementId)
                        .collect(Collectors.toList());


                List<Long> sbIdsCollect = subsbIds.stream().filter(standingBookIds::contains).collect(Collectors.toList());

                if (CollectionUtil.isNotEmpty(sbIdsCollect)) {
                    List<UsageCostData> standingbookList = usageCostService.getStandingbookStandardCoal(
                            rangeOrigin[0],
                            rangeOrigin[1],
                            sbIdsCollect);
                    for (UsageCostData usageData : standingbookList) {

                        // 获取标签  根据台账获取标签
                        // 查询标签信息
                        List<StandingbookLabelInfoDO> standingbookLabelInfoDOList =
                                standingbookLabelInfoService.getByStandingBookId(usageData.getStandingbookId());

                        if (CollUtil.isNotEmpty(standingbookLabelInfoDOList)) {
                            for (StandingbookLabelInfoDO labelInfo : standingbookLabelInfoDOList) {


                                // 处理一级标签
                                String topLabelKey = labelInfo.getName();
                                Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
                                String target = labelMap.get(topLabelId).getLabelName();
                                // 存入点
                                data.add(target);
                                // 存入link
                                if (!source.equals(target)) {
                                    links.add(new EnergyLinkData()
                                            .setSource(source)
                                            .setValue(usageData.getTotalStandardCoalEquivalent())
                                            .setTarget(target));
                                }

                                // 处理n级标签问题
                                String value = labelInfo.getValue();
                                if (StrUtil.isNotEmpty(value)) {
                                    String[] labelIds = value.split(",");
                                    String labelName = "";
                                    for (int i = 0; i < labelIds.length; i++) {
                                        LabelConfigDO label = labelMap.get(Long.valueOf(labelIds[i]));
                                        if (i == 0) {
                                            labelName = label.getLabelName();
                                            data.add(labelName);
                                            // 存入link
                                            if (!source.equals(target)) {
                                                links.add(new EnergyLinkData()
                                                        .setSource(target)
                                                        .setValue(usageData.getTotalStandardCoalEquivalent())
                                                        .setTarget(labelName));
                                            }

                                        } else {

                                            String t = label.getLabelName();
                                            // 存入点
                                            data.add(t);
                                            // 存入link
                                            if (!source.equals(target)) {
                                                links.add(new EnergyLinkData()
                                                        .setSource(labelName)
                                                        .setValue(usageData.getTotalStandardCoalEquivalent())
                                                        .setTarget(t));
                                            }

                                            labelName = t;
                                        }
                                    }

                                }

                            }
                        }
                    }
                }
            });
        }

    }


    private void dealSbLabel(List<EnergyItemData> data,
                             List<EnergyLinkData> links,
                             List<Long> sbIds,
                             LocalDateTime[] rangeOrigin,
                             String source,
                             Map<Long, LabelConfigDO> labelMap) {
        // TODO: 2025/5/26 根据sbIds 再去获取下级计量器具 然后递归 获取数据 此时就需要取标签了。而不是能源的了
        Map<Long, List<MeasurementAssociationDO>> sbs = standingbookService.getSubStandingbookIdsBySbIds(sbIds);

        // 根据sbId获取对应的标签
        // 计算下级计量器具数据
        if (CollUtil.isNotEmpty(sbs)) {
            // 分组 台账id-下级计量器具们
            sbs.forEach((sbId, association) -> {

                List<Long> subsbIds = association.stream()
                        .map(MeasurementAssociationDO::getMeasurementId)
                        .collect(Collectors.toList());

                List<UsageCostData> standingbookList = usageCostService.getStandingbookStandardCoal(
                        rangeOrigin[0],
                        rangeOrigin[1],
                        subsbIds);

                for (UsageCostData usageData : standingbookList) {

                    // 获取标签  根据台账获取标签
                    // 查询标签信息
                    List<StandingbookLabelInfoDO> standingbookLabelInfoDOList =
                            standingbookLabelInfoService.getByStandingBookId(usageData.getStandingbookId());

                    if (CollUtil.isNotEmpty(standingbookLabelInfoDOList)) {
                        for (StandingbookLabelInfoDO labelInfo : standingbookLabelInfoDOList) {
                            String target = dealLabelTarget(labelInfo, labelMap);

                            // 存入点
                            data.add(new EnergyItemData()
                                    .setName(target));

                            // 存入link
                            if (!source.equals(target)) {
                                links.add(new EnergyLinkData()
                                        .setSource(source)
                                        .setValue(usageData.getTotalStandardCoalEquivalent())
                                        .setTarget(target));
                            }

                            // 下级计量器具处理
                            dealSbLabel(data, links, subsbIds, rangeOrigin, target, labelMap);
                        }
                    }
                }
            });
        }

    }


    private String dealLabelTarget(StandingbookLabelInfoDO labelInfo, Map<Long, LabelConfigDO> labelMap) {

        String value = labelInfo.getValue();
        if (StrUtil.isNotEmpty(value)) {
            String[] labelIds = value.split(",");
            LabelConfigDO label = labelMap.get(Long.valueOf(labelIds[labelIds.length - 1]));
            return label.getLabelName();
        } else {
            String topLabelKey = labelInfo.getName();
            Long topLabelId = Long.valueOf(topLabelKey.substring(topLabelKey.indexOf("_") + 1));
            return labelMap.get(topLabelId).getLabelName();
        }

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

    @Override
    public Map<String, Object> energyFlowAnalysis(StatisticsParamVO paramVO) {


        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        Map<String, Object> result = new HashMap<>(2);

        List<Map<String, String>> data = new ArrayList<>();

        List<Map<String, Object>> links = new ArrayList<>();

        // 能源数据
        List<EnergyConfigurationDO> energyList = dealEnergyQueryDataForEnergyFlow(paramVO);
        energyList.forEach(e -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", e.getEnergyName());
            data.add(map);
        });


        List<EnergyConfigurationDO> energyList1 = new ArrayList<>();
        List<EnergyConfigurationDO> energyList2 = new ArrayList<>();
        ;

        Integer energyClassify = energyList.get(0).getEnergyClassify();
        if (1 == energyClassify) {
            // 外购
            energyList1 = energyList;
            // 园区
            energyList2 = energyConfigurationService.getByEnergyClassify(2);

        } else {
            // 外购
            energyList1 = energyConfigurationService.getByEnergyClassify(1);
            // 园区
            energyList2 = energyList;
        }

        // 标签数据
        ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> labelPair = dealLabelQueryDataForEnergyFlow(paramVO);

        List<LabelConfigDO> list = labelPair.getLeft();
        list.forEach(l -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", l.getLabelName());
            data.add(map);
        });

        List<EnergyConfigurationDO> finalEnergyList = energyList2;
        energyList1.forEach(e -> {

            finalEnergyList.forEach(e2 -> {
                Map<String, Object> map = new HashMap<>();
                map.put("source", e.getEnergyName());
                map.put("target", e2.getEnergyName());
                map.put("value", RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
                links.add(map);
            });
        });

        List<Tree<Long>> labelTree = labelPair.getRight();

        energyList2.forEach(e -> {
            for (Tree<Long> tree : labelTree) {
                Map<String, Object> map = new HashMap<>();
                map.put("source", e.getEnergyName());
                map.put("target", tree.getName());
                map.put("value", RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
                links.add(map);
            }
        });


        for (Tree<Long> tree : labelTree) {
            List<Map<String, Object>> mapList = getMapList(energyList, tree, range);
            links.addAll(mapList);
        }

        result.put("data", data);
        result.put("links", links);
        return result;
    }

    private List<Map<String, Object>> getMapList(List<EnergyConfigurationDO> energyList, Tree<Long> labelTree, LocalDate[] range) {

        List<Map<String, Object>> links = new ArrayList<>();


        List<Tree<Long>> labelTreeList = labelTree.getChildren();
        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {
            Map<String, Object> map = new HashMap<>();

            List<CharSequence> parentsNames = labelTree.getParentsName(true);
            map.put("source", parentsNames.get(parentsNames.size() - 1));
            map.put("target", labelTree.getName());
            map.put("value", RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
            links.add(map);
            return links;
        }

        // TODO: 2024/12/18 能流数据处理  关系 以及 标签，处理

        // 能源关系处理
//        energyList.forEach(e -> {
//
//            if (e.getEnergyClassify() == 1){
//                // 外购能源
//                DeviceAssociationConfigurationPageReqVO deviceAssociationVo = new DeviceAssociationConfigurationPageReqVO();
//                deviceAssociationVo.setEnergyId(e.getId());
//                List<DeviceAssociationConfigurationDO> list = deviceAssociationConfigurationService.getDeviceAssociationConfigurationList(deviceAssociationVo);
//                list.forEach(l->{
//                    String preMeasurement = l.getPreMeasurement();
//                });
//            }
//        });

        for (Tree<Long> longTree : labelTreeList) {
            List<Map<String, Object>> mapList = getMapList(energyList, longTree, range);
            links.addAll(mapList);
        }

        return links;
    }


    @Override
    public Map<String, Object> standardCoalAnalysisTable(StatisticsParamVO paramVO) {

        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        Integer dateType = paramVO.getDateType();
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        Integer queryType = paramVO.getQueryType();
        if (queryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }
        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = getTableHeader(rangeOrigin, dateType);

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);

            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            // TODO: 2024/12/11 多线程处理 labelTree for循环
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        result.put("dataTime", LocalDateTime.now());
        return result;
    }

    @Override
    public Object standardCoalAnalysisChart(StatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }
        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        Integer dateType = paramVO.getDateType();
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        Integer queryType = paramVO.getQueryType();
        if (queryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

            return getStackVO(rangeOrigin, dateType, list, queryType);

        } else if (2 == queryType) {
            // 2、按标签查看
            //X轴数据
            List<String> XData = getTableHeader(rangeOrigin, dateType);
            //Y轴数据list
            List<StackDataVO> YData = new ArrayList<>();

            // 标签查询条件处理 只需要第一级别就可以
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);

                List<BigDecimal> collect = getYData(XData, statisticsResultVOList);
                YData.add(StackDataVO.builder()
                        .id(tree.getId())
                        .name(tree.getName().toString())
                        .data(collect).build());

            }
            return StatisticsStackVO.builder()
                    .XData(XData)
                    .YData(YData)
                    .dataTime(LocalDateTime.now()).build();

        } else {
            // 0、综合查看（默认）
            return getOverallViewBar(paramVO);
        }

    }

    @Override
    public StatisticsBarVO getOverallViewBar(StatisticsParamVO paramVO) {
        // 0、综合查看（默认）
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        Integer dateType = paramVO.getDateType();
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();
        // 标签查询条件处理
        List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
        if (CollectionUtil.isEmpty(labelTree)) {
            throw exception(LABEL_CONFIG_NOT_EXISTS);
        }
        // 能源查询条件处理
        List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

        //X轴数据
        List<String> XData = getTableHeader(rangeOrigin, dateType);

        // 统计结果list
        for (Tree<Long> tree : labelTree) {
            List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, 0);
            list.addAll(statisticsResultVOList);
        }

        //Y轴数据
        List<BigDecimal> YData = getYData(XData, list);

        return StatisticsBarVO.builder()
                .XData(XData)
                .YData(YData)
                .dataTime(LocalDateTime.now()).build();

    }

    private List<BigDecimal> getYData(List<String> XData, List<StatisticsResultVO> statisticsResultVOList) {
        // 初始化一个Map来存储每个时间点的总金额
        Map<String, BigDecimal> totalMoneyByDate = new HashMap<>();
        for (String date : XData) {
            totalMoneyByDate.put(date, BigDecimal.ZERO);
        }
        // 填充Y轴数据
        for (StatisticsResultVO vo : statisticsResultVOList) {
            for (StatisticsDateData dateData : vo.getStatisticsDateDataList()) {
                String date = dateData.getDate();
                BigDecimal money = dateData.getMoney();
                totalMoneyByDate.merge(date, money, BigDecimal::add);
            }
        }
        // 生成YData列表
        return XData.stream().map(totalMoneyByDate::get).collect(Collectors.toList());

    }

    /**
     * 堆叠图
     *
     * @param rangeOrigin
     * @param dateType
     * @param statisticsResultVOList
     * @return
     */
    private StatisticsStackVO getStackVO(LocalDateTime[] rangeOrigin, Integer dateType, List<StatisticsResultVO> statisticsResultVOList, Integer queryType) {

        //X轴数据
        List<String> XData = getTableHeader(rangeOrigin, dateType);

        //Y轴数据list
        List<StackDataVO> YData = new ArrayList<>();

        for (StatisticsResultVO statisticsResultVO : statisticsResultVOList) {
            String name;
            Long id;
            if (1 == queryType) {
                name = statisticsResultVO.getEnergyName();
                id = statisticsResultVO.getEnergyId();
            } else if (2 == queryType) {
                name = statisticsResultVO.getLabel1();
                id = statisticsResultVO.getLabelId();
            } else {
                name = "";
                id = null;
            }

            List<StatisticsDateData> statisticsDateDataList = statisticsResultVO.getStatisticsDateDataList();
            List<BigDecimal> collect = statisticsDateDataList.stream().map(StatisticsDateData::getMoney).collect(Collectors.toList());
            YData.add(StackDataVO.builder()
                    .id(id)
                    .name(name)
                    .data(collect).build());
        }

        return StatisticsStackVO.builder()
                .XData(XData)
                .YData(YData)
                .dataTime(LocalDateTime.now()).build();
    }

    @Override
    public Map<String, Object> moneyAnalysisTable(StatisticsParamVO paramVO) {

        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        Integer dateType = paramVO.getDateType();
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        Integer queryType = paramVO.getQueryType();
        if (queryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }

        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = getTableHeader(rangeOrigin, dateType);

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);

            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            // TODO: 2024/12/11 多线程处理 labelTree for循环
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        result.put("dataTime", LocalDateTime.now());
        return result;
    }

    @Override
    public Object moneyAnalysisChart(StatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }
        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        Integer dateType = paramVO.getDateType();
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        Integer queryType = paramVO.getQueryType();
        if (queryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

            return getStackVO(rangeOrigin, dateType, list, queryType);

        } else if (2 == queryType) {
            // 2、按标签查看
            //X轴数据
            List<String> XData = getTableHeader(rangeOrigin, dateType);
            //Y轴数据list
            List<StackDataVO> YData = new ArrayList<>();

            // 标签查询条件处理 只需要第一级别就可以
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);

                List<BigDecimal> collect = getYData(XData, statisticsResultVOList);
                YData.add(StackDataVO.builder()
                        .id(tree.getId())
                        .name(tree.getName().toString())
                        .data(collect).build());

            }
            return StatisticsStackVO.builder()
                    .XData(XData)
                    .YData(YData).build();

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            //X轴数据
            List<String> XData = getTableHeader(rangeOrigin, dateType);

            // 统计结果list
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }

            //Y轴数据
            List<BigDecimal> YData = getYData(XData, list);

            return StatisticsBarVO.builder()
                    .XData(XData)
                    .YData(YData).build();
        }

    }

    private List<StatisticsResultVO> getStatisticsData(StatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }
        List<StatisticsResultVO> list = new ArrayList<>();
        List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsResultVO vo = new StatisticsResultVO();
            vo.setEnergyId(energy.getId());
            StatisticsResultVO dateData = getDateData(vo, range, paramVO.getDateType(), paramVO.getQueryType());
            list.add(dateData);
        }
        return list;
    }

    /**
     * 格式 时间list ['2024/5/5','2024/5/5','2024/5/5'];
     *
     * @param rangeOrigin 时间范围
     * @return ['2024/5/5','2024/5/5','2024/5/5']
     */
    private List<String> getTableHeader(LocalDateTime[] rangeOrigin, Integer dateType) {

        List<String> headerList = new ArrayList<>();

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        if (1 == dateType) {
            // 月
            LocalDate tempStartDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
            LocalDate tempEndDate = LocalDate.of(endDate.getYear(), endDate.getMonth(), 1);

            while (tempStartDate.isBefore(tempEndDate) || tempStartDate.isEqual(tempEndDate)) {

                int year = tempStartDate.getYear();
                int month = tempStartDate.getMonthValue();
                String monthSuffix = (month < 10 ? "-0" : "-") + month;
                headerList.add(year + monthSuffix);

                tempStartDate = tempStartDate.plusMonths(1);
            }

        } else if (2 == dateType) {
            // 年
            while (startDate.getYear() <= endDate.getYear()) {

                headerList.add(String.valueOf(startDate.getYear()));

                startDate = startDate.plusYears(1);
            }
        } else if (3 == dateType) {
            // 时
            LocalDateTime startDateTime = rangeOrigin[0];
            LocalDateTime endDateTime = rangeOrigin[1];

            while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
                String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
                headerList.add(formattedDate);
                startDateTime = startDateTime.plusHours(1);
            }

        } else {
            // 日
            while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

                String formattedDate = LocalDateTimeUtil.formatNormal(startDate);
                headerList.add(formattedDate);

                startDate = startDate.plusDays(1);
            }
        }

        return headerList;
    }

    /**
     * 标签查询条件处理 能流图
     *
     * @param paramVO
     * @return
     */
    private ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> dealLabelQueryDataForEnergyFlow(StatisticsParamVO paramVO) {
        ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> labelPair;
        List<Long> labelIds = paramVO.getLabelIds();
        if (CollectionUtil.isNotEmpty(labelIds)) {
            labelPair = labelConfigService.getLabelPairByParam(labelIds);
        } else {
            labelPair = labelConfigService.getLabelPair(false, null, null);
        }
        return labelPair;
    }

    /**
     * 标签查询条件处理
     *
     * @param paramVO
     * @return
     */
    private List<Tree<Long>> dealLabelQueryData(StatisticsParamVO paramVO) {
        List<Tree<Long>> labelTree;
        List<Long> labelIds = paramVO.getLabelIds();
        if (CollectionUtil.isNotEmpty(labelIds)) {
            labelTree = labelConfigService.getLabelTreeByParam(labelIds);
        } else {
            labelTree = labelConfigService.getLabelTree(false, null, null);
        }
        return labelTree;
    }

    /**
     * 能源查询条件处理
     *
     * @param paramVO
     * @return
     */
    private List<EnergyConfigurationDO> dealEnergyQueryDataForEnergyFlow(StatisticsParamVO paramVO) {
        // 能源查询条件处理
        EnergyConfigurationPageReqVO queryVO = new EnergyConfigurationPageReqVO();

        List<Long> energyIds = paramVO.getEnergyIds();
        if (CollectionUtil.isNotEmpty(energyIds)) {
            queryVO.setEnergyIds(energyIds);
        }
        // 能源list
        return energyConfigurationService.getEnergyConfigurationList(queryVO);
    }

    /**
     * 能源查询条件处理
     *
     * @param paramVO
     * @return
     */
    private List<EnergyConfigurationDO> dealEnergyQueryData(StatisticsParamVO paramVO) {
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


    private List<StatisticsResultVO> getStatisticsResultVOHaveEnergy(Tree<Long> labelTree, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsResultVO statisticsResultVO = new StatisticsResultVO();
            statisticsResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsResultVO.setLabel1(parentsNameList.get(0));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsResultVO.setLabel1(parentsNameList.get(1));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsResultVO.setLabel1(parentsNameList.get(2));
                    statisticsResultVO.setLabel2(parentsNameList.get(1));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(statisticsResultVO, energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsResultVO> statisticsResultList = getStatisticsResultVOHaveEnergy(longTree, energyList, range, dateType, queryType);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsResultVO> getStatisticsResultVONotHaveEnergy(Tree<Long> labelTree, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsResultVO statisticsResultVO = new StatisticsResultVO();
            statisticsResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsResultVO.setLabel1(parentsNameList.get(0));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsResultVO.setLabel1(parentsNameList.get(1));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsResultVO.setLabel1(parentsNameList.get(2));
                    statisticsResultVO.setLabel2(parentsNameList.get(1));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsResultVO> statisticsResultVOList = getLabelList(statisticsResultVO, range, dateType, queryType);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsResultVO> statisticsResultList = getStatisticsResultVONotHaveEnergy(longTree, range, dateType, queryType);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsResultVO> getEnergyList(StatisticsResultVO statisticsResultVO, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();
        BigDecimal sumLabelConsumption = BigDecimal.ZERO;
        BigDecimal sumLabelMoney = BigDecimal.ZERO;
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsResultVO vo = BeanUtils.toBean(statisticsResultVO, StatisticsResultVO.class);
            vo.setEnergyName(energy.getEnergyName());
            vo.setEnergyId(energy.getId());

            // 获取对应标签下对应能源的每日用能数据和折价数据
            StatisticsResultVO dateData = getDateData(vo, range, dateType, queryType);
            list.add(dateData);
        }

        // 获取合计
        for (StatisticsResultVO resultVO : list) {
            sumLabelConsumption = sumLabelConsumption.add(resultVO.getSumEnergyConsumption());
            sumLabelMoney = sumLabelMoney.add(resultVO.getSumEnergyMoney());
        }

        // 赋值合计
        for (StatisticsResultVO resultVO : list) {
            resultVO.setSumLabelConsumption(sumLabelConsumption);
            resultVO.setSumLabelMoney(sumLabelMoney);
        }
        return list;
    }

    private List<StatisticsResultVO> getLabelList(StatisticsResultVO statisticsResultVO, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();
        StatisticsResultVO vo = BeanUtils.toBean(statisticsResultVO, StatisticsResultVO.class);

        // 获取对应标签下对应能源的每日用能数据和折价数据
        StatisticsResultVO dateData = getDateData(vo, range, dateType, queryType);

        dateData.setSumLabelConsumption(dateData.getSumEnergyConsumption());
        dateData.setSumLabelMoney(dateData.getSumEnergyMoney());
        list.add(dateData);
        return list;
    }

    /**
     * @param range 时间范围
     * @return
     */
    private StatisticsResultVO getDateData(StatisticsResultVO vo, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsDateData> statisticsDateDataList = new ArrayList<>();

        //时间预处理
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        // 标签、能源id预处理
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();


        BigDecimal sumEnergyConsumption = BigDecimal.ZERO;
        BigDecimal sumEnergyMoney = BigDecimal.ZERO;

        if (1 == dateType) {
            // 月
            LocalDate tempStartDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
            LocalDate tempEndDate = LocalDate.of(endDate.getYear(), endDate.getMonth(), 1);

            while (tempStartDate.isBefore(tempEndDate) || tempStartDate.isEqual(tempEndDate)) {

                int year = tempStartDate.getYear();
                int month = tempStartDate.getMonthValue();

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsDateData statisticsDateData = getMonthData(labelId, energyId, range, year, month, queryType);

                String monthSuffix = (month < 10 ? "-0" : "-") + month;
                statisticsDateData.setDate(year + monthSuffix);
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                tempStartDate = tempStartDate.plusMonths(1);
            }

        } else if (2 == dateType) {
            // 年
            while (startDate.getYear() <= endDate.getYear()) {

                int year = startDate.getYear();
                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsDateData statisticsDateData = getYearData(labelId, energyId, range, year, queryType);
                statisticsDateData.setDate(String.valueOf(year));
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                startDate = startDate.plusYears(1);
            }

        } else if (3 == dateType) {
            // 时

            LocalDateTime startDateTime = range[0].atStartOfDay();
            LocalDateTime endDateTime = range[1].atStartOfDay();

            while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
                String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
                StatisticsDateData statisticsDateData = getHourData(labelId, energyId, startDateTime, queryType);
                statisticsDateData.setDate(formattedDate);
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }
                startDateTime = startDateTime.plusHours(1);
            }

        } else {
            // 日
            while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

                // 时间处理
                String formattedDate = LocalDateTimeUtil.formatNormal(startDate);

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsDateData statisticsDateData = getData(labelId, energyId, startDate, queryType);

                statisticsDateData.setDate(formattedDate);
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                startDate = startDate.plusDays(1);
            }
        }

        vo.setStatisticsDateDataList(statisticsDateDataList);
        // 横向合计：所有日期的一个合计
        vo.setSumEnergyConsumption(sumEnergyConsumption);
        vo.setSumEnergyMoney(sumEnergyMoney);


        return vo;
    }

    private List<StatisticsResultVO> getHourlyStatisticsData(StatisticsParamVO paramVO) {
        List<StatisticsResultVO> list = new ArrayList<>();
        List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsResultVO vo = new StatisticsResultVO();
            vo.setEnergyId(energy.getId());
            StatisticsResultVO dateData = getHourlyDateData(vo, paramVO.getRange(), paramVO.getQueryType());
            list.add(dateData);
        }
        return list;
    }

    private List<String> getHourlyTableHeader(LocalDateTime[] range) {
        List<String> headerList = new ArrayList<>();
        LocalDateTime startDateTime = range[0];
        LocalDateTime endDateTime = range[1];

        while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
            String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
            headerList.add(formattedDate);
            startDateTime = startDateTime.plusHours(1);
        }

        return headerList;
    }

    private StatisticsResultVO getHourlyDateData(StatisticsResultVO vo, LocalDateTime[] range, Integer queryType) {
        List<StatisticsDateData> statisticsDateDataList = new ArrayList<>();
        LocalDateTime startDateTime = range[0];
        LocalDateTime endDateTime = range[1];
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();
        while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
            String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
            StatisticsDateData statisticsDateData = getHourData(labelId, energyId, startDateTime, queryType);
            statisticsDateData.setDate(formattedDate);
            statisticsDateDataList.add(statisticsDateData);
            startDateTime = startDateTime.plusHours(1);
        }
        vo.setStatisticsDateDataList(statisticsDateDataList);
        // 计算总金额
        BigDecimal sumEnergyMoney = statisticsDateDataList.stream()
                .map(StatisticsDateData::getMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setSumEnergyMoney(sumEnergyMoney);
        return vo;
    }

    private StatisticsDateData getHourData(Long labelId, Long energyId, LocalDateTime dateTime, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();
        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            // 如果没有找到数据，你可以设置默认值或空值。
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(BigDecimal.ZERO);
        }
        return statisticsDateData;
    }

    private StatisticsDateData getData(Long labelId, Long energyId, LocalDate date, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）

        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        }
        return statisticsDateData;
    }


    private StatisticsDateData getMonthData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer month, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024 and month = 12

        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        }
        return statisticsDateData;
    }

    private StatisticsDateData getYearData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024

        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        }
        return statisticsDateData;
    }
}