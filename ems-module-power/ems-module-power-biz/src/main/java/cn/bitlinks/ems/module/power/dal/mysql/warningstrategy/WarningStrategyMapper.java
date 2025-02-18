package cn.bitlinks.ems.module.power.dal.mysql.warningstrategy;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.WarningStrategyPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.WarningStrategyRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 告警策略 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface WarningStrategyMapper extends BaseMapperX<WarningStrategyDO> {

    List<WarningStrategyRespVO> getPage(@Param("reqVO") WarningStrategyPageReqVO reqVO, @Param("offset") Integer offset);

    Long getCount(@Param("reqVO") WarningStrategyPageReqVO reqVO);

}