package cn.bitlinks.ems.module.power.dal.mysql.doublecarbon;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.DoubleCarbonMappingPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon.DoubleCarbonMappingDO;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface DoubleCarbonMappingMapper extends BaseMapperX<DoubleCarbonMappingDO> {
    default PageResult<DoubleCarbonMappingDO> selectPage(DoubleCarbonMappingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapper<DoubleCarbonMappingDO>()
                .like(CharSequenceUtil.isNotBlank(reqVO.getStandingbookCode()), DoubleCarbonMappingDO::getStandingbookCode, reqVO.getStandingbookCode())
                .or()
                .like(CharSequenceUtil.isNotBlank(reqVO.getStandingbookCode()), DoubleCarbonMappingDO::getDoubleCarbonCode, reqVO.getStandingbookCode()));
    }

}