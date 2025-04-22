package cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 台账分类的数采参数表（自定义和能源） Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookTmplDaqAttrMapper extends BaseMapperX<StandingbookTmplDaqAttrDO> {


    @Select("SELECT distinct type_id FROM power_standingbook_tmpl_daq_attr  " +
            "WHERE energy_id = #{energyId} AND raw_attr_id is null AND deleted = 0")
    List<Long> selectSbTypeIdsByEnergyId(@Param("energyId") Long energyId);
}