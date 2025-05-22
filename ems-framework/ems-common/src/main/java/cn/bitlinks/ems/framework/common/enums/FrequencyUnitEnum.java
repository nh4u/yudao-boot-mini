package cn.bitlinks.ems.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

/**
 * 采集频率单位
 */
@Getter
@AllArgsConstructor
public enum FrequencyUnitEnum {
    SECONDS(3, "秒"),
    MINUTES(2, "分"),
    HOUR(1, "时"),
    DAY(0, "天"),
    ;
    private final Integer code;

    private final String desc;

    public static FrequencyUnitEnum codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }
}
