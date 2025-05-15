package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 堆叠图 Y 轴单个序列数据项 VO（用于 now / previous / ratio）
 */
@Schema(description = "堆叠图Y轴序列数据项")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChartSeriesItemVO {

    /**
     * 序列名称（如 now、previous、ratio）
     */
    @Schema(description = "序列名称")
    @ExcelProperty("序列名称")
    private String name;

    /**
     * 图表类型（bar 或 line）
     */
    @Schema(description = "图表类型")
    @ExcelProperty("图表类型")
    private String type;

    /**
     * 数据值
     */
    @Schema(description = "序列数据")
    @ExcelProperty("序列数据")
    private List<BigDecimal> data;

    /**
     * Y 轴索引
     */
    @Schema(description = "Y轴索引")
    @ExcelProperty("Y轴索引")
    private Integer yaxisIndex;
}
