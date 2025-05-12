package cn.bitlinks.ems.module.power.utils;

import cn.hutool.core.util.ArrayUtil;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;
import com.ql.util.express.Operator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
     * 数采公式计算(存在非数值型返回)
     *
     * @param formula 填充值后的公式带符号
     * @param context 填充参数
     */
    public static Object calcAcquisitionFormula(String formula, IExpressContext<String, Object> context) {
        try {
            ExpressRunner runner = new ExpressRunner(true, false);
            runner.addFunction(FORMUlA_SUM, new SumOperator());
            runner.addFunction(FORMUlA_AVG, new AvgOperator());
            return runner.execute(formula, context, null, false, false);
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
}




