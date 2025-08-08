package cn.bitlinks.ems.module.power.service.cophouraggdata;

import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopTableResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface CopHourAggDataService {


    CopTableResultVO copTable(ReportParamVO paramVO);

    List<List<Object>> getExcelData(ReportParamVO paramVO);

    List<List<String>>  getExcelHeader(ReportParamVO paramVO);

    CopChartResultVO copChart(ReportParamVO paramVO);
}
