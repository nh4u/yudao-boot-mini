package cn.bitlinks.ems.module.power.enums.warninginfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;


@Getter
@AllArgsConstructor
public enum WarningStrategyConnectorEnum {
    NOT_CONTAINS(7, "不包含"),
    CONTAINS(6, "包含"),
    NE(5, "≠"),
    EQUALS(4, "="),
    LTE(3, "≤"),
    GTE(2, "≥"),
    LT(1, "＜"),
    GT(0, "＞"),
    ;

    private final Integer code;

    private final String desc;

    public static WarningStrategyConnectorEnum codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }

    private static final Map<WarningStrategyConnectorEnum, BiFunction<String, String, Boolean>> strategyMap = new EnumMap<>(WarningStrategyConnectorEnum.class);

    static {
        strategyMap.put(WarningStrategyConnectorEnum.NOT_CONTAINS, (conditionValue, value) -> !conditionValue.equals(value));
        strategyMap.put(WarningStrategyConnectorEnum.CONTAINS, String::equals);
        strategyMap.put(WarningStrategyConnectorEnum.NE, (conditionValue, value) -> !new BigDecimal(conditionValue).equals(new BigDecimal(value)));
        strategyMap.put(WarningStrategyConnectorEnum.EQUALS, (conditionValue, value) -> new BigDecimal(conditionValue).equals(new BigDecimal(value)));
        strategyMap.put(WarningStrategyConnectorEnum.LTE, (conditionValue, value) -> new BigDecimal(conditionValue).compareTo(new BigDecimal(value)) <= 0);
        strategyMap.put(WarningStrategyConnectorEnum.GTE, (conditionValue, value) -> new BigDecimal(conditionValue).compareTo(new BigDecimal(value)) >= 0);
        strategyMap.put(WarningStrategyConnectorEnum.LT, (conditionValue, value) -> new BigDecimal(conditionValue).compareTo(new BigDecimal(value)) < 0);
        strategyMap.put(WarningStrategyConnectorEnum.GT, (conditionValue, value) -> new BigDecimal(conditionValue).compareTo(new BigDecimal(value)) > 0);
    }

    /**
     * 匹配策略的条件，参数条件是否符合
     *
     * @param conditionValue               条件值
     * @param value                        设备参数值
     * @param warningStrategyConnectorEnum 连接符
     * @return 是否符合条件
     */
    public static boolean evaluate(WarningStrategyConnectorEnum warningStrategyConnectorEnum, String conditionValue, String value) {
        BiFunction<String, String, Boolean> strategy = strategyMap.get(warningStrategyConnectorEnum);
        return strategy != null ? strategy.apply(conditionValue, value) : false;
    }
}
