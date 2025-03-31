package cn.bitlinks.ems.module.power.dal.mysql.labelconfig;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配置标签 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface LabelConfigMapper extends BaseMapperX<LabelConfigDO> {

    default PageResult<LabelConfigDO> selectPage(LabelConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<LabelConfigDO>()
                .likeIfPresent(LabelConfigDO::getLabelName, reqVO.getLabelName())
                .eqIfPresent(LabelConfigDO::getSort, reqVO.getSort())
                .eqIfPresent(LabelConfigDO::getRemark, reqVO.getRemark())
                .eqIfPresent(LabelConfigDO::getCode, reqVO.getCode())
                .eqIfPresent(LabelConfigDO::getParentId, reqVO.getParentId())
                .eqIfPresent(LabelConfigDO::getIfDefault, reqVO.getIfDefault())
                .orderByDesc(LabelConfigDO::getId));
    }


}