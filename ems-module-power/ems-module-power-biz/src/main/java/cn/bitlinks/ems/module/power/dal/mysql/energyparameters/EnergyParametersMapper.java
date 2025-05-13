package cn.bitlinks.ems.module.power.dal.mysql.energyparameters;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 能源参数 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface EnergyParametersMapper extends BaseMapperX<EnergyParametersDO> {

    default PageResult<EnergyParametersDO> selectPage(EnergyParametersPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnergyParametersDO>()
                .eqIfPresent(EnergyParametersDO::getEnergyId, reqVO.getEnergyId())
                .eqIfPresent(EnergyParametersDO::getParameter, reqVO.getParameter())
                .eqIfPresent(EnergyParametersDO::getCode, reqVO.getCode())
                .eqIfPresent(EnergyParametersDO::getDataFeature, reqVO.getDataFeature())
                .eqIfPresent(EnergyParametersDO::getUnit, reqVO.getUnit())
                .eqIfPresent(EnergyParametersDO::getDataType, reqVO.getDataType())
                .eqIfPresent(EnergyParametersDO::getUsage, reqVO.getUsage())
                .betweenIfPresent(EnergyParametersDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(EnergyParametersDO::getId));
    }

    int deleteByEnergyId(@Param("energyId") Long energyId);

    int deleteByEnergyIds(@Param("energyIds") List<Long> energyIds);

    List<EnergyParametersDO> selectByEnergyId(@Param("energyId") Long energyId);

    List<EnergyParametersDO> selectListByEnergyIds(@Param("energyIds") List<Long> energyIds);

}