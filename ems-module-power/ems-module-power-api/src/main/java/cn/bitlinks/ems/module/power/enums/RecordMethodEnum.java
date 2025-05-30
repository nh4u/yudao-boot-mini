package cn.bitlinks.ems.module.power.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum RecordMethodEnum {
    IMPORT_MANUAL(1, "手动录入"),
    IMPORT_VOUCHER(2, "凭证信息导入"),
    ;
    private final Integer code;

    private final String desc;
    public static RecordMethodEnum codeOf(Integer code) {
        return Stream.of(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }
}
