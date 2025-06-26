package cn.bitlinks.ems.module.power.controller.admin.report.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


/**
 * @author liumingqiang
 */
@Data
@NoArgsConstructor
public class CopHourAggData {
    /**
     * 聚合时间
     */
    private String time;
    /**
     * 低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS
     */
    private String copType;
    /**
     * 公式计算值
     */
    private BigDecimal copValue;


}
