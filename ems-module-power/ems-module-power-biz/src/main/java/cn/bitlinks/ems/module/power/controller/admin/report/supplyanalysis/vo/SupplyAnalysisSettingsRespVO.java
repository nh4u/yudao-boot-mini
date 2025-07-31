package cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 供应分析 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SupplyAnalysisSettingsRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "系统")
    private String system;

    @Schema(description = "分析项")
    private String item;

    @Schema(description = "台账id")
    private Long standingbookId;


}