package cn.bitlinks.ems.framework.common.enums;

import java.util.Arrays;

import cn.bitlinks.ems.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用能分析查看范围
 * @author wangl
 * @date 2025年05月06日 15:51
 */
@Getter
@AllArgsConstructor
public enum QueryDimensionEnum implements IntArrayValuable {
    //0：综合查看；1：按能源查看；2：按标签查看
    OVERALL_REVIEW(0, "综合查看"),
    ENERGY_REVIEW(1, "按能源查看"),
    LABEL_REVIEW(2, "按标签查看"),
    ;

    /**
     * 类型
     */
    private final Integer code;
    /**
     * 名称
     */
    private final String name;

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(QueryDimensionEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static QueryDimensionEnum codeOf(Integer code) {
        return  Arrays.stream(values()).filter(s -> s.getCode().equals(code)).findAny().orElse(null);
    }
}
