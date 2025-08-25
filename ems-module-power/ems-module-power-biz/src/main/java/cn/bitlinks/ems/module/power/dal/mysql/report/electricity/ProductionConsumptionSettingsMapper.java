package cn.bitlinks.ems.module.power.dal.mysql.report.electricity;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ProductionConsumptionReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ProductionConsumptionSettingsPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.electricity.ProductionConsumptionSettingsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author liumingqiang
 */
@Mapper
public interface ProductionConsumptionSettingsMapper extends BaseMapperX<ProductionConsumptionSettingsDO> {

    default PageResult<ProductionConsumptionSettingsDO> selectPage(ProductionConsumptionSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ProductionConsumptionSettingsDO>()
                .eqIfPresent(ProductionConsumptionSettingsDO::getName, reqVO.getName())
                .eqIfPresent(ProductionConsumptionSettingsDO::getStandingbookId, reqVO.getStandingbookId())
                .orderByAsc(ProductionConsumptionSettingsDO::getCreateTime));
    }

    /**
     * 获取所有系统
     *
     * @return
     */
    @Select("select distinct `name` from power_production_consumption_settings where  deleted = 0")
    List<String> getName();

    /**
     * 根据系统获取所有供水温度参数数据
     *
     * @param reqVO
     * @return
     */
    default List<ProductionConsumptionSettingsDO> selectList(ProductionConsumptionReportParamVO reqVO) {
        return selectList(new LambdaQueryWrapperX<ProductionConsumptionSettingsDO>()
                .inIfPresent(ProductionConsumptionSettingsDO::getName, reqVO.getNameList())
                .orderByAsc(ProductionConsumptionSettingsDO::getCreateTime));
    }

    /**
     * 根据系统获取对应供水温度参数数据
     *
     * @param name
     * @return
     */
    default ProductionConsumptionSettingsDO selectOne(String name) {
        return selectOne(new LambdaQueryWrapperX<ProductionConsumptionSettingsDO>()
                .eq(ProductionConsumptionSettingsDO::getName, name)
                .orderByAsc(ProductionConsumptionSettingsDO::getCreateTime).last("limit 1"), false);
    }

}