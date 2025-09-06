package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 补录导出 Response VO")
@Data
@ExcelIgnoreUnannotated
@ColumnWidth(20)
public class AdditionalRecordingExportRespVO {

    @Schema(description = "计量器具id", example = "21597")
    @ExcelProperty("计量器具编号")
    private String standingbookCode;

    @Schema(description = "计量器具名称", example = "21597")
    @ExcelProperty("计量器具名称")
    private String standingbookName;

    @Schema(description = "数据值")
    @ExcelProperty("数据值")
    private BigDecimal thisValue;

    @Schema(description = "采集时间")
    @ExcelProperty("采集时间")
    private LocalDateTime thisCollectTime;

}