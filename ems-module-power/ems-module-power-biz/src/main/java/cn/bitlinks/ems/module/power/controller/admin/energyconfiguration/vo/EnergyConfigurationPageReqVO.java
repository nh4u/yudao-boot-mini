package cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo;

import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 能源配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnergyConfigurationPageReqVO extends PageParam {

    @Schema(description = "分組id", example = "26887")
    private Long groupId;

    @Schema(description = "分组名称", example = "王五")
    private String groupName;

    @Schema(description = "能源名称", example = "赵六")
    private String energyName;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "能源分类 1：外购能源；2：园区能源")
    private Integer energyClassify;

    @Schema(description = "能源参数")
    private List<EnergyParametersDO> energyParameters;

    @Schema(description = "折标煤系数")
    private BigDecimal factor;

    @Schema(description = "折标煤公式")
    private String coalFormula;

    @Schema(description = "折标煤小数位数")
    private String coalScale;

    @Schema(description = "开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] startTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] endTime;

    @Schema(description = "计费方式")
    private Integer billingMethod;

    @Schema(description = "核算频率")
    private Integer accountingFrequency;

    @Schema(description = "单价详细", example = "11713")
    private UnitPriceConfigurationDO unitPrice;

    @Schema(description = "用能成本公式")
    private String unitPriceFormula;

    @Schema(description = "单价小数位")
    private String unitPriceScale;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "统计能源", example = "[1,2,3,4]")
    private List<Long> energyIds;

}