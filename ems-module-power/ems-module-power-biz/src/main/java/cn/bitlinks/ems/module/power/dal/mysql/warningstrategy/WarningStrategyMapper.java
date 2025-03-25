package cn.bitlinks.ems.module.power.dal.mysql.warningstrategy;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.WarningStrategyPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.WarningStrategyPageRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 告警策略 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface WarningStrategyMapper extends BaseMapperX<WarningStrategyDO> {

    List<WarningStrategyPageRespVO> getPage(@Param("reqVO") WarningStrategyPageReqVO reqVO, @Param("offset") Integer offset);

    Long getCount(@Param("reqVO") WarningStrategyPageReqVO reqVO);

    @Select("SELECT site_template_id FROM power_warning_strategy WHERE site_template_id IS NOT NULL UNION ALL SELECT mail_template_id FROM power_warning_strategy WHERE mail_template_id IS NOT NULL")
    List<Long> getAllTemplateIds();
}