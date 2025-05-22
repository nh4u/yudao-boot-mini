package cn.bitlinks.ems.module.power.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BillingMethod {

    //计费方式  |  1：统一计价  2：分时段计价  3：阶梯计价',
    UNIFIED_PRICE(1, "统一计价"),
    TIME_SPAN_PRICE(2, "分时段计价"),
    STAIR_PRICE(3, "阶梯计价");

    private Integer code;

    private String detail;



}
