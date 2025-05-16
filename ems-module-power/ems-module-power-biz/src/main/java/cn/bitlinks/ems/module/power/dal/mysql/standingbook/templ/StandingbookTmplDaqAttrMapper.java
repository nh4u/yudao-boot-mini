package cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 台账分类的数采参数表（自定义和能源） Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookTmplDaqAttrMapper extends BaseMapperX<StandingbookTmplDaqAttrDO> {

    /**
     * 查询绑定的台账分类ids
     * @param energyId 能源id
     * @return 台账分类ids
     */
    @Select("SELECT distinct type_id FROM power_standingbook_tmpl_daq_attr  " +
            "WHERE energy_id = #{energyId} AND raw_attr_id is null AND deleted = 0")
    List<Long> selectRawSbTypeIdsByEnergyId(@Param("energyId") Long energyId);
    /**
     * 查询能源相关的台账分类ids
     * @param energyId 能源id
     * @return 台账分类ids
     */
    @Select("SELECT distinct type_id FROM power_standingbook_tmpl_daq_attr  " +
            "WHERE energy_id = #{energyId} AND deleted = 0")
    List<Long> selectSbTypeIdsByEnergyId(@Param("energyId") Long energyId);

    /**
     * 查询分类-关联能源
     * @return typeId-energyId
     */
    @Select("SELECT distinct type_id,energy_id FROM `power_standingbook_tmpl_daq_attr` where energy_id is not null and deleted = 0 and status = 1")
    Map<Long, Long> selectEnergyMapping();
}