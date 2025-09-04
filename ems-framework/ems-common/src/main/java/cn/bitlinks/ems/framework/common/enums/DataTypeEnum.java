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
    MINUTE(4, "分","yyyy-MM-dd HH:mm"),
    ;
    // 假设的静态变量（需要返回的目标值）
    public static final String DAILY_STATISTICS = "每日合计";
    public static final String MONTHLY_STATISTICS = "每月合计";
    public static final String YEARLY_STATISTICS = "每年合计";
    private Integer code;

    private String detail;

    private String format;

    public static DataTypeEnum codeOf(Integer code){
        return Arrays.stream(values()).filter(e -> e.getCode().equals(code)).findAny().orElse(null);
    }
    public static String getBottomSumCell(DataTypeEnum dataTypeEnum){
        switch (dataTypeEnum) {
            case DAY:
                return DAILY_STATISTICS;
            case MONTH:
                return MONTHLY_STATISTICS;
            case YEAR:
                return YEARLY_STATISTICS;
            default:
                return "";
        }
    }
}
