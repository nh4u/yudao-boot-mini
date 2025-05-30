package cn.bitlinks.ems.framework.common.enums;


import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 查询条件时间类型
 * @author wangl
 * @date 2025年05月08日 16:59
 */
@Getter
@AllArgsConstructor
public enum DataTypeEnum {

    //时间类型 0：日；1：月；2：年；3：时
    DAY(0, "日", "yyyy-MM-dd"),
    MONTH(1, "月","yyyy-MM"),
    YEAR(2, "年","yyyy"),
    HOUR(3, "时","yyyy-MM-dd HH"),

    ;

    private Integer code;

    private String detail;

    private String format;

    public static DataTypeEnum codeOf(Integer code){
        return Arrays.stream(values()).filter(e -> e.getCode().equals(code)).findAny().orElse(null);
    }
}
