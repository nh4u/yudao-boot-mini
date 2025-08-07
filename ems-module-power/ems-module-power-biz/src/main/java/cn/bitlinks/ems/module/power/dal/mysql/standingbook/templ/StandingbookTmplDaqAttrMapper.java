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

    /**
     * 查询绑定的台账分类ids
     *
     * @param energyId 能源id
     * @return 台账分类ids
     */
    @Select("SELECT distinct type_id FROM power_standingbook_tmpl_daq_attr  " +
            "WHERE energy_id = #{energyId} AND raw_attr_id is null AND deleted = 0")
    List<Long> selectRawSbTypeIdsByEnergyId(@Param("energyId") Long energyId);

    /**
     * 查询能源相关的台账分类ids
     *
     * @param energyId 能源id
     * @return 台账分类ids
     */
    @Select("SELECT distinct type_id FROM power_standingbook_tmpl_daq_attr  " +
            "WHERE energy_id = #{energyId} AND deleted = 0")
    List<Long> selectSbTypeIdsByEnergyId(@Param("energyId") Long energyId);

    /**
     * 根据多个能源ID查询能源相关的台账分类ids
     *
     * @param energyIds 能源id列表
     * @return 台账分类ids
     */
    List<Long> selectSbTypeIdsByEnergyIds(@Param("energyIds") List<Long> energyIds);

    /**
     * 查询分类能源关联关系
     *
     * @param typeIds
     * @return
     */
    List<StandingbookTmplDaqAttrDO> selectEnergyMapping(@Param("typeIds") List<Long> typeIds);

    /**
     * 根据属性名字和台账获取属性code
     *
     * @param standingBookId
     * @param paramName
     * @return
     */
    @Select("SELECT `code` FROM power_standingbook_tmpl_daq_attr  " +
            "WHERE type_id = (select type_id from power_standingbook where id = #{standingBookId} and deleted = 0) " +
            "and parameter =  #{paramName} and energy_flag = 1 AND deleted = 0 order by create_time desc limit 1")
    String getParamCode(@Param("standingBookId") Long standingBookId, @Param("paramName") String paramName);
}