package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimplePriceResult {
    private BigDecimal price;
}
