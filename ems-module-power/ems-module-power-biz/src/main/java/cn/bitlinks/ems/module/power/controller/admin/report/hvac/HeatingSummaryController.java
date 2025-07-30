package cn.bitlinks.ems.module.power.controller.admin.report.hvac;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.HeatingSummaryInfo;
import cn.bitlinks.ems.module.power.service.report.hvac.HeatingSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;


@Tag(name = "管理后台 - 个性化报表[暖通科报表]-热力汇总报表")
@RestController
@RequestMapping("/power/report/hvac/heatingSummary")
@Validated
public class HeatingSummaryController {
    @Resource
    private HeatingSummaryService heatingSummaryService;

    @PostMapping("/table")
    @Operation(summary = "表")
    public CommonResult<BaseReportResultVO<HeatingSummaryInfo>> getTable(@Valid @RequestBody BaseTimeDateParamVO paramVO) {
        return success(heatingSummaryService.getTable(paramVO));
    }

    @PostMapping("/chart")
    @Operation(summary = "图")
    public CommonResult<BaseReportChartResultVO<BigDecimal>> getChart(@Valid @RequestBody BaseTimeDateParamVO paramVO) {
        return success(heatingSummaryService.getChart(paramVO));
    }

//    @PostMapping("/export-excel")
//    @Operation(summary = "导出COP Excel")
//    @ApiAccessLog(operateType = EXPORT)
//    public void exportCopExcel(@Valid @RequestBody ReportParamVO pageReqVO,
//                               HttpServletResponse response) throws IOException {

//        String filename = "COP报表.xlsx";
//        List<List<String>> header = copHourAggDataService.getExcelHeader(pageReqVO);
//        List<List<Object>> dataList = copHourAggDataService.getExcelData(pageReqVO);
//
//        // 放在 write前配置response才会生效，放在后面不生效
//        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
//        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
//        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
//
//        EasyExcel.write(response.getOutputStream())
//                // 动态头
//                .head(header)
//                .sheet("数据")
//                // 表格数据
//                .doWrite(dataList);
//    }


}
