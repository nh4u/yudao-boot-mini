package cn.bitlinks.ems.module.power.enums.additionalrecording;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdditionalRecordingScene {

    NO_HIS(1, "无历史数据"),
    ONE_NEXT(2, "下一个业务点"),
    ONE_PRE(3, "上一个业务点"),
    TWO_POINT(4, "两个业务点"),
//    TWO_PRE_COVER(5, "两个业务点与前一个业务点重合"),
//    TWO_NEXT_COVER(6, "两个业务点与后一个业务点重合"),
    ;

    private final Integer code;

    private final String desc;


}