package cn.bitlinks.ems.module.power.service.cophouraggdata;

import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;

import java.util.List;
import java.util.Map;

/**
 * @author liumingqiang
 */
public interface CopHourAggDataService {


    List<Map<String,Object>> copTable(ReportParamVO paramVO);

    List<List<Object>> getExcelData(ReportParamVO paramVO);

    List<List<String>>  getExcelHeader(ReportParamVO paramVO);

    CopChartResultVO copChart(ReportParamVO paramVO);
}
