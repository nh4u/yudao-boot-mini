package cn.bitlinks.ems.module.power.utils;

import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import com.ql.util.express.IExpressContext;
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
}
