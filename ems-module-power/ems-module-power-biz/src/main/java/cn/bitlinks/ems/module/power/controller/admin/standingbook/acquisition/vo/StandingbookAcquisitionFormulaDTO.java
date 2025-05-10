package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 台账-数采设置公式 VO")
@Data
public class StandingbookAcquisitionFormulaDTO {
    @Schema(description = "公式参数编码")
    private String code;
    @Schema(description = "公式参数类型")
    private Integer formulaType;
}
