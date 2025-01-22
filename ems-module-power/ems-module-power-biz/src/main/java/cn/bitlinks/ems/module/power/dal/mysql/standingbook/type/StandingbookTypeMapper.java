package cn.bitlinks.ems.module.power.dal.mysql.standingbook.type;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 台账类型 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookTypeMapper extends BaseMapperX<StandingbookTypeDO> {

    default List<StandingbookTypeDO> selectList(StandingbookTypeListReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                .likeIfPresent(StandingbookTypeDO::getName, reqVO.getName())
                .eqIfPresent(StandingbookTypeDO::getSuperId, reqVO.getSuperId())

                .eqIfPresent(StandingbookTypeDO::getTopType, reqVO.getTopType())
                .eqIfPresent(StandingbookTypeDO::getSort, reqVO.getSort())
                .eqIfPresent(StandingbookTypeDO::getLevel, reqVO.getLevel())
                .eqIfPresent(StandingbookTypeDO::getCode, reqVO.getCode())
                .eqIfPresent(StandingbookTypeDO::getDescription, reqVO.getDescription())
                .betweenIfPresent(StandingbookTypeDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StandingbookTypeDO::getId));
    }

	default StandingbookTypeDO selectBySuperIdAndName(Long superId, String name) {
	    return selectOne(StandingbookTypeDO::getSuperId, superId, StandingbookTypeDO::getName, name);
	}

    default Long selectCountBySuperId(Long superId) {
        return selectCount(StandingbookTypeDO::getSuperId, superId);
    }
    default  List<StandingbookTypeDO> selectNotDelete() {
        return selectList(
                );
    }

    default List<StandingbookTypeDO> selectByName(String name){
        return   selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                .likeIfPresent(StandingbookTypeDO::getName, name)
                .orderByDesc(StandingbookTypeDO::getId));
    }

    // Mapper 接口
    @Select("SELECT value FROM power_standingbook_attribute WHERE standingbook_id = #{standingbookId} AND code = #{code} AND deleted=0 ORDER BY sort")
    String selectAttributeValueByCode(@Param("standingbookId") Long standingbookId, @Param("code") String code);
}
