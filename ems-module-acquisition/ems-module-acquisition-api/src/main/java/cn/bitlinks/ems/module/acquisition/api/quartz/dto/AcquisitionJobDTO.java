package cn.bitlinks.ems.module.acquisition.api.quartz.dto;

import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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
     * 采集频率
     */
    @NotNull(message = "采集频率不能为空")
    private Long frequency;

    /**
     * 采集频率单位
     */
    @NotNull(message = "采集频率单位不能为空")
    private Integer frequencyUnit;

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



