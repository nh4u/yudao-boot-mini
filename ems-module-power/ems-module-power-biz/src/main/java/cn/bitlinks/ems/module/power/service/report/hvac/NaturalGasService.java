package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

public interface NaturalGasService {
    /**
     * 表
     * @param paramVO
     * @return
     */
    BaseReportResultVO<NaturalGasInfo> getTable(@Valid BaseTimeDateParamVO paramVO);

    /**
     * 图
     * @param paramVO
     * @return
     */
    BaseReportChartResultVO<BigDecimal> getChart(@Valid BaseTimeDateParamVO paramVO);

    List<List<String>> getExcelHeader(@Valid BaseTimeDateParamVO paramVO);

    List<List<Object>> getExcelData(@Valid BaseTimeDateParamVO paramVO);
}
