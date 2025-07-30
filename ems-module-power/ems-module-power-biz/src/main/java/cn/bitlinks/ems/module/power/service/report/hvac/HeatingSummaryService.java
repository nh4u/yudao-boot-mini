package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.HeatingSummaryInfo;

import javax.validation.Valid;

public interface HeatingSummaryService {

    BaseReportResultVO<HeatingSummaryInfo> getTable(@Valid BaseTimeDateParamVO paramVO);

}
