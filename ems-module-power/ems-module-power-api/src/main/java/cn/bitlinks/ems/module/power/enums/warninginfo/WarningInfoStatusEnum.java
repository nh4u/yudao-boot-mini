package cn.bitlinks.ems.module.power.enums.warninginfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 告警信息处理状态
 */
@Getter
@AllArgsConstructor
public enum WarningInfoStatusEnum {

    NOT_PROCESSED(0, "未处理"),
    PROCESSING(1, "处理中"),
    PROCESSED(2, "已处理"),
    ;


    private final Integer code;

    private final String desc;


}