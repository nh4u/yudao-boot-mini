package cn.bitlinks.ems.module.power.dal.mysql.energygroup;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.energygroup.EnergyGroupDO;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.energygroup.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 能源分组 Mapper
 *
 * @author hero
 */
@Mapper
public interface EnergyGroupMapper extends BaseMapperX<EnergyGroupDO> {

    default PageResult<EnergyGroupDO> selectPage(EnergyGroupPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnergyGroupDO>()
                .likeIfPresent(EnergyGroupDO::getName, reqVO.getName())
                .betweenIfPresent(EnergyGroupDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(EnergyGroupDO::getId));
    }

    /**
     * 获取是否重复
     *
     * @param reqVO
     * @return
     */
    default EnergyGroupDO selectOne(EnergyGroupSaveReqVO reqVO) {
        return selectOne(new LambdaQueryWrapper<EnergyGroupDO>()
                .eq(StrUtil.isNotEmpty(reqVO.getName()), EnergyGroupDO::getName, reqVO.getName())
                .ne(!Objects.isNull(reqVO.getId()), EnergyGroupDO::getId, reqVO.getId())
                .last("limit 1"));
    }

    /**
     * 获取所有分组id
     *
     * @return List<Long>
     */
    List<Long> selectIds();

    /**
     * 带有是否可编辑状态的返回体
     *
     * @param ids 能源分组ids
     * @return
     */
    List<EnergyGroupRespVO> getEnergyGroups(@Param("ids") List<Long> ids);
}