package cn.bitlinks.ems.module.power.dal.mysql.standingbook;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

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

    /**
     * 根据多条件查询台账id
     * @param labelInfoConditions 标签条件参数列表
     * @param typeId 台账分类id
     * @param typeIds 台账分类ids列表
     * @param stage 环节
     * @param createTimeArr 创建时间数组
     * @return sbId
     */
    List<Long> selectStandingbookIdByCondition(@Param("labelInfoConditions") Map<String, List<String>> labelInfoConditions,@Param("typeId") Long typeId, @Param("typeIds") List<String> typeIds,@Param("stage") Integer stage, @Param("createTimeArr")List<String> createTimeArr);
}
