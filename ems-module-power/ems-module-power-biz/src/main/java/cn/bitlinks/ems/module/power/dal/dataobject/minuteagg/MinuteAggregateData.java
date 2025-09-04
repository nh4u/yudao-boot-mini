package cn.bitlinks.ems.module.power.dal.dataobject.minuteagg;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Data
public class MinuteAggregateData {
    private String time;
    private BigDecimal value;
    private Long standingbookId;
    private String paramCode;

}
