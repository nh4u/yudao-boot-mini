package cn.bitlinks.ems.module.power.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 发票电量记录明细 Response VO")
@Data
public class InvoicePowerRecordItemRespVO {

    @Schema(description = "表计编号", example = "CUB-1AH03")
    private String meterCode;

    @Schema(
            description = "总电度(kWh)",
            type = "string",
            example = "1234.56"
    )
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal totalKwh;

    @Schema(
            description = "需量电度(kWh)",
            type = "string",
            example = "234.56"
    )
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal demandKwh;
}
