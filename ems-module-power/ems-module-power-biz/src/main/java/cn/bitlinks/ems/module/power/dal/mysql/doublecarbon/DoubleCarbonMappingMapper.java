package cn.bitlinks.ems.module.power.dal.mysql.doublecarbon;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.DoubleCarbonMappingPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon.DoubleCarbonMappingDO;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface DoubleCarbonMappingMapper extends BaseMapperX<DoubleCarbonMappingDO> {
    default PageResult<DoubleCarbonMappingDO> selectPage(DoubleCarbonMappingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DoubleCarbonMappingDO>()
                .likeIfPresent(DoubleCarbonMappingDO::getDoubleCarbonCode, reqVO.getStandingbookCode())
                .likeIfPresent(DoubleCarbonMappingDO::getStandingbookCode, reqVO.getStandingbookCode()));
    }

}