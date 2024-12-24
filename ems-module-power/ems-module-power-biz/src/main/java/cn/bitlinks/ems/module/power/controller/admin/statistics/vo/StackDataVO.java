package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "堆叠图Y轴数据")
@Data
@Builder
public class StackDataVO {
    /**
     * 名称
     */
    @Schema(description = "名称")
    @ExcelProperty("名称")
    private String name;

    /**
     * 对应的数据
     */
    @Schema(description = "对应的数据")
    @ExcelProperty("对应的数据")
    private List<BigDecimal> data;
}
