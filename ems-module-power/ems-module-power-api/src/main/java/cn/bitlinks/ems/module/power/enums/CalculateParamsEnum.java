package cn.bitlinks.ems.module.power.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wangl
 * @date 2025年06月11日 15:04
 */
@Getter
@AllArgsConstructor
public enum CalculateParamsEnum{

    ENERGY_CONSUMPTION("能源用量"),
    PRICE("单价"),
    STANDARD_COAL_COEFFICIENT ("折标煤系数"),
;


    private String detail;
}
