package cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalTime;
import java.util.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 单价详细新增/修改 Request VO")
@Data
public class PriceDetailSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "705")
    private Long id;

    @Schema(description = "单价id", example = "9614")
    private Long priceId;

    @Schema(description = "时段类型", example = "1")
    private Integer periodType;

    @Schema(description = "时段开始时间")
    private LocalTime periodStart;

    @Schema(description = "时段结束时间")
    private LocalTime periodEnd;

    @Schema(description = "档位用量下限")
    private BigDecimal usageMin;

    @Schema(description = "档位用量上限")
    private BigDecimal usageMax;

    @Schema(description = "单价", example = "4236")
    private BigDecimal unitPrice;

}