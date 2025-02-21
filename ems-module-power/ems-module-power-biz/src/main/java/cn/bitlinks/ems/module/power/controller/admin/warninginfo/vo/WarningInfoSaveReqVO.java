package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 告警信息新增/修改 Request VO")
@Data
public class WarningInfoSaveReqVO {

    @Schema(description = "编号", example = "6410")
    private Long id;

    @Schema(description = "用户id", requiredMode = Schema.RequiredMode.REQUIRED, example = "30582")
    @NotNull(message = "用户id不能为空")
    private Long userId;

    @Schema(description = "告警等级：紧急4 重要3 次要2 警告1 提示0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "告警等级：紧急4 重要3 次要2 警告1 提示0不能为空")
    private Integer level;

    @Schema(description = "告警时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "告警时间不能为空")
    private LocalDateTime warningTime;

    @Schema(description = "处理状态:0-未处理1-处理中2-已处理", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "处理状态:0-未处理1-处理中2-已处理不能为空")
    private Integer status;

    @Schema(description = "设备名称与编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "设备名称与编号不能为空")
    private String deviceRel;

    @Schema(description = "模板id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3996")
    @NotNull(message = "模板id不能为空")
    private Long templateId;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "内容不能为空")
    private String content;

}