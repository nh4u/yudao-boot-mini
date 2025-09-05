package cn.bitlinks.ems.module.power.controller.admin.minitor.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
@ExcelIgnoreUnannotated
@ColumnWidth(25)
public class MinitorDetailData {

    @Schema(description = "时间")
    @ExcelProperty("时间")
    String time;

    @Schema(description = "参数值")
    @ExcelProperty("参数值")
    private BigDecimal value;


}
