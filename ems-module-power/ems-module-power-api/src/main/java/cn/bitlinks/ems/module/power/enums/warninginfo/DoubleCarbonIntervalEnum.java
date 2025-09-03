package cn.bitlinks.ems.module.power.enums.warninginfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum DoubleCarbonIntervalEnum {
    MINUTES(1, "分钟"),
    HOUR(2, "小时"),
    DAY(3, "天"),
    ;


    private final Integer code;

    private final String desc;

    public static DoubleCarbonIntervalEnum codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }

}
