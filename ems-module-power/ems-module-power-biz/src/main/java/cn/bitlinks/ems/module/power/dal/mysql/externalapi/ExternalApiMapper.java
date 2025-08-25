package cn.bitlinks.ems.module.power.dal.mysql.externalapi;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.externalapi.ExternalApiDO;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * @author liumingqiang
 */
@Mapper
public interface ExternalApiMapper extends BaseMapperX<ExternalApiDO> {

    default PageResult<ExternalApiDO> selectPage(ExternalApiPageReqVO reqVO) {
        LambdaQueryWrapper<ExternalApiDO> queryWrapper = new LambdaQueryWrapper<ExternalApiDO>()
                .eq(CharSequenceUtil.isNotBlank(reqVO.getName()), ExternalApiDO::getName, reqVO.getName())
                .orderByAsc(ExternalApiDO::getCreateTime);
        return selectPage(reqVO, queryWrapper);
    }

}
