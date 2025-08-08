package cn.bitlinks.ems.module.power.dal.mysql.report.supplywatertmp;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author liumingqiang
 */
@Mapper
public interface SupplyWaterTmpSettingsMapper extends BaseMapperX<SupplyWaterTmpSettingsDO> {

    default PageResult<SupplyWaterTmpSettingsDO> selectPage(SupplyWaterTmpSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SupplyWaterTmpSettingsDO>()
                .eqIfPresent(SupplyWaterTmpSettingsDO::getSystem, reqVO.getSystem())
                .eqIfPresent(SupplyWaterTmpSettingsDO::getStandingbookId, reqVO.getStandingbookId())
                .orderByAsc(SupplyWaterTmpSettingsDO::getCreateTime));
    }

    /**
     * 获取所有系统
     *
     * @return
     */
    @Select("select distinct `system` from power_supply_water_tmp_settings where  deleted = 0")
    List<String> getSystem();

    /**
     * 根据copType获取所有cop参数数据
     *
     * @param reqVO
     * @return
     */
    default List<SupplyWaterTmpSettingsDO> selectList(SupplyWaterTmpReportParamVO reqVO) {
        return selectList(new LambdaQueryWrapperX<SupplyWaterTmpSettingsDO>()
                .inIfPresent(SupplyWaterTmpSettingsDO::getSystem, reqVO.getSystem())
                .orderByAsc(SupplyWaterTmpSettingsDO::getCreateTime));
    }
}