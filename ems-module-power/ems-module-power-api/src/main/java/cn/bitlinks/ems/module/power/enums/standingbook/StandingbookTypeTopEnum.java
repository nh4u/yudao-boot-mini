package cn.bitlinks.ems.module.power.enums.standingbook;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 台账分类的顶级节点类型
 */
@Getter
@AllArgsConstructor
public enum StandingbookTypeTopEnum {
    EQUIPMENT("1", "重点设备"),
    MEASURING_INSTRUMENT("2", "计量器具"),
    ;

    private final String code;

    private final String desc;


}