package cn.bitlinks.ems.module.power.dal.mysql.additionalrecording;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 补录 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface AdditionalRecordingMapper extends BaseMapperX<AdditionalRecordingDO> {

    default PageResult<AdditionalRecordingDO> selectPage(AdditionalRecordingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AdditionalRecordingDO>()
                .eqIfPresent(AdditionalRecordingDO::getVoucherId, reqVO.getVoucherId())
                .eqIfPresent(AdditionalRecordingDO::getStandingbookId, reqVO.getStandingbookId())
                .eqIfPresent(AdditionalRecordingDO::getValueType, reqVO.getValueType())
                .betweenIfPresent(AdditionalRecordingDO::getLastCollectTime, reqVO.getLastCollectTime())
                .eqIfPresent(AdditionalRecordingDO::getLastValue, reqVO.getLastValue())
                .betweenIfPresent(AdditionalRecordingDO::getThisCollectTime, reqVO.getThisCollectTime())
                .eqIfPresent(AdditionalRecordingDO::getThisValue, reqVO.getThisValue())
                .eqIfPresent(AdditionalRecordingDO::getRecordPerson, reqVO.getRecordPerson())
                .eqIfPresent(AdditionalRecordingDO::getRecordReason, reqVO.getRecordReason())
                .eqIfPresent(AdditionalRecordingDO::getRecordMethod, reqVO.getRecordMethod())
                .betweenIfPresent(AdditionalRecordingDO::getEnterTime, reqVO.getEnterTime())
                .betweenIfPresent(AdditionalRecordingDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AdditionalRecordingDO::getId));
    }

    boolean existsByVoucherId(@Param("voucherId") Long voucherId);

}