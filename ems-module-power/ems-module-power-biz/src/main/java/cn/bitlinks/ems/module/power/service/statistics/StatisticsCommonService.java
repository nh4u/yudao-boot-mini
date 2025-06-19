package cn.bitlinks.ems.module.power.service.statistics;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.label.StandingbookLabelInfoService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;


/**
 * @author wangl
 * @date 2025年05月09日 10:10
 */
@Service
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
    private StandingbookService standingbookService;

    private static final String CHILD_LABEL_REGEX_PREFIX = "^%s";
    private static final String CHILD_LABEL_REGEX_SUFFIX = "$";
    private static final String CHILD_LABEL_REGEX_ADD = ",\\d+";

    //递归最大深度，目前标签支持5层，递归不包含顶层递归，所以最多是4层，设置5层为了扩宽
    private static final Integer MAX_DEPTH = 5;

    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();


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
        List<StandingbookLabelInfoDO> labelInfoDOList = new ArrayList<>();
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

        return filterStandingbookLabelInfoDO(standingbookLabelInfoList, childIds, CHILD_LABEL_REGEX_ADD);
    }




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
            }else {
                collect.forEach((k, v) -> {
                    if (matchesCachedRegex(k, childLabel, add)) {
                        result.addAll(v);
                        has.add(k);

                    }
                });

            }
        });

        if(CollUtil.isEmpty(has) && CollUtil.isEmpty(result)){
            next.addAll(byLabelNames);
        }else {

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



    public static void main(String[] args) {
        List<StandingbookLabelInfoDO> byLabelNames = new ArrayList<>();
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
        });
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


}
