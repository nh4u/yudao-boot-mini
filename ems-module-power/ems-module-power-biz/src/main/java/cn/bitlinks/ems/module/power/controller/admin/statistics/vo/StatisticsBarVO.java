package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import cn.bitlinks.ems.framework.common.util.collection.CollectionUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "柱状图数据")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsBarVO {

    /**
     * X轴数据
     */
    @Schema(description = "X轴数据")
    private List<String> XData;

    /**
     * Y轴环节对应的数据
     */
    @Schema(description = "Y轴数据")
    private List<BigDecimal> YData;

    /**
     * 数据更新时间
     */
    @Schema(description = "数据更新时间")
    private LocalDateTime dataTime;

    public List<BigDecimal> getYData() {
        return CollectionUtils.convertList(YData, e -> e.setScale(2, RoundingMode.HALF_UP));
    }
}
