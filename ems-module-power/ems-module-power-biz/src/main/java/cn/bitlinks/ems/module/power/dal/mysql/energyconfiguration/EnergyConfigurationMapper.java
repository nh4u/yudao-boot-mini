package cn.bitlinks.ems.module.power.dal.mysql.energyconfiguration;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
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
                .likeIfPresent(EnergyConfigurationDO::getEnergyName, reqVO.getEnergyName())
                .eqIfPresent(EnergyConfigurationDO::getCode, reqVO.getCode())
                .eqIfPresent(EnergyConfigurationDO::getEnergyClassify, reqVO.getEnergyClassify())
                .eqIfPresent(EnergyConfigurationDO::getEnergyIcon, reqVO.getEnergyIcon())
                .eqIfPresent(EnergyConfigurationDO::getEnergyParameter, reqVO.getEnergyParameter())
                .eqIfPresent(EnergyConfigurationDO::getFactor, reqVO.getFactor())
                .eqIfPresent(EnergyConfigurationDO::getCoalFormula, reqVO.getCoalFormula())
                .eqIfPresent(EnergyConfigurationDO::getCoalScale, reqVO.getCoalScale())
                .betweenIfPresent(EnergyConfigurationDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(EnergyConfigurationDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(EnergyConfigurationDO::getBillingMethod, reqVO.getBillingMethod())
                .eqIfPresent(EnergyConfigurationDO::getAccountingFrequency, reqVO.getAccountingFrequency())
                .eqIfPresent(EnergyConfigurationDO::getUnitPrice, reqVO.getUnitPrice())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceFormula, reqVO.getUnitPriceFormula())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceScale, reqVO.getUnitPriceScale())
                .inIfPresent(EnergyConfigurationDO::getId, reqVO.getEnergyIds())
                .betweenIfPresent(EnergyConfigurationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(EnergyConfigurationDO::getId));
    }

    default List<EnergyConfigurationDO> selectList(EnergyConfigurationPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<EnergyConfigurationDO>()
                .likeIfPresent(EnergyConfigurationDO::getEnergyName, reqVO.getEnergyName())
                .eqIfPresent(EnergyConfigurationDO::getCode, reqVO.getCode())
                .eqIfPresent(EnergyConfigurationDO::getEnergyClassify, reqVO.getEnergyClassify())
                .eqIfPresent(EnergyConfigurationDO::getEnergyIcon, reqVO.getEnergyIcon())
                .eqIfPresent(EnergyConfigurationDO::getEnergyParameter, reqVO.getEnergyParameter())
                .eqIfPresent(EnergyConfigurationDO::getFactor, reqVO.getFactor())
                .eqIfPresent(EnergyConfigurationDO::getCoalFormula, reqVO.getCoalFormula())
                .eqIfPresent(EnergyConfigurationDO::getCoalScale, reqVO.getCoalScale())
                .eqIfPresent(EnergyConfigurationDO::getBillingMethod, reqVO.getBillingMethod())
                .eqIfPresent(EnergyConfigurationDO::getAccountingFrequency, reqVO.getAccountingFrequency())
                .eqIfPresent(EnergyConfigurationDO::getUnitPrice, reqVO.getUnitPrice())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceFormula, reqVO.getUnitPriceFormula())
                .eqIfPresent(EnergyConfigurationDO::getUnitPriceScale, reqVO.getUnitPriceScale())
                .inIfPresent(EnergyConfigurationDO::getId, reqVO.getEnergyIds())
                .orderByAsc(EnergyConfigurationDO::getId));
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
            "SELECT",
            "  JSON_UNQUOTE(",
            "    JSON_EXTRACT(",
            "      energy_parameter,",
            "      REPLACE(",
            "        JSON_UNQUOTE(",
            "          JSON_SEARCH(",
            "            energy_parameter,",
            "            'one',",
            "            '用量',",  // 这里固定为'用量'
            "            NULL,",
            "            '$[*].chinese'",
            "          )",
            "        ),",
            "        '.chinese',",
            "        '.unit'",
            "      )",
            "    )",
            "  ) AS unit",
            "FROM ems_energy_configuration",
            "WHERE id = #{energyId}",
            "LIMIT 1"
    })
    String selectUnitByEnergyNameAndChinese(@Param("energyName") String energyId);
}