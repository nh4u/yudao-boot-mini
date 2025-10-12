package cn.bitlinks.ems.framework.common.enums;

import java.util.regex.Pattern;

public interface CommonConstants {
    String SPRING_PROFILES_ACTIVE_LOCAL = "local";
    String SPRING_PROFILES_ACTIVE_DEV = "dev";
    String SPRING_PROFILES_ACTIVE_TEST = "test";
    /**
     * 数采公式：匹配格式获取公式中的参数
     */
    Pattern PATTERN_ACQUISITION_FORMULA_PARAM = Pattern.compile("\\{\\[\"([^\"]+)\"," +
            " (true|false|\"[^\"]+\")\\]\\}");
    /**
     * 数采公式：填充参数格式 code、energyFlag
     */
    String  PATTERN_ACQUISITION_FORMULA_FILL =
            "{[\"%s\", %s]}";
}
