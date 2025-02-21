package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 告警策略批量修改状态 Request VO")
@Data
public class WarningStrategyBatchUpdIntervalReqVO {

    @Schema(description = "编号s", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;


    @Schema(description = "告警间隔", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "告警间隔不能为空")
    private String interval;

    @Schema(description = "告警间隔单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "告警间隔单位不能为空")
    private Integer intervalUnit;
}