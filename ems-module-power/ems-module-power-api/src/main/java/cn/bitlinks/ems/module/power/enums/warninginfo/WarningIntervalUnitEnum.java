package cn.bitlinks.ems.module.power.enums.warninginfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum WarningIntervalUnitEnum {
    HOUR(1, "时"),
    MINUTE(2, "分"),
    SECOND(3, "秒"),
    ;


    private final Integer code;

    private final String desc;

    public static WarningIntervalUnitEnum codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }

    private static final Map<WarningIntervalUnitEnum, BiFunction<LocalDateTime, Integer, LocalDateTime>> intervalMap = new EnumMap<>(WarningIntervalUnitEnum.class);

    static {
        intervalMap.put(WarningIntervalUnitEnum.SECOND, LocalDateTime::plusSeconds);
        intervalMap.put(WarningIntervalUnitEnum.MINUTE, LocalDateTime::plusMinutes);
        intervalMap.put(WarningIntervalUnitEnum.HOUR, LocalDateTime::plusHours);
    }

    /**
     * 根据时间间隔单位计算时间
     *
     * @param intervalUnit  时间间隔单位
     * @param latestTime    上次最新的时间
     * @param intervalValue 间隔时间
     * @return 计算后的时间
     */
    public static LocalDateTime calculateThresholdTime(WarningIntervalUnitEnum intervalUnit, LocalDateTime latestTime, int intervalValue) {
        BiFunction<LocalDateTime, Integer, LocalDateTime> intervalFunction = intervalMap.get(intervalUnit);
        if (intervalFunction != null) {
            return intervalFunction.apply(latestTime, intervalValue);
        } else {
            // 该策略系统不支持处理，简单返回null
            return null;
        }
    }
}
