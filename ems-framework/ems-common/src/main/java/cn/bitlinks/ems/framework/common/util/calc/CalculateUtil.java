package cn.bitlinks.ems.framework.common.util.calc;

import cn.bitlinks.ems.framework.common.pojo.StatsResult;
import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;
import com.ql.util.express.Operator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/03/31 08:54
 **/

@Slf4j
public class CalculateUtil {

    public static final int SCALE = 30;
    public static final String FORMUlA_SUM = "SUM";
    public static final String FORMUlA_AVG = "AVG";
    /**
     * 数采公式计算(存在非数值型返回)
     *
     * @param formula 填充值后的公式带符号
     */
    public static Object calcAcquisitionFormula(String formula) {
        try {
            ExpressRunner runner = new ExpressRunner(true, false);
            runner.addFunction(FORMUlA_SUM, new SumOperator());
            runner.addFunction(FORMUlA_AVG, new AvgOperator());
            return runner.execute(formula, null, null, false, false);
        } catch (Exception e) {
            log.error("CalculateUtil error : 表达式【{}】", formula, e);
        }
        return null;
    }


    /**
     * 将数据总量、碳排因子值代入碳排因子公式进行计算  公式示例：$value*$factor
     *
     * @param formula
     * @param totalAcqValue
     * @param factorValue
     * @return
     */
    public static BigDecimal calFormula(String formula, BigDecimal totalAcqValue, BigDecimal factorValue) {

        BigDecimal result = null;
        ExpressRunner runner = new ExpressRunner(true, false);
        IExpressContext<String, Object> context = new DefaultContext<>();
        context.put("$value", totalAcqValue);
        context.put("$factor", factorValue);
        try {
            BigDecimal execute = new BigDecimal(runner.execute(formula, context, null, true, false).toString());
            result = execute.setScale(SCALE, RoundingMode.DOWN);

        } catch (Exception e) {
            log.info("CalculateUtil error : ", e);
        }
        return result;

    }


    /**
     * sum 和 avg 测试 max min 本身自带有在 OperatorMinMax类中
     */
    public static void qlExpressTest() throws Exception {

        // 运算符
        ExpressRunner runner = new ExpressRunner();
        runner.addFunction("sum", new SumOperator());
        runner.addFunction("avg", new AvgOperator());

        //参数
        DefaultContext<String, Object> context = new DefaultContext<String, Object>();
        context.put("用电量1", 100);
        context.put("用电量2", 200);
        context.put("用电量3", 300);
        context.put("电力标煤系数", 10);
        String express1 = "用电量*电力标煤系数*1.5";
        String express2 = "sum(用电量1,用电量2,用电量3)*电力标煤系数*1.5";
        String express3 = "avg(用电量1,用电量2,用电量3)*电力标煤系数*1.5";
        String express4 = "min(用电量1,用电量2,用电量3)*电力标煤系数*1.5";
        String express5 = "max(用电量1,用电量2,用电量3)*电力标煤系数*1.5";
        String express6 = "if ( 用电量1 > 用电量2 ) then {return 用电量1;} else {return 用电量2;}";
        String express7 = " (用电量1 < 用电量2 and 用电量3 >用电量2 ? 用电量1 : 用电量2)*电力标煤系数*1.5";
        String express8 = "if ( 用电量1 > 用电量2 ) then {return 用电量1*电力标煤系数*1.5;} else {return 用电量2*电力标煤系数*1.5;}";
        // 如果调用过程不出现异常，指令集instructionSet就是可以被加载运行（execute）了！
        // InstructionSet instructionSet = runner.parseInstructionSet(express);
        Object r = runner.execute(express8, context, null, false, false);
        System.out.println(r);
    }

    /**
     * 定义一个继承自com.ql.util.express.Operator的操作符（SUM）
     */
    public static class SumOperator extends Operator {
        @Override
        public Object executeInner(Object[] list) {

            BigDecimal sum = BigDecimal.ZERO;
            for (Object num : list) {
                BigDecimal n = new BigDecimal(num.toString());
                sum = sum.add(n);
            }
            return sum;
        }
    }

    /**
     * 定义一个继承自com.ql.util.express.Operator的操作符（AVG）
     */
    public static class AvgOperator extends Operator {
        @Override
        public Object executeInner(Object[] list) {
            BigDecimal avg = null;
            if (ArrayUtil.isNotEmpty(list)) {
                //求和
                BigDecimal sum = BigDecimal.ZERO;
                for (Object num : list) {
                    BigDecimal n = new BigDecimal(num.toString());
                    sum = sum.add(n);
                }

                // 求平均值
                BigDecimal length = BigDecimal.valueOf(list.length);
                avg = sum.divide(length, SCALE, RoundingMode.HALF_UP);
            }
            return avg;
        }
    }
    /**
     * 通用分组统计方法
     * @param list 原始数据列表
     * @param groupKeyFunc 分组 key 提取函数（例如：r -> r.getType()）
     * @param valueExtractor 参与统计字段提取函数（必须是 BigDecimal）
     * @param <T> 数据类型
     * @param <K> 分组 key 类型
     * @return 每个 groupKey 对应的 Stats 统计信息
     */
    public static <T, K> Map<K, StatsResult> calculateGroupStats(List<T> list,
                                                                 Function<T, K> groupKeyFunc,
                                                                 Function<T, BigDecimal> valueExtractor) {
        Map<K, List<T>> grouped = list.stream()
                .collect(Collectors.groupingBy(groupKeyFunc));

        Map<K, StatsResult> result = new HashMap<>();

        for (Map.Entry<K, List<T>> entry : grouped.entrySet()) {
            K key = entry.getKey();
            List<T> items = entry.getValue();

            List<BigDecimal> values = items.stream()
                    .map(valueExtractor)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            BigDecimal sum = values.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal avg = values.isEmpty() ? BigDecimal.ZERO :
                    sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);

            BigDecimal max = values.stream()
                    .max(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            BigDecimal min = values.stream()
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            StatsResult statsResult = new StatsResult();
            statsResult.setAvg(avg);
            statsResult.setSum(sum);
            statsResult.setMax(max);
            statsResult.setMin(min);
            result.put(key,statsResult);
        }

        return result;
    }

    /**
     * 多字段分组，多个字段统计（sum / avg / max / min）
     *
     * @param list 原始数据列表
     * @param groupKeyFunc 多字段分组 key 提取函数（可用字符串拼接或 Map 封装等方式）
     * @param valueExtractors 统计字段提取函数 Map（key 为字段名，value 为提取函数）
     * @param <T> 数据类型
     * @param <K> 分组 key 类型（如：String 或 List<String> 等）
     * @return 每组分组对应的统计字段结果 Map
     */
    public static <T, K> Map<K, Map<String, StatsResult>> calculateMultiFieldGroupStats(
            List<T> list,
            Function<T, K> groupKeyFunc,
            Map<String, Function<T, BigDecimal>> valueExtractors
    ) {
        Map<K, List<T>> grouped = list.stream()
                .collect(Collectors.groupingBy(groupKeyFunc));

        Map<K, Map<String, StatsResult>> result = new LinkedHashMap<>();

        for (Map.Entry<K, List<T>> entry : grouped.entrySet()) {
            K groupKey = entry.getKey();
            List<T> items = entry.getValue();

            Map<String, StatsResult> statsMap = new LinkedHashMap<>();

            for (Map.Entry<String, Function<T, BigDecimal>> fieldEntry : valueExtractors.entrySet()) {
                String fieldName = fieldEntry.getKey();
                Function<T, BigDecimal> extractor = fieldEntry.getValue();

                List<BigDecimal> values = items.stream()
                        .map(extractor)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = values.isEmpty() ? BigDecimal.ZERO :
                        sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
                BigDecimal max = values.stream().max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
                BigDecimal min = values.stream().min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

                StatsResult stats = new StatsResult();
                stats.setSum(sum);
                stats.setAvg(avg);
                stats.setMax(max);
                stats.setMin(min);

                statsMap.put(fieldName, stats);
            }

            result.put(groupKey, statsMap);
        }

        return result;
    }

}




