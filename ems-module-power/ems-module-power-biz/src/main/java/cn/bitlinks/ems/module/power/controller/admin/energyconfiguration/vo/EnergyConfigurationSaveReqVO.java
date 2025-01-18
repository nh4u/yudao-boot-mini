package cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 能源配置新增/修改 Request VO")
@Data
public class EnergyConfigurationSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "14490")
    private Long id;

    @Schema(description = "能源名称", example = "赵六")
    private String energyName;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "能源分类 1：外购能源；2：园区能源")
    private Integer energyClassify;

    @Schema(description = "能源图标")
    private String energyIcon;

    @Schema(description = "能源参数")
    private String energyParameter;

    @Schema(description = "折标煤系数")
    private BigDecimal factor;

    @Schema(description = "折标煤公式")
    private String coalFormula;

    @Schema(description = "折标煤小数位数")
    private String coalScale;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "计费方式")
    private Integer billingMethod;

    @Schema(description = "单价详细", example = "11713")
    private String unitPrice;

    @Schema(description = "用能成本公式")
    private String unitPriceFormula;

    @Schema(description = "单价小数位")
    private String unitPriceScale;

}