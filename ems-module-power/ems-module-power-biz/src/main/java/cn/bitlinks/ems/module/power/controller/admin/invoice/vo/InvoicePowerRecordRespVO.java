package cn.bitlinks.ems.module.power.controller.admin.invoice.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 发票电量记录详情 Response VO")
@Data
public class InvoicePowerRecordRespVO {

    @Schema(description = "主键", example = "1")
    private Long id;

    @Schema(description = "补录月份", example = "2025-09-01")
    private LocalDate recordMonth;

    @Schema(
            description = "金额(含税13%)",
            type = "string",
            example = "12345.67"
    )
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;

    @Schema(
            description = "平均电价 = 金额 ÷ 总电度之和，金额为空或总电度为 0 时为空",
            type = "string",
            example = "1.2345"
    )
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal avgPrice;

    @Schema(description = "明细列表")
    private List<InvoicePowerRecordItemRespVO> items;
}
