package cn.bitlinks.ems.module.acquisition.api.job.dto;

import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "RPC 服务 - 任务创建")
@Data
public class AcquisitionJobDTO {
    /**
     * 台账id
     */
    @NotNull(message = "台账id不能为空")
    private Long standingbookId;
    /**
     * 设备采集启停开关
     */
    private Boolean status;
    /**
     * 任务开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime jobStartTime;

    /**
     * cron表达式
     */
    @NotEmpty(message = "cron表达式不能为空")
    private String cronExpression;
    /**
     * 详情
     */
    @NotNull(message = "数采参数列表不能为空")
    private List<StandingbookAcquisitionDetailDTO> details;
    /**
     * 服务设置
     */
    @NotNull(message = "服务设置不能为空")
    private ServiceSettingsDTO serviceSettingsDTO;
}



