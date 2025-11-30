package cn.bitlinks.ems.module.power.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - 发票电量记录新增/修改 Request VO")
@Data
public class InvoicePowerRecordSaveReqVO {

    @Schema(description = "主键，新增时为空，修改时必填", example = "1")
    private Long id;

    @Schema(description = "补录月份（当月第一天）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-09-01")
    @NotNull(message = "补录月份不能为空")
    private LocalDate recordMonth;

    @Schema(description = "金额(含税13%)，可为空", example = "12345.67")
    private BigDecimal amount;

    @Schema(description = "备注", example = "补录 2025-09 发票电量")
    private String remark;

    @Schema(description = "明细列表（9 个表计）", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<InvoicePowerRecordItemReqVO> items;
}
