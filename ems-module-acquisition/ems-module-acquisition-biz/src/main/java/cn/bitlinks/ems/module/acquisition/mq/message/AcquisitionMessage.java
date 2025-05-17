package cn.bitlinks.ems.module.acquisition.mq.message;

import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.module.acquisition.api.job.dto.ServiceSettingsDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据采集MQ消息
 */
@Data
public class AcquisitionMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 数采参数列表
     */
    private List<StandingbookAcquisitionDetailDTO> details;
    /**
     * 服务设置
     */
    private ServiceSettingsDTO serviceSettingsDTO;
    /**
     * 任务执行时间
     */
    private LocalDateTime jobTime;
}