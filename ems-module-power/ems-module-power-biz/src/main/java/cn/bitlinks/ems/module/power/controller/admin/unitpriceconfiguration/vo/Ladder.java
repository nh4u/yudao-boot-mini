package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class Ladder {

    private BigDecimal min;
    private BigDecimal max; // null表示无上限
    private BigDecimal price;

}
