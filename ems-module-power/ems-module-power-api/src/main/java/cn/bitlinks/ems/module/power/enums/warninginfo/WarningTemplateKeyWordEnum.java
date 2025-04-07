package cn.bitlinks.ems.module.power.enums.warninginfo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum WarningTemplateKeyWordEnum {

    WARNING_TIME("告警时间", true),
    WARNING_LEVEL("告警等级", true),
    WARNING_STRATEGY_NAME("规则名称", true),
    WARNING_USER_NAME("收件人", true),
    WARNING_EXCEPTION_TIME("异常时间", false),
    WARNING_VALUE("当前值", false),
    WARNING_PARAM("告警参数", false),
    WARNING_UNIT("单位", false),
    WARNING_CONDITION_VALUE("预警值", false),
    WARNING_DEVICE_TYPE("设备分类", false),
    WARNING_DEVICE_NAME("设备名称", false),
    WARNING_DEVICE_CODE("设备编号", false),
    WARNING_DETAIL_LINK("详情链接", false),
    ;

    /**
     * 关键字
     */
    private final String keyWord;

    /**
     * 是否唯一
     */
    private final boolean isUnique;

    public static WarningTemplateKeyWordEnum keyWordOf(String keyWord) {
        return Stream.of(values()).filter(s -> s.getKeyWord().equals(keyWord)).findAny().orElse(null);
    }

    public static WarningTemplateKeyWordEnum uniqueKeyWordOf(String uniqueKeyWord) {
        return Stream.of(values()).filter(s -> s.getKeyWord().equals(uniqueKeyWord) && s.isUnique).findAny().orElse(null);
    }

    /**
     * 判断参数列表中是否超出设定
     * @param params 外来参数列表
     * @return 是否超出设定关键字
     */
    public static boolean areAnyKeywordsOutsideRange(List<String> params) {
        return params.stream().anyMatch(param -> keyWordOf(param) == null);
    }
    /**
     * 判断参数列表中是否都是唯一的关键字
     * @param params 外来参数列表
     * @return 是否超出唯一关键字
     */
    public static boolean areAnyKeywordsOutsideUniqueRange(List<String> params) {
        return params.stream().anyMatch(param -> uniqueKeyWordOf(param) == null);
    }
}

