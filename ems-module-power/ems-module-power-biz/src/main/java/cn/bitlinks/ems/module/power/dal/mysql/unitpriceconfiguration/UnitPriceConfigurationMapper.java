package cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration;

import java.time.LocalDateTime;
import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 单价配置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface UnitPriceConfigurationMapper extends BaseMapperX<UnitPriceConfigurationDO> {

    default PageResult<UnitPriceConfigurationDO> selectPage(UnitPriceConfigurationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<UnitPriceConfigurationDO>()
                .eqIfPresent(UnitPriceConfigurationDO::getEnergyId, reqVO.getEnergyId())
                .betweenIfPresent(UnitPriceConfigurationDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(UnitPriceConfigurationDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(UnitPriceConfigurationDO::getBillingMethod, reqVO.getBillingMethod())
                .eqIfPresent(UnitPriceConfigurationDO::getAccountingFrequency, reqVO.getAccountingFrequency())
                .eqIfPresent(UnitPriceConfigurationDO::getPriceDetails, reqVO.getPriceDetails())
                .eqIfPresent(UnitPriceConfigurationDO::getFormula, reqVO.getFormula())
                .betweenIfPresent(UnitPriceConfigurationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(UnitPriceConfigurationDO::getId));
    }

    @Select("SELECT price_details FROM ems_unit_price_configuration " +
            "WHERE energy_id = #{energyId} AND start_time <= #{currentDateTime} AND end_time >= #{currentDateTime}")
    String getPriceDetailsByEnergyIdAndTime(@Param("energyId") Long energyId, @Param("currentDateTime") LocalDateTime currentDateTime);

}