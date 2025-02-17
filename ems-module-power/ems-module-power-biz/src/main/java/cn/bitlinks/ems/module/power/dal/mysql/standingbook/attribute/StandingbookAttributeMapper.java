package cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 台账属性 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookAttributeMapper extends BaseMapperX<StandingbookAttributeDO> {

    default PageResult<StandingbookAttributeDO> selectPage(StandingbookAttributePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StandingbookAttributeDO>()
                .likeIfPresent(StandingbookAttributeDO::getName, reqVO.getName())
                .eqIfPresent(StandingbookAttributeDO::getValue, reqVO.getValue())
                .eqIfPresent(StandingbookAttributeDO::getTypeId, reqVO.getTypeId())
                .eqIfPresent(StandingbookAttributeDO::getStandingbookId, reqVO.getStandingbookId())
                .eqIfPresent(StandingbookAttributeDO::getFileId, reqVO.getFileId())
                .eqIfPresent(StandingbookAttributeDO::getIsRequired, reqVO.getIsRequired())
                .eqIfPresent(StandingbookAttributeDO::getCode, reqVO.getCode())
                .eqIfPresent(StandingbookAttributeDO::getSort, reqVO.getSort())
                .eqIfPresent(StandingbookAttributeDO::getFormat, reqVO.getFormat())
                .eqIfPresent(StandingbookAttributeDO::getNode, reqVO.getNode())
                .eqIfPresent(StandingbookAttributeDO::getDescription, reqVO.getDescription())
                .betweenIfPresent(StandingbookAttributeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StandingbookAttributeDO::getId));
    }
    @Select("SELECT * FROM power_standingbook_attribute WHERE type_id = #{typeId} and deleted=0 and standingbook_id IS NULL order by sort")
     List<StandingbookAttributeDO> selectTypeId(@Param("typeId")Long typeId) ;

    @Delete("DELETE FROM power_standingbook_attribute WHERE type_id = #{typeId} and deleted=0 and standingbook_id IS NULL")
    int deleteTypeId( @Param("typeId") Long typeId) ;

    @Select("SELECT * FROM power_standingbook_attribute WHERE standingbook_id = #{standingbookId} and deleted=0 order by sort")
     List<StandingbookAttributeDO> selectStandingbookId(@Param("standingbookId") Long standingbookId) ;


    default int deleteStandingbookId(Long standingbookId) {
        return delete(StandingbookAttributeDO::getStandingbookId, standingbookId);
    }

    List<StandingbookDO> selectStandingbook(@Param("list")List<StandingbookAttributePageReqVO> list,@Param("typeId")Long typeId);
}
