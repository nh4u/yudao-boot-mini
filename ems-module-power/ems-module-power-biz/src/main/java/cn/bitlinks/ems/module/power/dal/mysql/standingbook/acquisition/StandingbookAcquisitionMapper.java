package cn.bitlinks.ems.module.power.dal.mysql.standingbook.acquisition;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition.StandingbookAcquisitionDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 台账-数采设置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookAcquisitionMapper extends BaseMapperX<StandingbookAcquisitionDO> {

    default PageResult<StandingbookAcquisitionDO> selectPage(StandingbookAcquisitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StandingbookAcquisitionDO>()
                .eqIfPresent(StandingbookAcquisitionDO::getStatus, reqVO.getStatus())
                .eqIfPresent(StandingbookAcquisitionDO::getStandingbookId, reqVO.getStandingbookId())
                .eqIfPresent(StandingbookAcquisitionDO::getFrequency, reqVO.getFrequency())
                .eqIfPresent(StandingbookAcquisitionDO::getFrequencyUnit, reqVO.getFrequencyUnit())
                .eqIfPresent(StandingbookAcquisitionDO::getServiceSettingsId, reqVO.getServiceSettingsId())
                .betweenIfPresent(StandingbookAcquisitionDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(StandingbookAcquisitionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StandingbookAcquisitionDO::getId));
    }

}