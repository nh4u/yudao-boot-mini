package cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 配置标签新增/修改 Request VO")
@Data
public class LabelConfigSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @NotEmpty(message = "标签名称不能为空")
    private String labelName;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "备注", requiredMode = Schema.RequiredMode.REQUIRED, example = "随便")
    private String remark;

    @Schema(description = "编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "编码不能为空")
    private String code;

    @Schema(description = "父标签ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "26722")
    @NotNull(message = "父标签ID不能为空")
    private Long parentId;

}