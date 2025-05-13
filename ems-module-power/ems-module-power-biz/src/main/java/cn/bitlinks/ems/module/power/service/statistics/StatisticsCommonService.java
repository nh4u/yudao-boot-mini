package cn.bitlinks.ems.module.power.service.statistics;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.label.StandingbookLabelInfoService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
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
    private StandingbookLabelInfoService standingbookLabelInfoService;

    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;

    @Resource
    private StandingbookService standingbookService;

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
    public List<StandingbookLabelInfoDO> getStandingbookIdsByLabel(String topLabel, String childLabels, List<Long> standingBookIdList) {
        if(StrUtil.isBlank(topLabel) && StrUtil.isBlank(childLabels)){
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


}
