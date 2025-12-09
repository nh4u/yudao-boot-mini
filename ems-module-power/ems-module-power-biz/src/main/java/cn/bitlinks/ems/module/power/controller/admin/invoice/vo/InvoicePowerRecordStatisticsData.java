package cn.bitlinks.ems.module.power.controller.admin.invoice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 发票电量记录按月统计数据 VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class InvoicePowerRecordStatisticsData {

    @Schema(description = "月份，例如 2025年09月")
    private String date;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "总电度")
    private BigDecimal totalKwh;

    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "需量电度")
    private BigDecimal demandKwh;

    /**
     * 金额行用到；普通电表行这里为 null
     */
    @JsonSerialize(using = ToStringSerializer.class)
    @Schema(description = "金额（仅在“金额”行使用）")
    private BigDecimal amount;
}

