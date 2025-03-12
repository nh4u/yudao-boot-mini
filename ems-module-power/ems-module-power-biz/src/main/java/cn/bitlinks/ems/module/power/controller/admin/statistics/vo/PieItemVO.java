package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class PieItemVO {
    private String name;
    private BigDecimal value;
    private BigDecimal proportion;

    public PieItemVO(String name, BigDecimal value, BigDecimal proportion) {
        this.name = name;
        this.value = value;
        this.proportion = proportion;
    }
}
