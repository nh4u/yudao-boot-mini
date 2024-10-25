package cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 单价历史新增/修改 Request VO")
@Data
public class UnitPriceHistorySaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "29507")
    private Long id;

    @Schema(description = "能源id", example = "8300")
    private Long energyId;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "计费方式")
    private Integer billingMethod;

    @Schema(description = "核算频率")
    private Integer accountingFrequency;

    @Schema(description = "单价详细")
    private String priceDetails;

    @Schema(description = "计算公式")
    private String formula;

}