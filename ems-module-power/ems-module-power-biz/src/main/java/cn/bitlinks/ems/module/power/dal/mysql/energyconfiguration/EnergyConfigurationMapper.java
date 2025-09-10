package cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 能源配置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface EnergyConfigurationMapper extends BaseMapperX<EnergyConfigurationDO> {

    default PageResult<EnergyConfigurationDO> selectPage(EnergyConfigurationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnergyConfigurationDO>()
                .eqIfPresent(EnergyConfigurationDO::getGroupId, reqVO.getGroupId())
                .likeIfPresent(EnergyConfigurationDO::getEnergyName, reqVO.getEnergyName())
                .eqIfPresent(EnergyConfigurationDO::getCode, reqVO.getCode())
                .eqIfPresent(EnergyConfigurationDO::getEnergyClassify, reqVO.getEnergyClassify())
                .eqIfPresent(EnergyConfigurationDO::getFactor, reqVO.getFactor())
                .inIfPresent(EnergyConfigurationDO::getId, reqVO.getEnergyIds())
                .betweenIfPresent(EnergyConfigurationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(EnergyConfigurationDO::getId));
    }

    default List<EnergyConfigurationDO> selectList(EnergyConfigurationPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<EnergyConfigurationDO>()
                .eqIfPresent(EnergyConfigurationDO::getGroupId, reqVO.getGroupId())
                .likeIfPresent(EnergyConfigurationDO::getEnergyName, reqVO.getEnergyName())
                .eqIfPresent(EnergyConfigurationDO::getCode, reqVO.getCode())
                .eqIfPresent(EnergyConfigurationDO::getEnergyClassify, reqVO.getEnergyClassify())
                .eqIfPresent(EnergyConfigurationDO::getFactor, reqVO.getFactor())
                .inIfPresent(EnergyConfigurationDO::getId, reqVO.getEnergyIds())
                .orderByAsc(EnergyConfigurationDO::getId));
    }
    default List<EnergyConfigurationDO> getByEnergyClassifyUnit(
            Integer energyClassify){
        MPJLambdaWrapperX<EnergyConfigurationDO> query = new MPJLambdaWrapperX<EnergyConfigurationDO>()
                .selectAll(EnergyConfigurationDO.class)
                .selectAs(EnergyParametersDO::getUnit,"unit")
                .eq(EnergyParametersDO::getUsage, 1)
                .eqIfPresent(EnergyConfigurationDO::getEnergyClassify, energyClassify);
        query.leftJoin(EnergyParametersDO.class,EnergyParametersDO::getEnergyId, EnergyConfigurationDO::getId);


        return selectJoinList(EnergyConfigurationDO.class, query);
    }

    @Select("SELECT COUNT(*) FROM ems_energy_configuration " +
            "WHERE code = #{code} " +
            "AND deleted = 0 " +  // 新增条件：仅校验未删除的数据
            "AND (id != #{id} OR #{id} IS NULL)")
    int countByCodeAndNotId(@Param("code") String code, @Param("id") Long id);

    @Select("SELECT COUNT(*) FROM ems_energy_configuration " +
            "WHERE energy_name = #{energyName} AND (id != #{id} OR #{id} IS NULL)")
    int countByEnergyNameAndNotId(@Param("energyName") String energyName, @Param("id") Long id);

    @Select("SELECT * FROM ems_energy_configuration " +
            "WHERE energy_name = #{energyName}")
    EnergyConfigurationSaveReqVO selectByEnergyName(@Param("energyName") String energyName);

    @Select({
            "SELECT unit ",
            "FROM ems_energy_parameters ",
            "WHERE energy_id = #{energyId} ",
            "  AND 'usage' = 1 ", // 直接通过usage字段过滤
            "  AND deleted = 0 ", // 增加删除状态过滤
            "LIMIT 1" // 确保唯一性
    })
    String selectUnitByEnergyNameAndChinese(@Param("energyId") Long energyId);



    List<EnergyConfigurationDO> getByEnergyClassify(@Param("energyIds") Set<Long> energyIds,
                                                    @Param("energyClassify") Integer energyClassify);

    List<EnergyConfigurationDO> getHasUnitByEnergyClassify(@Param("energyIds") Set<Long> energyIds,
                                                    @Param("energyClassify") Integer energyClassify);


    EnergyConfigurationDO getEnergyAndUnit(@Param("energyId")Long energyId);

    List<Long> getEnergyIdByCode(@Param("energyCodes")List<String> energyCodes);
}