package cn.bitlinks.ems.framework.common.pojo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月15日 10:58
 */
@Data
public class StatsResult {
    private BigDecimal sum;
    private BigDecimal avg;
    private BigDecimal max;
    private BigDecimal min;

}
