package cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 告警模板新增/修改 Request VO")
@Data
public class WarningTemplateSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "16957")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @NotEmpty(message = "模板名称不能为空")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模板编码不能为空")
    private String code;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模板内容不能为空")
    private String content;

    @Schema(description = "模板标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "模板标题不能为空")
    private String title;

//    @Schema(description = "标题参数数组", requiredMode = Schema.RequiredMode.REQUIRED)
//    @NotEmpty(message = "标题参数数组不能为空")
//    private String tParams;
//
//    @Schema(description = "内容参数数组", requiredMode = Schema.RequiredMode.REQUIRED)
//    @NotEmpty(message = "内容参数数组不能为空")
//    private String params;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "模板类型:0-站内信1-邮件", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "模板类型:0-站内信1-邮件不能为空")
    private Integer type;

}