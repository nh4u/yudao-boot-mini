package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 补录 Service 接口
 *
 * @author bitlinks
 */
public interface AdditionalRecordingService {

    /**
     * 创建补录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    void createAdditionalRecording(@Valid AdditionalRecordingManualSaveReqVO createReqVO);

    AdditionalRecordingExistAcqDataRespVO getExistDataRange(Long standingbookId, LocalDateTime currentCollectTime);

    /**
     * 批量创建补录操作记录
     *
     * @param minuteAggAcqDataList
     */
    void saveAdditionalRecordingBatch(List<MinuteAggregateDataDTO> minuteAggAcqDataList);

    List<Long> createAdditionalRecordingByVoucherId(List<Long> VoucherIds, Long standingbookId);

    List<Long> getVoucherIdsByStandingbookId(Long standingbookId);

    /**
     * 更新补录
     *
     * @param updateReqVO 更新信息
     */
    void updateAdditionalRecording(@Valid AdditionalRecordingSaveReqVO updateReqVO);

    /**
     * 批量删除补录
     *
     * @param ids 编号
     */
    void deleteAdditionalRecordings(List<Long> ids);


    /**
     * 删除补录
     *
     * @param id 编号
     */
    void deleteAdditionalRecording(Long id);

    /**
     * 获得补录
     *
     * @param id 编号
     * @return 补录
     */
    AdditionalRecordingDO getAdditionalRecording(Long id);

    /**
     * 获得补录分页
     *
     * @param pageReqVO 分页查询
     * @return 补录分页
     */
    PageResult<AdditionalRecordingDO> getAdditionalRecordingPage(AdditionalRecordingPageReqVO pageReqVO);

    List<AdditionalRecordingDO> selectByCondition(
            BigDecimal minThisValue, BigDecimal maxThisValue,
            String recordPerson,
            Integer recordMethod,
            LocalDateTime startThisCollectTime, LocalDateTime endThisCollectTime,
            LocalDateTime startEnterTime, LocalDateTime endEnterTime);


    List<AdditionalRecordingExportRespVO> getAdditionalRecordingList(Map<String, String> pageReqVO);
}