package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import cn.bitlinks.ems.framework.common.util.collection.CollectionUtils;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "柱状图数据")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class RatioBarVO {

    /**
     * X轴数据
     */
    @Schema(description = "X轴数据")
    private List<String> XData;

    /**
     * Y轴环节对应的数据
     */
    @Schema(description = "Y轴数据")
    private List<RatioDataVO> YData;

    /**
     * 名称
     */
    @Schema(description = "名称")
    @ExcelProperty("名称")
    private String name;
}
