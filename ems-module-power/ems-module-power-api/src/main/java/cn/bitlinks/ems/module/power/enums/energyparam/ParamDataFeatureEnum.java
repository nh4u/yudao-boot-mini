package cn.bitlinks.ems.module.power.enums.energyparam;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParamDataFeatureEnum {

    Accumulated(1, "累计值"),
    STEADY(2, "稳态值"),
    STATUS(3, "状态值"),
    ;

    private final Integer code;

    private final String desc;


}