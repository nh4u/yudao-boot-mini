package cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 配置标签分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LabelConfigPageReqVO extends PageParam {

    @Schema(description = "标签名称", example = "赵六")
    private String labelName;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "父标签ID", example = "26722")
    private Long parentId;

}