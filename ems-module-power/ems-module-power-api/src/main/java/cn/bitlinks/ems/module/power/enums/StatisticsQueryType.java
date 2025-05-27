package cn.bitlinks.ems.module.power.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wangl
 * @date 2025年05月24日 18:14
 */
@Getter
@AllArgsConstructor
public enum StatisticsQueryType {
    //查看类型 0：综合查看；1：按能源查看；2：按标签查看。 默认0
    COMPREHENSIVE_VIEW(0, "综合查看"),
    ENERGY_VIEW(1, "按能源查看"),
    TAG_VIEW(2, "按标签查看");

    private Integer code;

    private String detail;
}
