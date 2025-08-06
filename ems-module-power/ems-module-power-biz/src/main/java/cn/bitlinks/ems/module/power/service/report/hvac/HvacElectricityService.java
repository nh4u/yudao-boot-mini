package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportMultiChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.HvacElectricityInfo;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.HvacElectricityParamVO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface HvacElectricityService {
    /**
     * 表
     *
     * @param paramVO
     * @return
     */
    BaseReportResultVO<HvacElectricityInfo> getTable(@Valid HvacElectricityParamVO paramVO);

    /**
     * 图
     *
     * @param paramVO
     * @return
     */
    BaseReportMultiChartResultVO<Map<String, List<BigDecimal>>> getChart(@Valid HvacElectricityParamVO paramVO);

    List<List<String>> getExcelHeader(@Valid HvacElectricityParamVO paramVO);

    List<List<Object>> getExcelData(@Valid HvacElectricityParamVO paramVO);
}
