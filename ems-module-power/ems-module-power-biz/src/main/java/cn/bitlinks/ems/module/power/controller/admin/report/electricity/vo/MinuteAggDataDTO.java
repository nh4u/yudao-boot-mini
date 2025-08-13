package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class MinuteAggDataDTO {
    private String time;
    private BigDecimal fullValue;
    private Long standingbookId;
    private String paramCode;

}
