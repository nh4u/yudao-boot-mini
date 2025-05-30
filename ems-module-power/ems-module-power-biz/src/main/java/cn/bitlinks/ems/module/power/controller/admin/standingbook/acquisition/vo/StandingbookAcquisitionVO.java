package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 台账-数采设置 VO")
@Data
public class StandingbookAcquisitionVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "台账id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long standingbookId;

    @Schema(description = "设备数采启停开关（0：关；1开。）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean status;

    @Schema(description = "采集频率", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long frequency;

    @Schema(description = "采集频率单位(秒、分钟、小时、天)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer frequencyUnit;

    @Schema(description = "服务设置id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long serviceSettingsId;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @Schema(description = "设备参数配置详细", requiredMode = Schema.RequiredMode.REQUIRED)
    List<StandingbookAcquisitionDetailVO> details;

}