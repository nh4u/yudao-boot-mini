package cn.bitlinks.ems.module.power.enums.standingbook;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 台账属性树形节点类型
 */
@Getter
@AllArgsConstructor
public enum AttributeTreeNodeTypeEnum {

    SB_TYPE(0, "设备分类"),
    EQUIPMENT(1, "重点设备"),
    MEASURING(2, "计量器具"),
    ENERGY_DAQ_ATTR(3, "能源数采参数"),
    CUSTOMIZE_DAQ_ATTR(4, "自定义数采参数"),
    ;

    private final Integer code;

    private final String desc;


}