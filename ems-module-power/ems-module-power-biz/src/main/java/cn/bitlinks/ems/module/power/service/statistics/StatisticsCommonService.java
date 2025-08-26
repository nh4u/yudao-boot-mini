package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.EnergyClassifyEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookEnergyTypeVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.measurementassociation.MeasurementAssociationMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.label.StandingbookLabelInfoService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;


/**
 * @author wangl
 * @date 2025年05月09日 10:10
 */
@Service
@Slf4j
@Validated
public class StatisticsCommonService {

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private StandingbookLabelInfoService standingbookLabelInfoService;

    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;

    @Resource
    private MeasurementAssociationMapper measurementAssociationMapper;

    @Resource
    private StandingbookService standingbookService;

    private static final String CHILD_LABEL_REGEX_PREFIX = "^%s";
    private static final String CHILD_LABEL_REGEX_SUFFIX = "$";
    private static final String CHILD_LABEL_REGEX_ADD = ",\\d+";

    //递归最大深度，目前标签支持5层，递归不包含顶层递归，所以最多是4层，设置5层为了扩宽
    private static final Integer MAX_DEPTH = 5;

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    /**
     * 能源id获取台账id
     *
     * @return
     */
    public List<Long> getSbIdsByEnergy(List<Long> energyIds) {
        List<StandingbookDO> standingbookIdsByEnergy = getStandingbookIdsByEnergy(energyIds);
        if (CollUtil.isEmpty(standingbookIdsByEnergy)) {
            return Collections.emptyList();
        }
        return standingbookIdsByEnergy.stream().map(StandingbookDO::getId).collect(Collectors.toList());
    }

    /**
     * 根据筛选条件筛选台账ID
     *
     * @return
     */
    public List<StandingbookDO> getStandingbookIdsByEnergy(List<Long> energyIds) {
        //根据能源类型+id从power_standingbook_tmpl_daq_attr查询分类ID，根据分类ID查询台账ID
        List<StandingbookTmplDaqAttrDO> tmplList = standingbookTmplDaqAttrService.getByEnergyIds(energyIds);
        //分类ID列表
        List<Long> typeIds = tmplList.stream().map(StandingbookTmplDaqAttrDO::getTypeId).collect(Collectors.toList());
        //根据分类ID获取台账
        List<StandingbookDO> byTypeIds = standingbookService.getByTypeIds(typeIds);
        return byTypeIds;
    }

    /**
     * 统一校验时间类型、时间范围
     *
     * @param paramVO
     */
    public void validParamConditionDate(StatisticsParamV2VO paramVO) {
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
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }
    }

    /**
     * 查询终端使用的最底层叶子节点的计量器具
     */
    public List<Long> getStageEnergySbIds(Integer stage, boolean toppest, EnergyClassifyEnum energyClassifyEnum) {
        // 查询终端使用的最底层叶子节点的计量器具
        List<Long> stageSbIds = standingbookService.getStandingBookIdsByStage(stage);
        if (CollUtil.isEmpty(stageSbIds)) {
            return Collections.emptyList();
        }
        List<Long> measurementIds;
        if (toppest) {
            measurementIds = measurementAssociationMapper.getNotToppestMeasurementId(stageSbIds);
        } else {
            measurementIds = measurementAssociationMapper.getNotLeafMeasurementId(stageSbIds);
        }

        Set<Long> measurementSet = new HashSet<>(measurementIds);
        // 先过滤掉null
        stageSbIds = stageSbIds.stream()
                .filter(Objects::nonNull) // 先过滤掉null
                .filter(id -> !measurementSet.contains(id))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(stageSbIds)) {
            return Collections.emptyList();
        }
        if (energyClassifyEnum != null) {
            // 查询外购能源 计量器具ids
            List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(energyClassifyEnum.getCode());
            if (CollUtil.isEmpty(energyList)) {
                return Collections.emptyList();
            }
            List<Long> energySbIds = getSbIdsByEnergy(energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList()));
            if (CollUtil.isEmpty(energySbIds)) {
                return Collections.emptyList();
            }
            // 取外购能源计量器具与环节最底层节点交集
            stageSbIds.retainAll(new HashSet<>(energySbIds));
            return stageSbIds;
        }
        return stageSbIds;
    }

    /**
     * 根据筛选条件筛选台账ID
     *
     * @return
     */
    @Deprecated
    public List<StandingbookLabelInfoDO> getStandingbookIdsByLabelOld(String topLabel, String childLabels, List<Long> standingBookIdList) {
        if (StrUtil.isBlank(topLabel) && StrUtil.isBlank(childLabels) && CollUtil.isEmpty(standingBookIdList)) {
            return Collections.emptyList();
        }
        if (StrUtil.isBlank(topLabel) && StrUtil.isBlank(childLabels)) {
            return standingbookLabelInfoService.getByStandingBookIds(standingBookIdList);
        }

        //根据标签获取台账
        //只选择顶级标签
        //标签也可能关联重点设备，需要去除重点设备ID
        List<StandingbookLabelInfoDO> labelInfoDOList = new ArrayList<>();
        if (StrUtil.isNotBlank(topLabel) && StrUtil.isBlank(childLabels)) {
            List<StandingbookLabelInfoDO> byLabelNames = standingbookLabelInfoService.getByLabelNames(Collections.singletonList(topLabel));
            labelInfoDOList.addAll(byLabelNames);
            // 顶级跟下级都选择
        } else if (StrUtil.isNotBlank(topLabel) && StrUtil.isNotEmpty(childLabels)) {
            List<String> childLabelValues = StrSplitter.split(childLabels, "#", 0, true, true);
            //根据标签获取台账
            List<StandingbookLabelInfoDO> byValues = standingbookLabelInfoService.getByValues(childLabelValues);
            labelInfoDOList.addAll(byValues);
        }
        //台账不一定会有标签
        if (ArrayUtil.isEmpty(labelInfoDOList)) {
            return Collections.emptyList();
        }
        return labelInfoDOList;
    }

    /**
     * 根据指定的子级标签筛选台账ID
     *
     * @return
     */
    public List<StandingbookLabelInfoDO> getStandingbookIdsByDefaultLabel(List<String> childLabels) {
        return standingbookLabelInfoService.getByValuesSelected(childLabels);
    }

    /**
     * 根据筛选条件筛选台账ID
     *
     * @return
     */
    public List<StandingbookLabelInfoDO> getStandingbookIdsByLabel(String topLabel, String childLabels) {

        if (CharSequenceUtil.isBlank(topLabel)) {
            return Collections.emptyList();
        } else {
            if (CharSequenceUtil.isBlank(childLabels)) {
                //只有顶级标签
                return standingbookLabelInfoService.getByLabelNames(Collections.singletonList(topLabel));
            } else {
                // 只有子标签
                List<String> childLabelValues = StrSplitter.split(childLabels, "#", 0, true, true);
                return standingbookLabelInfoService.getByValues(childLabelValues);
            }
        }
    }

    /**
     * 根据筛选条件筛选台账ID
     *
     * @return
     */
    public List<StandingbookLabelInfoDO> getStandingbookIdsByLabel(String topLabel, String childLabels, List<Long> standingBookIdList) {
        if (StrUtil.isBlank(topLabel) && StrUtil.isBlank(childLabels) && CollUtil.isEmpty(standingBookIdList)) {
            return Collections.emptyList();
        }
        if (StrUtil.isBlank(topLabel) && StrUtil.isBlank(childLabels)) {
            return standingbookLabelInfoService.getByStandingBookIds(standingBookIdList);
        }

        //根据标签获取台账
        //只选择顶级标签
        //标签也可能关联重点设备，需要去除重点设备ID
        List<StandingbookLabelInfoDO> byLabelNames = standingbookLabelInfoService.getByLabelNames(Collections.singletonList(topLabel));
        Set<String> childIds = new HashSet<>();
        if (StrUtil.isNotBlank(topLabel) && StrUtil.isBlank(childLabels)) {
            String topId = topLabel.split("_")[1];
            List<LabelConfigDO> childId = labelConfigService.getByParentId(Collections.singletonList(Long.valueOf(topId)));
            Set<String> childIdSet = childId.stream().map(LabelConfigDO::getId).map(String::valueOf).collect(Collectors.toSet());
            childIds.addAll(childIdSet);
            // 顶级跟下级都选择
        } else if (StrUtil.isNotBlank(topLabel) && StrUtil.isNotEmpty(childLabels)) {
            List<String> childLabelValues = StrSplitter.split(childLabels, "#", 0, true, true);
            childIds.addAll(childLabelValues);
        }


        List<StandingbookLabelInfoDO> standingbookLabelInfoList = byLabelNames.stream()
                .filter(standingbook -> {
                    String value = standingbook.getValue();
                    return childIds.stream().anyMatch(value::startsWith);
                })
                .collect(Collectors.toList());
        List<StandingbookLabelInfoDO> standingbookLabelInfoDOS = filterStandingbookLabelInfoDO(standingbookLabelInfoList, childIds, CHILD_LABEL_REGEX_ADD);
        log.info("根据标签查询的计量器具数据：top:{}, childLabels:{}, 计量器具：{}", topLabel, childLabels, JSONUtil.toJsonStr(standingbookLabelInfoDOS));
        return standingbookLabelInfoDOS;
    }


    /**
     * 获取标签关联的计量器具，当当前标签没有计量器具时则获取下一级所有的计量器具，以此类推
     *
     * @param byLabelNames
     * @param childLabelValues
     * @param add
     * @return
     */
    private static List<StandingbookLabelInfoDO> filterStandingbookLabelInfoDO(List<StandingbookLabelInfoDO> byLabelNames, Set<String> childLabelValues, String add) {

        List<StandingbookLabelInfoDO> result = new ArrayList<>();
        if (CollUtil.isEmpty(byLabelNames) || CollUtil.isEmpty(childLabelValues) || add.length() > MAX_DEPTH * CHILD_LABEL_REGEX_ADD.length()) {
            return result;
        }

        Map<String, List<StandingbookLabelInfoDO>> collect = byLabelNames.stream().collect(Collectors.groupingBy(StandingbookLabelInfoDO::getValue));
        List<StandingbookLabelInfoDO> next = new ArrayList<>();
        Set<String> has = new HashSet<>();
        childLabelValues.forEach(childLabel -> {
            //包含只取当前
            if (collect.containsKey(childLabel)) {
                result.addAll(collect.get(childLabel));
                has.add(childLabel);
            } else {
                collect.forEach((k, v) -> {
                    if (matchesCachedRegex(k, childLabel, add)) {
                        result.addAll(v);
                        has.add(k);

                    }
                });

            }
        });

        if (CollUtil.isEmpty(has) && CollUtil.isEmpty(result)) {
            next.addAll(byLabelNames);
        } else {

            List<StandingbookLabelInfoDO> nextList = collect.entrySet()
                    .stream()
                    .filter(entry -> has.stream().noneMatch(prefix -> entry.getKey().startsWith(prefix)))
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toList());
            next.addAll(nextList);

        }

        result.addAll(filterStandingbookLabelInfoDO(next, childLabelValues, add + CHILD_LABEL_REGEX_ADD));
        return result;
    }


    private static boolean matchesCachedRegex(String key, String childLabel, String add) {
        String regexKey = childLabel + add; // 缓存键：标签+递归层级标识
        return PATTERN_CACHE
                .computeIfAbsent(regexKey, k -> Pattern.compile(
                        String.format(CHILD_LABEL_REGEX_PREFIX, childLabel) + add + CHILD_LABEL_REGEX_SUFFIX
                ))
                .matcher(key)
                .matches();
    }


    /**
     * 根据筛选条件筛选能源ID
     *
     * @return
     */
    public List<Long> getEnergyByStandingbookIds(List<Long> standingbookIds) {

        List<StandingbookDO> standingbookList = standingbookService.getByStandingbookIds(standingbookIds);
        List<Long> typeIds = standingbookList.stream().map(StandingbookDO::getTypeId).collect(Collectors.toList());
        List<StandingbookTmplDaqAttrDO> tmplList = standingbookTmplDaqAttrService.getByTypeIds(typeIds);

        return tmplList.stream().map(StandingbookTmplDaqAttrDO::getEnergyId).collect(Collectors.toList());
    }


    public static void main(String[] args) {
     /*   List<StandingbookLabelInfoDO> byLabelNames = new ArrayList<>();
        Set<String> childLabelValues = new HashSet<>();
        //childLabelValues.add("2");
        childLabelValues.add("2,897");
        childLabelValues.add("2,896");

        childLabelValues.add("3,897");


        StandingbookLabelInfoDO infoDO2 = new StandingbookLabelInfoDO();
        infoDO2.setValue("2,897,893");
        StandingbookLabelInfoDO infoDO3 = new StandingbookLabelInfoDO();
        infoDO3.setValue("2,897,89");
        StandingbookLabelInfoDO infoDO4 = new StandingbookLabelInfoDO();
        infoDO4.setValue("3,897,82");
        StandingbookLabelInfoDO infoDO5 = new StandingbookLabelInfoDO();
        infoDO5.setValue("2,897");

        StandingbookLabelInfoDO infoDO6 = new StandingbookLabelInfoDO();
        infoDO6.setValue("2,897,88,88");

        StandingbookLabelInfoDO infoDO7 = new StandingbookLabelInfoDO();
        infoDO7.setValue("2,897,89,88");

        StandingbookLabelInfoDO infoDO8 = new StandingbookLabelInfoDO();
        infoDO8.setValue("2,896,98");


        byLabelNames.add(infoDO2);
        byLabelNames.add(infoDO3);
        byLabelNames.add(infoDO4);
       // byLabelNames.add(infoDO5);
        byLabelNames.add(infoDO6);
        byLabelNames.add(infoDO7);
        byLabelNames.add(infoDO8);


        List<StandingbookLabelInfoDO> standingbookLabelInfoList = byLabelNames.stream()
                .filter(standingbook -> {
                    String value = standingbook.getValue();
                    return childLabelValues.stream().anyMatch(value::startsWith);
                })
                .collect(Collectors.toList());

        List<StandingbookLabelInfoDO> tt = filterStandingbookLabelInfoDO(standingbookLabelInfoList, childLabelValues, CHILD_LABEL_REGEX_ADD);
        tt.forEach(a -> {
            System.out.println(a.getValue());
        });*/


        List<MeasurementAssociationDO> associations = Arrays.asList(
                new MeasurementAssociationDO(90L, 1L, 2L),
                new MeasurementAssociationDO(91L, 2L, 3L),
                new MeasurementAssociationDO(92L, 3L, 4L), // 循环
                new MeasurementAssociationDO(93L, 4L, 5L),
                new MeasurementAssociationDO(94L, 6L, 7L),
                new MeasurementAssociationDO(94L, 7L, 3L)
        );

        Map<Long, StandingbookEnergyTypeVO> energyMap = new HashMap<>();
        energyMap.put(1L, new StandingbookEnergyTypeVO(1L, 2L, 100L)); // 电
        energyMap.put(2L, new StandingbookEnergyTypeVO(2L, 2L, 100L)); // 电
        energyMap.put(3L, new StandingbookEnergyTypeVO(3L, 2L, 100L)); // 电
        energyMap.put(4L, new StandingbookEnergyTypeVO(4L, 2L, 200L)); // 水
        energyMap.put(5L, new StandingbookEnergyTypeVO(5L, 2L, 300L));
        energyMap.put(6L, new StandingbookEnergyTypeVO(6L, 2L, 400L)); // 能源不同，6 是根节点
        energyMap.put(7L, new StandingbookEnergyTypeVO(7L, 2L, 100L)); // 能源不同，6 是根节点

        Set<Long> allNodes = new HashSet<>(energyMap.keySet());
        Map<Long, Set<Long>> memo = new HashMap<>();
        Set<Long> finalRoots = new HashSet<>();


        // 构建下级 -> 上级映射
        Map<Long, Set<Long>> childToParents = new HashMap<>();
        for (MeasurementAssociationDO assoc : associations) {
            childToParents.computeIfAbsent(assoc.getMeasurementId(), k -> new HashSet<>()).add(assoc.getMeasurementInstrumentId());
        }
        for (Long node : allNodes) {
            Set<Long> roots = findRoot(node, childToParents, energyMap, new HashSet<>(), memo);
            finalRoots.addAll(roots);
        }

        System.out.println("根节点: " + finalRoots);


        Map<Long, Set<Long>> energyToRoots = new HashMap<>(); // 能源ID -> 根节点集合
        //Map<Long, Set<Long>> memo = new HashMap<>();

        for (Long node : energyMap.keySet()) {
            Set<Long> roots = findRoot(node, childToParents, energyMap, new HashSet<>(), memo);
            StandingbookEnergyTypeVO standingbookEnergyTypeVO = energyMap.get(node);
            energyToRoots.computeIfAbsent(standingbookEnergyTypeVO.getEnergyId(), k -> new HashSet<>()).addAll(roots);
        }

        // 打印每个能源类型对应的根节点
        energyToRoots.forEach((energy, roots) -> {
            System.out.println("能源类型: " + energy + " 根节点: " + roots);
        });
    }


    /**
     * 获取每个能源的计量器具根节点
     */
    public Map<Long, Set<Long>> getRootNodeStandingbooks() {
        //获取所有台账和能源关系
        List<StandingbookEnergyTypeVO> allEnergyAndType = standingbookService.getAllEnergyAndType();
        //台账能源类型map
        Map<Long, StandingbookEnergyTypeVO> standingbookEnergyTypeVOMap = allEnergyAndType.stream().collect(Collectors.toMap(StandingbookEnergyTypeVO::getStandingbookId, Function.identity()));

        //获取所有计量器具关系
        List<MeasurementAssociationDO> measurementAssociationDOS = measurementAssociationMapper.selectList();


        Map<Long, Set<Long>> energyToRoots = new HashMap<>(); // 能源ID -> 根节点集合
        Map<Long, Set<Long>> memo = new HashMap<>();

        // 构建下级 -> 上级映射
        Map<Long, Set<Long>> childToParents = new HashMap<>();
        for (MeasurementAssociationDO assoc : measurementAssociationDOS) {
            childToParents.computeIfAbsent(assoc.getMeasurementId(), k -> new HashSet<>()).add(assoc.getMeasurementInstrumentId());
        }

        for (Long node : standingbookEnergyTypeVOMap.keySet()) {
            Set<Long> roots = findRoot(node, childToParents, standingbookEnergyTypeVOMap, new HashSet<>(), memo);
            StandingbookEnergyTypeVO standingbookEnergyTypeVO = standingbookEnergyTypeVOMap.get(node);
            energyToRoots.computeIfAbsent(standingbookEnergyTypeVO.getEnergyId(), k -> new HashSet<>()).addAll(roots);
        }
        // 打印每个能源类型对应的根节点
        log.info("能源对应的计量器具根节点: {}", JSONUtil.toJsonStr(energyToRoots));
        return energyToRoots;
    }


    /**
     * 递归返回节点node的所有根节点集合
     *
     * @param node           当前节点
     * @param childToParents 下级到上级映射
     * @param energyMap      计量器具ID对应能源ID
     * @param memo           记忆化缓存
     * @return 根节点ID
     */
    private static Set<Long> findRoot(Long node,
                                      Map<Long, Set<Long>> childToParents,
                                      Map<Long, StandingbookEnergyTypeVO> energyMap,
                                      Set<Long> path,
                                      Map<Long, Set<Long>> memo) {
        if (memo.containsKey(node)) {
            return memo.get(node);
        }

        // 环检测
        if (path.contains(node)) {
            // 环起点就是当前节点，返回单节点集合
            return Collections.singleton(node);
        }

        path.add(node);

        StandingbookEnergyTypeVO nodeEnergy = energyMap.get(node);
        Set<Long> parents = childToParents.getOrDefault(node, Collections.emptySet());

        // 没有父节点，自己就是根
        if (parents.isEmpty()) {
            path.remove(node);
            memo.put(node, Collections.singleton(node));
            return memo.get(node);
        }

        Set<Long> roots = new HashSet<>();

        boolean hasDifferentEnergyParent = false;
        for (Long p : parents) {
            StandingbookEnergyTypeVO parentEnergy = energyMap.get(p);
            if (Objects.isNull(parentEnergy) || Objects.isNull(nodeEnergy) || !parentEnergy.getEnergyId().equals(nodeEnergy.getEnergyId())) {
                // 能源不同，当前节点就是根
                hasDifferentEnergyParent = true;
                break;
            }
        }

        if (hasDifferentEnergyParent) {
            path.remove(node);
            memo.put(node, Collections.singleton(node));
            return memo.get(node);
        }

        // 父节点能源都相同，递归查找父节点根节点
        for (Long p : parents) {
            Set<Long> parentRoots = findRoot(p, childToParents, energyMap, path, memo);
            roots.addAll(parentRoots);
        }

        path.remove(node);

        memo.put(node, roots);
        return roots;
    }


}
