package cn.bitlinks.ems.module.power.enums.warninginfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警信息等级
 */
@Getter
@AllArgsConstructor
public enum WarningInfoLevelEnum {

    TIP(0, "提示"),
    WARNING(1, "警告"),
    MINOR(2, "次要"),
    IMPORTANT(3, "重要"),
    URGENT(4, "紧急"),
    ;

    private final Integer code;

    private final String desc;


}