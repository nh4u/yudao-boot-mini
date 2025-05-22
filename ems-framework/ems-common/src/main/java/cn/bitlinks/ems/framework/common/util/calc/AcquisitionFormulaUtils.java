package cn.bitlinks.ems.framework.common.util.calc;

import cn.bitlinks.ems.framework.common.core.ParameterKey;
import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.hutool.core.collection.CollUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.bitlinks.ems.framework.common.enums.CommonConstants.PATTERN_ACQUISITION_FORMULA_FILL;

/**
 * 数采计算公式部分
 */
public class AcquisitionFormulaUtils {


    /**
     * 根据数采设置计算单个参数的值
     *
     * @return 可为null
     */
    public static String calcSingleParamValue(StandingbookAcquisitionDetailDTO currentDetail, Map<ParameterKey,
            StandingbookAcquisitionDetailDTO> paramMap,
                                              Map<String, ItemStatus> itemStatusMap) {
        // 获取当前的参数设置
        String dataSite = currentDetail.getDataSite();
        String formula = currentDetail.getActualFormula();
        // 0.未配置io未配置公式
        if (StringUtils.isEmpty(dataSite) && StringUtils.isEmpty(formula)) {
            return null;
        }

        // 1. 配置了io，配置了公式/未配置公式
        if (StringUtils.isNotEmpty(dataSite)) {
            // 1.1 未配置公式
            if (StringUtils.isEmpty(formula)) {
                return itemStatusMap.get(dataSite).getValue();
            }
            // 1.2 配置了公式，替换自身参数部分进行计算
            String currenParam = String.format(PATTERN_ACQUISITION_FORMULA_FILL, currentDetail.getCode(),
                    currentDetail.getEnergyFlag());
            // 1.2.1 替换自身参数部分进行计算
            formula = formula.replace(currenParam, itemStatusMap.get(dataSite).getValue());
            Object result = CalculateUtil.calcAcquisitionFormula(formula);
            if (Objects.isNull(result)) {
                return null;
            }
            return result.toString();
        }
        // 2. 未配置io配置了公式, 需要计算出本身的公式
        // 2.1 需要找到当前的参数设置的真实公式，然后找到依赖的参数，获取他们的dataSite，
        Set<ParameterKey> parameterKeys = FormulaUtil.getDependencies(formula);
        // 配置了公式但不需要依赖任何参数，公式必须包含参数，所以公式不对。
        if (CollUtil.isEmpty(parameterKeys)) {
            return null;
        }
        // 将计算后的数值替换到公式中，
        for (ParameterKey parameterKey : parameterKeys) {
            String relyParam = String.format(PATTERN_ACQUISITION_FORMULA_FILL, parameterKey.getCode(), parameterKey.getEnergyFlag());
            String acquisitionValue = itemStatusMap.get(paramMap.get(parameterKey).getDataSite()).getValue();
            formula = formula.replace(relyParam, acquisitionValue);
        }

        // 根据公式进行计算返回结果
        Object result = CalculateUtil.calcAcquisitionFormula(formula);
        if (Objects.isNull(result)) {
            return null;
        }
        return result.toString();
    }


}
