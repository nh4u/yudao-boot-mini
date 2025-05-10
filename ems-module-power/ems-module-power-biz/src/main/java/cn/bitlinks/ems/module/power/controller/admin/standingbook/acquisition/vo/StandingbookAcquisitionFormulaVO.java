package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 台账-数采设置公式 VO")
@Data
public class StandingbookAcquisitionFormulaVO {

    @Schema(description = "公式")
    private String currentFormula;
    @Schema(description = "公式参数编码")
    private List<String> otherFormula;
}
