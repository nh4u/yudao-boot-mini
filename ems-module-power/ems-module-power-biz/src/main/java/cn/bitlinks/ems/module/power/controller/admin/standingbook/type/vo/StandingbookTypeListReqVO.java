package cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 台账类型列表 Request VO")
@Data
public class StandingbookTypeListReqVO {

    @Schema(description = "名字", example = "bitlinks")
    private String name;

    @Schema(description = "父级类型编号", example = "31064")
    private Long superId;

    @Schema(description = "父级名字", example = "bitlinks")
    private String superName;

    @Schema(description = "类型", example = "2")
    private String topType;

    @Schema(description = "排序")
    private Long sort;

    @Schema(description = "当前层级")
    private Long level;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "简介", example = "你猜")
    private String description;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
