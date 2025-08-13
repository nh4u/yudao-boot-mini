package cn.bitlinks.ems.module.power.enums;

import cn.bitlinks.ems.module.power.enums.warninginfo.WarningIntervalUnitEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum VoltageLevelEnum {
    V400("400", "400V"),
    V215("215", "215V"),
    V480("480", "480V");

    private String code;

    private String desc;
    public static VoltageLevelEnum codeOf(String code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }
}
