package cn.bitlinks.ems.module.power.controller.admin.chemicals.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

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
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDateTime time;

    @Schema(description = "日期")
    private String strTime;

    @Schema(description = "金额")
    private BigDecimal price;
}