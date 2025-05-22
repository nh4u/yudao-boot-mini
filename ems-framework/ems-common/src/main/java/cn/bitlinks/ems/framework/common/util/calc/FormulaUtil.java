package cn.bitlinks.ems.framework.common.util.calc;

import cn.bitlinks.ems.framework.common.core.ParameterKey;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import static cn.bitlinks.ems.framework.common.enums.CommonConstants.PATTERN_ACQUISITION_FORMULA_PARAM;

public class FormulaUtil {
    /**
     * 从公式中提取依赖的参数。
     * 例如，从 "{[\"A\",true]}*4.2+{[\"B\",false]}" 提取出 ParameterKey(A, false) 和 ParameterKey(B, true)
     *
     * @param formula 公式字符串
     * @return 依赖的参数编码的集合
     */
    public static Set<ParameterKey> getDependencies(String formula) {
        Set<ParameterKey> dependencies = new HashSet<>();
        if (formula == null || formula.isEmpty()) {
            return dependencies;
        }

        // 使用正则表达式匹配公式中的参数引用，例如 String aa = "{[\"C\",\"true\"]}*3";
        //Pattern pattern = Pattern.compile("\\{\\[\"([^\"]+)\", (true|false|\"[^\"]+\")\\]\\}");
        Matcher matcher = PATTERN_ACQUISITION_FORMULA_PARAM.matcher(formula);

        while (matcher.find()) {
            String code = matcher.group(1);
            String energyFlagStr = matcher.group(2);
            boolean energyFlag = Boolean.TRUE.toString().equals(energyFlagStr);
            dependencies.add(new ParameterKey(code, energyFlag));
        }

        return dependencies;
    }
}

