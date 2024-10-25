package cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 台账属性分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StandingbookAttributePageReqVO extends PageParam {

    @Schema(description = "属性名字", example = "李四")
    private String name;

    @Schema(description = "属性值")
    private String value;

    @Schema(description = "类型编号", example = "16688")
    private Long typeId;

    @Schema(description = "台账编号", example = "28937")
    private Long standingbookId;

    @Schema(description = "文件编号", example = "28264")
    private Long fileId;

    @Schema(description = "是否必填")
    private String isRequired;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "排序")
    private Long sort;

    @Schema(description = "格式")
    private String format;

    @Schema(description = "归属节点")
    private String node;

    @Schema(description = "下拉框选项")
    private String options;

    @Schema(description = "简介", example = "你说的对")
    private String description;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
