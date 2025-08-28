package cn.bitlinks.ems.module.power.dal.mysql.production;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.externalapi.ExternalApiDO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;


/**
 * @author liumingqiang
 */
@Mapper
public interface ProductionMapper extends BaseMapperX<ProductionDO> {

    default PageResult<ProductionDO> selectPage(ProductionPageReqVO reqVO) {
        LambdaQueryWrapper<ProductionDO> queryWrapper = new LambdaQueryWrapperX<ProductionDO>()
                .betweenIfPresent(ProductionDO::getTime, reqVO.getRange())
                .orderByAsc(ProductionDO::getCreateTime);
        return selectPage(reqVO, queryWrapper);
    }

}
