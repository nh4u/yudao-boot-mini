package cn.bitlinks.ems.module.power.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 公式类型
 */
@Getter
@AllArgsConstructor
public enum FormulaTypeEnum {

    COAL(1, "折标煤公式"),
    USAGE_COST(2, "用能成本公式"),
    ;

    private Integer code;

    private String detail;
}
