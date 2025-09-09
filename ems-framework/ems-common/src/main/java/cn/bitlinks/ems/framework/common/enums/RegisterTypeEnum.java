package cn.bitlinks.ems.framework.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum RegisterTypeEnum {
    COILS("coils", "01（线圈）"),
    DISCRETE_INPUTS("discrete_inputs", "02（离散输入）"),
    HOLDING_REGISTERS("holding_registers", "03（保持寄存器）"),
    INPUT_REGISTERS("input_registers", "04（输入寄存器）"),
    ;
    private final String code;

    private final String desc;

    public static RegisterTypeEnum codeOf(String code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }

}