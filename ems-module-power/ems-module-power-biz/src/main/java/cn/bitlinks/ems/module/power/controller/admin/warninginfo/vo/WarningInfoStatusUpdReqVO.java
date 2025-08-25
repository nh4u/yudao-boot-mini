package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 告警信息修改状态 Request VO")
@Data
public class WarningInfoStatusUpdReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6410")
    private Long id;

    @Schema(description = "处理状态:0-未处理1-处理中2-已处理", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "处理状态:0-未处理1-处理中2-已处理不能为空")
    private Integer status;

    @Schema(description = "处理意见（非必填，≤500字）", example = "已完成检查")
    @Size(max = 500, message = "处理意见不能超过500字")
    private String handleOpinion;

}