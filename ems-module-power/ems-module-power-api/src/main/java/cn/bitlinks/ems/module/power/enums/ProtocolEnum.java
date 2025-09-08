package cn.bitlinks.ems.module.power.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;


@Getter
@AllArgsConstructor
public enum ProtocolEnum {
    OPC_DA(0, "opc-da"),
    MODBUS_TCP(1, "modbus-tcp"),
    ;
    private final Integer code;

    private final String desc;

    public static ProtocolEnum codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }
}