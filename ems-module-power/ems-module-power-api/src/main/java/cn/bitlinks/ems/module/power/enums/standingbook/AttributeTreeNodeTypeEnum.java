package cn.bitlinks.ems.module.power.enums.standingbook;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 台账属性树形节点类型
 */
@Getter
@AllArgsConstructor
public enum AttributeTreeNodeTypeEnum {

    SB_TYPE(0, "台账类型"),
    SB(1, "台账"),
    ATTR(2, "属性"),
    ;

    private final Integer code;

    private final String desc;


}