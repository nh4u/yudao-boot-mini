package cn.bitlinks.ems.module.power.service.additionalrecording;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import javax.validation.*;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;

/**
 * 补录 Service 接口
 *
 * @author bitlinks
 */
public interface AdditionalRecordingService {

    /**
     * 创建补录
     *
     * @param createReqVOs 创建信息
     * @return 编号
     */
    List<Long> createAdditionalRecording(@Valid List<AdditionalRecordingSaveReqVO> createReqVOs);

    AdditionalRecordingLastVO getLastRecord(Long standingbookId, LocalDateTime currentCollectTime);

    /**
     * 更新补录
     *
     * @param updateReqVO 更新信息
     */
    void updateAdditionalRecording(@Valid AdditionalRecordingSaveReqVO updateReqVO);

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

}