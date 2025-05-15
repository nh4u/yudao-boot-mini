package cn.bitlinks.ems.module.acquisition.api.job.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "RPC 服务 - 任务创建")
@Data
public class AcquisitionJobDTO {
    /**
     * 台账id
     */
    private Long standingbookId;

    /**
     * 任务开始时间
     */
    private LocalDateTime jobStartTime;

    /**
     * corn表达式
     */
    private String cornExpression;
    /**
     * 详情
     */
    List<StandingbookAcquisitionDetailDTO> details;
}



