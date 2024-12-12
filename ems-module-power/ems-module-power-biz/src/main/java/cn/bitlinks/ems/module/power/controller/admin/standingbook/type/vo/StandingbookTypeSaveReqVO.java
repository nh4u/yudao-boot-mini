package cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 台账类型新增/修改 Request VO")
@Data
public class StandingbookTypeSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13914")
    private Long id;

    @Schema(description = "名字", requiredMode = Schema.RequiredMode.REQUIRED, example = "bitlinks")
    private String name;

    @Schema(description = "父级类型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "31064")
    private Long superId;


    @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private String topType;

    @Schema(description = "排序")
    private Long sort;

    @Schema(description = "当前层级", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long level;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "简介", example = "你猜")
    private String description;

}
