package cn.bitlinks.ems.module.power.dal.mysql.warningtemplate;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplatePageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import org.apache.ibatis.annotations.Mapper;

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
                .orderByDesc(WarningTemplateDO::getCreateTime));
    }

    default WarningTemplateDO selectByCode(Integer type, String code) {
        return selectOne(WarningTemplateDO::getCode, code, WarningTemplateDO::getType, type);
    }


}