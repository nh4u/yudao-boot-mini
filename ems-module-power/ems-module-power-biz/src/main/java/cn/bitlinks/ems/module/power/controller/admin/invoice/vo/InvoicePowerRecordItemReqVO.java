package cn.bitlinks.ems.module.power.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 发票电量记录明细 Request VO")
@Data
public class InvoicePowerRecordItemReqVO {

    @Schema(description = "表计编号（字典 invoice_meter_code）", example = "CUB-1AH03")
    private String meterCode;

    @Schema(description = "总电度(kWh)，可为空", example = "1234.56")
    private BigDecimal totalKwh;

    @Schema(description = "需量电度(kWh)，可为空", example = "234.56")
    private BigDecimal demandKwh;
}
