package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PieChartVO {
    private String name;
    private List<PieItemVO> data;
    private BigDecimal total;

    public PieChartVO(String name, List<PieItemVO> data, BigDecimal total) {
        this.name = name;
        this.data = data;
        this.total = total;
    }
}
