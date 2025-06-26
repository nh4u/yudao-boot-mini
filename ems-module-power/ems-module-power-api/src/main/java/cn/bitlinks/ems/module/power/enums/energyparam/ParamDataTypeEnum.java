package cn.bitlinks.ems.module.power.enums.energyparam;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ParamDataTypeEnum {

    NUMBER(1, "数字"),
    TEXT(2, "文本"),
    ;

    private final Integer code;

    private final String desc;


}