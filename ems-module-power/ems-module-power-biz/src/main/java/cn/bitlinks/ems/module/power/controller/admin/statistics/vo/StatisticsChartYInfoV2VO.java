package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

/**
 * @author wangl
 * @date 2025年05月15日 10:57
 */

import com.alibaba.excel.annotation.ExcelProperty;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月14日 15:08
 */
@Schema(description = "堆叠图Y轴数据")
@Data
public class StatisticsChartYInfoV2VO {
    /**
     * 元素id
     */
    @Schema(description = "元素id")
    @ExcelProperty("元素id")
    private Long id;

    /**
     * 元素名称
     */
    @Schema(description = "元素名称")
    @ExcelProperty("元素名称")
    private String name;

    /**
     * 对应的数据
     */
    @Schema(description = "对应的数据")
    @ExcelProperty("对应的数据")
    private List<StatisticsChartYDataV2VO> data;
}
