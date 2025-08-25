package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 告警信息批量处理 Request VO")
@Data
public class WarningInfoStatusBatchUpdReqVO {

    @Schema(description = "待处理的告警ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "ID 列表不能为空")
    private List<Long> ids;

    @Schema(description = "目标处理状态:0-未处理1-处理中2-已处理", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "处理状态不能为空")
    private Integer status;

    @Schema(description = "处理意见（非必填，≤500字）")
    @Size(max = 500, message = "处理意见不能超过500字")
    private String handleOpinion;
}