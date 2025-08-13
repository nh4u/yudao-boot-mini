package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 生产源耗 Response VO")
@Data
public class ProductionConsumptionSettingsRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "统计项名称")
    private String name;

    @Schema(description = "台账id")
    private Long standingbookId;


}