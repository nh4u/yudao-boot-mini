package cn.bitlinks.ems.module.power.controller.admin.chemicals.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 化学品设置 Response VO")
@Data
@ExcelIgnoreUnannotated
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PowerChemicalsSettingsRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    private Long id;

    @Schema(description = "类型", example = "随便")
    private String code;

    @Schema(description = "日期")
    private LocalDateTime time;

    @Schema(description = "日期")
    private String strTime;

    @Schema(description = "金额")
    private BigDecimal price;
}