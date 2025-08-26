package cn.bitlinks.ems.module.power.enums.standingbook;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StandingBookStageEnum {
    RECYCLE(5, "回收利用"),
    TERMINAL_USE(4, "终端使用"),
    TRANSPORTATION_DISTRIBUTION(3, "输送分配"),
    PROCESSING_CONVERSION(2, "加工转换"),
    PROCUREMENT_STORAGE(1, "购入存储"),
    ;
    private final Integer code;

    private final String desc;
}
