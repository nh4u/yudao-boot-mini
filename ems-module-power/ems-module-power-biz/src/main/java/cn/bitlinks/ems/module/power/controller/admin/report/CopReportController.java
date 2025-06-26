package cn.bitlinks.ems.module.power.controller.admin.report;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/06/21 19:23
 **/

@Tag(name = "管理后台 - COP报表")
@RestController
@RequestMapping("/power/report")
@Validated
public class CopReportController {


    @Resource
    private CopHourAggDataService copHourAggDataService;


    @PostMapping("/copTable")
    @Operation(summary = "COP表")
    public CommonResult<List<Map<String, Object>>> copTable(@Valid @RequestBody ReportParamVO paramVO) {
        return success(copHourAggDataService.copTable(paramVO));
    }

    @PostMapping("/copChart")
    @Operation(summary = "COP图")
    public CommonResult<CopChartResultVO> copChart(@Valid @RequestBody ReportParamVO paramVO) {
        return success(copHourAggDataService.copChart(paramVO));
    }


}
