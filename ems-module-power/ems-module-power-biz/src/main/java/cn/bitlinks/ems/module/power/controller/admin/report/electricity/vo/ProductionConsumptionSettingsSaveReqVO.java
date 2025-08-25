package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 生产源耗新增/修改 Request VO")
@Data
public class ProductionConsumptionSettingsSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "统计项名称")
    @NotEmpty(message = "统计项名称不能为空")
    private String name;

    @Schema(description = "台账id")
    private Long standingbookId;


}