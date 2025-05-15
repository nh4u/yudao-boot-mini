package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.service.statistics.ComparisonV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;


@Tag(name = "管理后台 - 用能分析/环比分析")
@RestController
@RequestMapping("/power/statistics/comparison/v2")
@Validated
public class ComparisonStatisticsV2Controller {

    @Resource
    private ComparisonV2Service comparisonV2Service;


    @PostMapping("/discountAnalysisTable")
    @Operation(summary = "折价环比分析（表）")
    public CommonResult<StatisticsResultV2VO> discountAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(comparisonV2Service.discountAnalysisTable(paramVO));
    }

    @PostMapping("/discountAnalysisChart")
    @Operation(summary = "折价环比分析（图）")
    public CommonResult<ComparisonChartResultVO> discountAnalysisChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(comparisonV2Service.discountAnalysisChart(paramVO));

    }

}
