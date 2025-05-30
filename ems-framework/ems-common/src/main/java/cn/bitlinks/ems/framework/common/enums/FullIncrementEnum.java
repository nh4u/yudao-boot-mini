package cn.bitlinks.ems.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum FullIncrementEnum {
    FULL(0, "全量"),
    INCREMENT(1, "增量"),
    ;
    private final Integer code;

    private final String desc;
    public static FullIncrementEnum codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }
}
