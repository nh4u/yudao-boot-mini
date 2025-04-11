package cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Schema(description = "管理后台 - 单价详细新增/修改 Request VO")
@Data
public class PriceDetailSaveReqVO {

//    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "705")
//    private Long id;

//    @Schema(description = "单价id", example = "9614")
//    private Long priceId;

    @Schema(description = "时段类型（分时段计价）", example = "1")
    private Integer periodType;

    @Schema(description = "时段开始时间（分时段计价）")
    private LocalTime periodStart;

    @Schema(description = "时段结束时间（分时段计价）")
    private LocalTime periodEnd;

    @Schema(description = "档位用量下限（阶梯计价）")
    private BigDecimal usageMin;

    @Schema(description = "档位用量上限（阶梯计价）")
    private BigDecimal usageMax;

    @Schema(description = "单价（阶梯计价、分时段计价、统一计价）", example = "4236")
    private BigDecimal unitPrice;

}