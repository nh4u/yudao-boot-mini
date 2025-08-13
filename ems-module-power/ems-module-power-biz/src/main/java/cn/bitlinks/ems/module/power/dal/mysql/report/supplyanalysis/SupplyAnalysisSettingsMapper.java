package cn.bitlinks.ems.module.power.dal.mysql.report.supplyanalysis;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplyanalysis.SupplyAnalysisSettingsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author liumingqiang
 */
@Mapper
public interface SupplyAnalysisSettingsMapper extends BaseMapperX<SupplyAnalysisSettingsDO> {

    default PageResult<SupplyAnalysisSettingsDO> selectPage(SupplyAnalysisSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SupplyAnalysisSettingsDO>()
                .eqIfPresent(SupplyAnalysisSettingsDO::getSystem, reqVO.getSystem())
                .eqIfPresent(SupplyAnalysisSettingsDO::getItem, reqVO.getItem())
                .eqIfPresent(SupplyAnalysisSettingsDO::getStandingbookId, reqVO.getStandingbookId())
                .orderByAsc(SupplyAnalysisSettingsDO::getCreateTime));
    }

    /**
     * 获取所有系统
     *
     * @return
     */
    @Select("select distinct `system` from power_supply_analysis_settings where  deleted = 0")
    List<String> getSystem();

    /**
     * 根据copType获取所有cop参数数据
     *
     * @param reqVO
     * @return
     */
    default List<SupplyAnalysisSettingsDO> selectList(SupplyAnalysisReportParamVO reqVO) {
        return selectList(new LambdaQueryWrapperX<SupplyAnalysisSettingsDO>()
                .inIfPresent(SupplyAnalysisSettingsDO::getSystem, reqVO.getSystem())
                .orderByAsc(SupplyAnalysisSettingsDO::getCreateTime));
    }
}