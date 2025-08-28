package cn.bitlinks.ems.module.power.controller.admin.externalapi.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * @author liumingqiang
 */
@Data
public class ProductYieldMeta implements Serializable {
    /**
     * 出产时间
     */
    private String FABOUTTIME;

    /**
     * 计划产量
     */
    private BigDecimal PLAN_QTY;

    /**
     * 实际产量
     */
    private BigDecimal LOT_QTY;

    /**
     * 中间处理时间
     */
    private LocalDateTime time;
}
