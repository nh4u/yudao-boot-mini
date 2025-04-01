package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PriceResultDTO {
    // 单价类型：1-固定价 2-分时价 3-阶梯价
    private Integer priceType;
    // 固定价格（当priceType=1时有效）
    private BigDecimal fixedPrice;
    // 分时价格（当priceType=2时有效）
    private Map<String, BigDecimal> timePrices;
    // 阶梯价格配置（当priceType=3时有效）
    private List<LadderPrice> ladderPrices;
    // 当前周期起始时间（当priceType=3时有效）
    private LocalDateTime periodStart;
    private BigDecimal price;

    @Data
    public static class LadderPrice {
        private BigDecimal min;
        private BigDecimal max;
        private BigDecimal price;
    }
}
