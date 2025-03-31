package cn.bitlinks.ems.module.power.dal.mysql.starrocks;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 台账属性 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StarrocksMapper {
    List<Map<String, Objects>> queryData();

    void addData(@Param("date") String date);

    void deleteData(String date);
}
