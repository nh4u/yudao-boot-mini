package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import cn.bitlinks.ems.framework.common.util.collection.CollectionUtils;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "堆叠图Y轴数据")
@Data
@Builder
public class RatioDataVO {

    /**
     * 名称
     */
    @Schema(description = "yAxisIndex", example = "1:Y轴")
    @ExcelProperty("yAxisIndex")
    private Integer yAxisIndex;

    /**
     * 名称
     */
    @Schema(description = "名称")
    @ExcelProperty("名称")
    private String name;

    /**
     * 类型
     */
    @Schema(description = "类型", example = "bar/line")
    @ExcelProperty("类型")
    private String type;

    /**
     * 对应的数据
     */
    @Schema(description = "对应的数据")
    @ExcelProperty("对应的数据")
    private List<BigDecimal> data;

    public List<BigDecimal> getData() {
        return CollectionUtils.convertList(data, e -> e.setScale(2, RoundingMode.HALF_UP));
    }
}
