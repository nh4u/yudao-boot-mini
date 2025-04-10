package cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 能源参数分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnergyParametersPageReqVO extends PageParam {

    @Schema(description = "能源id", example = "10576")
    private Long energyId;

    @Schema(description = "中文名")
    private String chinese;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "数据特征值")
    private Integer characteristic;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "数据类型", example = "2")
    private Integer type;

    @Schema(description = "对应数采参数")
    private String acquisition;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}