package cn.bitlinks.ems.module.power.dal.mysql.standingbook;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 台账属性 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookMapper extends BaseMapperX<StandingbookDO> {

    default PageResult<StandingbookDO> selectPage(StandingbookPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StandingbookDO>()
                .likeIfPresent(StandingbookDO::getName, reqVO.getName())
                .eqIfPresent(StandingbookDO::getLabelInfo, reqVO.getLabelInfo())
                .eqIfPresent(StandingbookDO::getStage, reqVO.getStage())
                .eqIfPresent(StandingbookDO::getDescription, reqVO.getDescription())
                .eqIfPresent(StandingbookDO::getTypeId, reqVO.getTypeId())
                .betweenIfPresent(StandingbookDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StandingbookDO::getId));
    }

}
