package cn.bitlinks.ems.framework.common.pojo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月14日 16:11
 */
@Data
public class StatsResult {
    private BigDecimal sum;
    private BigDecimal avg;
    private BigDecimal max;
    private BigDecimal min;

}
