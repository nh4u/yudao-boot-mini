package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.HeatingSummaryInfo;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

public interface HeatingSummaryService {
    /**
     * 表
     * @param paramVO
     * @return
     */
    BaseReportResultVO<HeatingSummaryInfo> getTable(@Valid BaseTimeDateParamVO paramVO);

    /**
     * 图
     * @param paramVO
     * @return
     */
    BaseReportChartResultVO<BigDecimal> getChart(@Valid BaseTimeDateParamVO paramVO);

    List<List<String>> getExcelHeader(@Valid BaseTimeDateParamVO paramVO);

    List<List<Object>> getExcelData(@Valid BaseTimeDateParamVO paramVO);
}
