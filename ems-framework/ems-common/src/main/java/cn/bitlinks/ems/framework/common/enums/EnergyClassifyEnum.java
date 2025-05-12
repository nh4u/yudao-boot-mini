package cn.bitlinks.ems.framework.common.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 能源分类
 * @author wangl
 * @date 2025年05月08日 16:59
 */
@Getter
@AllArgsConstructor
public enum EnergyClassifyEnum {
    //能源分类 1：外购能源；2：园区能源
    OUTSOURCED(1, "外购能源"),
    PARK(2, "园区能源"),

    ;

    private Integer code;

    private String detail;

    public EnergyClassifyEnum codeOf(Integer code){
        return Arrays.stream(values()).filter(e -> e.getCode().equals(code)).findAny().orElse(null);
    }
}
