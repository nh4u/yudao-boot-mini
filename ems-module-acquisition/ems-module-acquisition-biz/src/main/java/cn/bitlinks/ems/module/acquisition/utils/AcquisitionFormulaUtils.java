package cn.bitlinks.ems.module.acquisition.utils;

import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
import cn.bitlinks.ems.module.acquisition.api.job.dto.StandingbookAcquisitionDetailDTO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.PATTERN_ACQUISITION_FORMULA_FILL;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.SPRING_PROFILES_ACTIVE_PROD;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.SERVICE_SETTINGS_NOT_EXISTS;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_ACQUISITION_TEST_FAIL;

/**
 * 数采计算公式部分
 */
public class AcquisitionFormulaUtils {

    public String testData(List<StandingbookAcquisitionDetailDTO> acquisitionDetailDTOS) {

        // 1. 计算每一个参数的值
        acquisitionDetailDTOS.forEach(acquisitionDetailDTO -> {

        });
        // 获取当前的参数设置
        //StandingbookAcquisitionDetailVO currentDetail = testReqVO.getCurrentDetail();
        //currentDetail.setActualFormula(currentDetail.getFormula());
        String dataSite = currentDetail.getDataSite();
        String formula = currentDetail.getFormula();
        // 0.未配置io未配置公式
        if (StringUtils.isEmpty(dataSite) && StringUtils.isEmpty(formula)) {
            throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
        }
        // 0.获取服务设置
        ServiceSettingsDO serviceSettingsDO = serviceSettingsMapper.selectById(testReqVO.getServiceSettingsId());
        if (Objects.isNull(serviceSettingsDO)) {
            throw exception(SERVICE_SETTINGS_NOT_EXISTS);
        }
        //try {
        // 1. 配置了io，配置了公式/未配置公式
        if (StringUtils.isNotEmpty(dataSite)) {
            // 采集参数
            Map<String, ItemStatus> itemStatusMap;
            if (env.equals(SPRING_PROFILES_ACTIVE_PROD)) {
                itemStatusMap = OpcDaUtils.batchGetValue(serviceSettingsDO.getIpAddress(),
                        serviceSettingsDO.getUsername(),
                        serviceSettingsDO.getPassword(),
                        serviceSettingsDO.getClsid(), Collections.singletonList(dataSite));
            } else {
                itemStatusMap = mockItemStatus(Collections.singletonList(dataSite));
            }

            if (CollUtil.isEmpty(itemStatusMap)) {
                throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
            }
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
                throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
            }
            return result.toString();
        }
        // 2. 未配置io配置了公式, 需要计算出本身的公式

        // 创建一个 Map，用于存储参数的唯一标识 (ParameterKey) 到 StandingbookAcquisitionDetailVO 对象的映射
        Map<ParameterKey, StandingbookAcquisitionDetailVO> paramMap = new HashMap<>();
        for (StandingbookAcquisitionDetailVO detail : testReqVO.getDetails()) {
            ParameterKey key = new ParameterKey(detail.getCode(), detail.getEnergyFlag());
            detail.setActualFormula(detail.getFormula());
            paramMap.put(key, detail);
        }

        // 计算当前公式的真实公式
        StandingbookAcquisitionDetailVO currentFormulaDetail = expandFormula(currentDetail, paramMap, new HashSet<>());
        // 2.1 需要找到当前的参数设置的真实公式，然后找到依赖的参数，获取他们的dataSite，
        Set<ParameterKey> parameterKeys = getDependencies(currentFormulaDetail.getActualFormula());
        // 配置了公式但不需要依赖任何参数，公式必须包含参数，所以公式不对。
        if (CollUtil.isEmpty(parameterKeys)) {
            throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
        }

        Map<ParameterKey, StandingbookAcquisitionDetailVO> relyParamMap = paramMap.entrySet().stream()
                .filter(entry -> parameterKeys.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (CollUtil.isEmpty(relyParamMap)) {
            throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
        }
        List<String> dataSites = relyParamMap.values().stream().map(StandingbookAcquisitionDetailVO::getDataSite).collect(Collectors.toList());
        // 2.2 采集这些参数，
        // 采集参数
        Map<String, ItemStatus> itemStatusMap;
        if (env.equals(SPRING_PROFILES_ACTIVE_PROD)) {
            itemStatusMap = OpcDaUtils.batchGetValue(serviceSettingsDO.getIpAddress(),
                    serviceSettingsDO.getUsername(),
                    serviceSettingsDO.getPassword(),
                    serviceSettingsDO.getClsid(), dataSites);
        } else {
            itemStatusMap = mockItemStatus(dataSites);
        }

        if (CollUtil.isEmpty(itemStatusMap)) {
            throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
        }
        // 将计算后的数值替换到公式中，

        String actualFormula = currentFormulaDetail.getActualFormula();
        String finalFormula = relyParamMap.entrySet().stream()
                .map(entry -> {
                    String relyParam = String.format(PATTERN_ACQUISITION_FORMULA_FILL, entry.getValue().getCode(), currentDetail.getEnergyFlag());
                    return actualFormula.replace(relyParam, itemStatusMap.get(entry.getValue().getDataSite()).getValue());
                })
                .reduce((prev, curr) -> curr)
                .orElse(actualFormula);

        // 根据公式进行计算返回结果
        Object result = CalculateUtil.calcAcquisitionFormula(finalFormula);
        if (Objects.isNull(result)) {
            throw exception(STANDINGBOOK_ACQUISITION_TEST_FAIL);
        }
        return result.toString();

    }
}
