package cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.*;

/**
 * 能源配置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface EnergyConfigurationMapper extends BaseMapperX<EnergyConfigurationDO> {

    default PageResult<EnergyConfigurationDO> selectPage(EnergyConfigurationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnergyConfigurationDO>()
                .likeIfPresent(EnergyConfigurationDO::getEnergyName, reqVO.getEnergyName())
                .eqIfPresent(EnergyConfigurationDO::getCode, reqVO.getCode())
                .eqIfPresent(EnergyConfigurationDO::getEnergyClassify, reqVO.getEnergyClassify())
                .eqIfPresent(EnergyConfigurationDO::getEnergyParameter, reqVO.getEnergyParameter())
                .eqIfPresent(EnergyConfigurationDO::getFactor, reqVO.getFactor())
                .eqIfPresent(EnergyConfigurationDO::getCoalFormula, reqVO.getCoalFormula())
                .eqIfPresent(EnergyConfigurationDO::getCoalScale, reqVO.getCoalScale())
                .betweenIfPresent(EnergyConfigurationDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(EnergyConfigurationDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(EnergyConfigurationDO::getBillingMethod, reqVO.getBillingMethod())
                .eqIfPresent(EnergyConfigurationDO::getUnitPrice, reqVO.getUnitPrice())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceFormula, reqVO.getUnitPriceFormula())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceScale, reqVO.getUnitPriceScale())
                .betweenIfPresent(EnergyConfigurationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(EnergyConfigurationDO::getId));
    }

    default List<EnergyConfigurationDO> selectList(EnergyConfigurationPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<EnergyConfigurationDO>()
                .likeIfPresent(EnergyConfigurationDO::getEnergyName, reqVO.getEnergyName())
                .eqIfPresent(EnergyConfigurationDO::getCode, reqVO.getCode())
                .eqIfPresent(EnergyConfigurationDO::getEnergyClassify, reqVO.getEnergyClassify())
                .eqIfPresent(EnergyConfigurationDO::getEnergyParameter, reqVO.getEnergyParameter())
                .eqIfPresent(EnergyConfigurationDO::getFactor, reqVO.getFactor())
                .eqIfPresent(EnergyConfigurationDO::getCoalFormula, reqVO.getCoalFormula())
                .eqIfPresent(EnergyConfigurationDO::getCoalScale, reqVO.getCoalScale())
                .eqIfPresent(EnergyConfigurationDO::getBillingMethod, reqVO.getBillingMethod())
                .eqIfPresent(EnergyConfigurationDO::getUnitPrice, reqVO.getUnitPrice())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceFormula, reqVO.getUnitPriceFormula())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceScale, reqVO.getUnitPriceScale())
                .inIfPresent(EnergyConfigurationDO::getId, reqVO.getEnergyIds())
                .orderByAsc(EnergyConfigurationDO::getId));
    }

}