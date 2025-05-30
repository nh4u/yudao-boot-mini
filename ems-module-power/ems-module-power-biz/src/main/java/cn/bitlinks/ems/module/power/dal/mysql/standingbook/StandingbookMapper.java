package cn.bitlinks.ems.module.power.dal.mysql.standingbook;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 台账属性 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookMapper extends BaseMapperX<StandingbookDO> {


    /**
     * 根据多条件查询台账id
     *
     * @param typeId        台账分类id
     * @param typeIds       台账分类ids列表
     * @param stage         环节
     * @param createTimeArr 创建时间数组
     * @return sbId
     */
    List<Long> selectStandingbookIdByCondition(@Param("typeId") Long typeId, @Param("typeIds") List<Long> typeIds, @Param("stage") Integer stage, @Param("createTimeArr") List<String> createTimeArr);
}
