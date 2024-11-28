package cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 台账属性新增/修改 Request VO")
@Data
public class StandingbookAttributeSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "14000")
    private Long id;

    @Schema(description = "属性名字", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
//    @NotEmpty(message = "属性名字不能为空")
    private String name;

    @Schema(description = "属性值")
    private String value;

    @Schema(description = "类型编号", example = "16688")
    private Long typeId;

    @Schema(description = "台账编号", example = "28937")
    private Long standingbookId;

    @Schema(description = "文件编号", example = "28264")
    private Long fileId;

    @Schema(description = "是否必填", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "是否必填不能为空")
    private String isRequired;

    @Schema(description = "编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "编码不能为空")
    private String code;

    @Schema(description = "排序")
    private Long sort;

    @NotEmpty(message = "格式不能为空")
    @Schema(description = "格式")
    private String format;

    @Schema(description = "归属节点")
    private String node;

    @Schema(description = "下拉框选项")
    private String options;

    @Schema(description = "简介", example = "你说的对")
    private String description;

}
