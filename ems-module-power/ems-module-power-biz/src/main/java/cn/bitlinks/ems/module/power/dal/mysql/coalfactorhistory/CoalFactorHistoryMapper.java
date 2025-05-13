package cn.bitlinks.ems.module.power.dal.mysql.coalfactorhistory;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 折标煤系数历史 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface CoalFactorHistoryMapper extends BaseMapperX<CoalFactorHistoryDO> {

    default PageResult<CoalFactorHistoryDO> selectPage(CoalFactorHistoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CoalFactorHistoryDO>()
                .eqIfPresent(CoalFactorHistoryDO::getEnergyId, reqVO.getEnergyId())
                .betweenIfPresent(CoalFactorHistoryDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(CoalFactorHistoryDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(CoalFactorHistoryDO::getFactor, reqVO.getFactor())
                .eqIfPresent(CoalFactorHistoryDO::getFormula, reqVO.getFormula())
                .eqIfPresent(CoalFactorHistoryDO::getUpdater,reqVO.getUpdater())
                .betweenIfPresent(CoalFactorHistoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CoalFactorHistoryDO::getId));
    }

    CoalFactorHistoryDO findLatestByEnergyId(Long energyId);

    void updateEndTime(CoalFactorHistoryDO coalFactorHistory);

    /**
     * 根据能源ID查询当前生效的折标煤系数
     * @param energyId 能源ID
     * @return 折标煤系数历史记录
     */
    CoalFactorHistoryDO selectCurrentByEnergyId(@Param("energyId") Long energyId);

}