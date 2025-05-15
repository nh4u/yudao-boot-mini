package cn.bitlinks.ems.module.power.enums.acquisition;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

/**
 * 采集频率单位
 */
@Getter
@AllArgsConstructor
public enum AcquisitionFrequencyUnit {
    SECONDS(1, "秒"),
    MINUTES(2, "分钟"),
    HOUR(3, "时"),
    DAY(4, "天"),
    ;
    private final Integer code;

    private final String desc;

    public static AcquisitionFrequencyUnit codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }
}
