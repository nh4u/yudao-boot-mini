package cn.bitlinks.ems.module.acquisition.mq.message;

import cn.bitlinks.ems.module.acquisition.api.job.dto.StandingbookAcquisitionDetailDTO;
import lombok.Data;

import java.io.Serializable;
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
    private String standingbookId;
    /**
     * 数采参数列表
     */
    private List<StandingbookAcquisitionDetailDTO> details;
}