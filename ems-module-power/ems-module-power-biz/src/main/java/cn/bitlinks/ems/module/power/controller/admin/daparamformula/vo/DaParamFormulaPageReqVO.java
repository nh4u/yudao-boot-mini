package cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 数据来源为关联计量器具时的参数公式分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DaParamFormulaPageReqVO extends PageParam {

    @Schema(description = "能源id", example = "13897")
    private Long energyId;

    @Schema(description = "能源参数计算公式")
    private String energyFormula;

    @Schema(description = "公式类型")
    private Integer formulaType;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}