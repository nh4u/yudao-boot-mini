package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 台账-数采设置新增/修改 Request VO")
@Data
public class StandingbookAcquisitionSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "14955")
    private Long id;

    @Schema(description = "设备数采启停开关（0：关；1开。）", example = "1")
    private Boolean status;

    @Schema(description = "台账id", example = "4669")
    private Long standingbookId;

    @Schema(description = "采集频率")
    private Long frequency;

    @Schema(description = "采集频率单位(秒、分钟、小时、天)")
    private Integer frequencyUnit;

    @Schema(description = "服务设置id", requiredMode = Schema.RequiredMode.REQUIRED, example = "22393")
    @NotNull(message = "服务设置id不能为空")
    private Long serviceSettingsId;

    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

}