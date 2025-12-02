package cn.bitlinks.ems.module.power.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - 发票电量记录列表查询 Request VO")
@Data
public class InvoicePowerRecordPageReqVO {

    @Schema(description = "补录月份区间 [开始, 结束]", example = "[2025-09-01, 2025-12-01]")
    private LocalDate[] recordMonth;
}
