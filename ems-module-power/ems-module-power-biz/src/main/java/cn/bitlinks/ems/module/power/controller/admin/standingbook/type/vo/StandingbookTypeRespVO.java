package cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 台账类型 Response VO")
@Data
@ExcelIgnoreUnannotated
public class StandingbookTypeRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "13914")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "名字", requiredMode = Schema.RequiredMode.REQUIRED, example = "bitlinks")
    @ExcelProperty("名字")
    private String name;

    @Schema(description = "父级类型编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "31064")
    @ExcelProperty("父级类型编号")
    private Long superId;


    @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("类型")
    private String topType;

    @Schema(description = "排序")
    @ExcelProperty("排序")
    private Long sort;

    @Schema(description = "当前层级", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("当前层级")
    private Long level;

    @Schema(description = "编码")
    @ExcelProperty("编码")
    private String code;

    @Schema(description = "简介", example = "你猜")
    @ExcelProperty("简介")
    private String description;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime updateTime;
    List<StandingbookTypeRespVO> children = new ArrayList<>();
}
