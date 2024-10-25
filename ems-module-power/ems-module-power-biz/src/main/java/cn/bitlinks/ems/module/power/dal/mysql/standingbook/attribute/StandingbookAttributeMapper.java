package cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import org.apache.ibatis.annotations.Mapper;

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

    default List<StandingbookAttributeDO> selectTypeId(Long typeId) {
        return selectList(StandingbookAttributeDO::getTypeId, typeId);
    }
    default int deleteTypeId(Long typeId) {
        return delete(StandingbookAttributeDO::getTypeId, typeId);
    }

    default List<StandingbookAttributeDO> selectStandingbookId(Long standingbookId) {
        return selectList(StandingbookAttributeDO::getStandingbookId, standingbookId);
    }
    default int deleteStandingbookId(Long standingbookId) {
        return delete(StandingbookAttributeDO::getStandingbookId, standingbookId);
    }

}
