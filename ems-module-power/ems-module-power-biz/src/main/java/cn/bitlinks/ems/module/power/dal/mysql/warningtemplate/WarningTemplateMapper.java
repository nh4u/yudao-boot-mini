package cn.bitlinks.ems.module.power.dal.mysql.warningtemplate;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplatePageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 告警模板 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface WarningTemplateMapper extends BaseMapperX<WarningTemplateDO> {

    default PageResult<WarningTemplateDO> selectPage(WarningTemplatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarningTemplateDO>()
                .likeIfPresent(WarningTemplateDO::getName, reqVO.getName())
                .eqIfPresent(WarningTemplateDO::getCode, reqVO.getCode())
                .likeIfPresent(WarningTemplateDO::getTitle, reqVO.getTitle())
                .eq(WarningTemplateDO::getType, reqVO.getType())
                .betweenIfPresent(WarningTemplateDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarningTemplateDO::getId));
    }

    default WarningTemplateDO selectByCode(Integer type, String code) {
        return selectOne(WarningTemplateDO::getCode, code, WarningTemplateDO::getType, type);
    }


    @Select("SELECT DISTINCT t.code " +
            "FROM power_warning_template t " +
            "WHERE EXISTS ( " +
            "    SELECT 1 " +
            "    FROM power_warning_strategy s " +
            "    WHERE s.site_template_id = t.id OR s.mail_template_id = t.id " +
            "    AND s.site_template_id IN (#{ids}) OR s.mail_template_id IN (#{ids}) " +
            ") ")
    List<String> queryUsedByStrategy(@Param("ids") List<Long> ids);
}