package cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 台账属性 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookAttributeMapper extends BaseMapperX<StandingbookAttributeDO> {


    @Select("SELECT * FROM power_standingbook_attribute WHERE type_id = #{typeId} and deleted=0 and standingbook_id IS NULL order by sort")
    List<StandingbookAttributeDO> selectTypeId(@Param("typeId") Long typeId);

    @Select("SELECT * FROM power_standingbook_attribute WHERE standingbook_id = #{standingbookId} and deleted=0 order by sort")
    List<StandingbookAttributeDO> selectStandingbookId(@Param("standingbookId") Long standingbookId);


    default int deleteStandingbookId(Long standingbookId) {
        return delete(StandingbookAttributeDO::getStandingbookId, standingbookId);
    }

    /**
     * 根据台账属性条件查询获取台账id
     *
     * @param children 台账属性
     * @param sbIds    台账ids
     * @return
     */
    List<Long> selectStandingbookIdByAttrCondition(@Param("children") Map<String, List<String>> children, @Param("sbIds") List<Long> sbIds);
}
