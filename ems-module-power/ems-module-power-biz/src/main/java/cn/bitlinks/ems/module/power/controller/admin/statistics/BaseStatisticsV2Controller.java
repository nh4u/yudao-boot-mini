package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.BaseStatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.ComparisonChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.service.statistics.BaseV2Service;
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


@Tag(name = "管理后台 - 用能分析/定基比分析")
@RestController
@RequestMapping("/power/statistics/base/v2")
@Validated
public class BaseStatisticsV2Controller {

    @Resource
    private BaseV2Service baseV2Service;


    @PostMapping("/discountAnalysisTable")
    @Operation(summary = "折价定基比分析（表）")
    public CommonResult<StatisticsResultV2VO> discountAnalysisTable(@Valid @RequestBody BaseStatisticsParamV2VO paramVO) {
        return success(baseV2Service.discountAnalysisTable(paramVO));
    }

    @PostMapping("/discountAnalysisChart")
    @Operation(summary = "折价定基比分析（图）")
    public CommonResult<ComparisonChartResultVO> discountAnalysisChart(@Valid @RequestBody BaseStatisticsParamV2VO paramVO) {
        return success(baseV2Service.discountAnalysisChart(paramVO));

    }

    @PostMapping("/foldCoalAnalysisTable")
    @Operation(summary = "折煤定基比分析（表）")
    public CommonResult<StatisticsResultV2VO> foldCoalAnalysisTable(@Valid @RequestBody BaseStatisticsParamV2VO paramVO) {
        return success(baseV2Service.foldCoalAnalysisTable(paramVO));
    }

    @PostMapping("/foldCoalAnalysisChart")
    @Operation(summary = "折煤定基比分析（图）")
    public CommonResult<ComparisonChartResultVO> foldCoalAnalysisChart(@Valid @RequestBody BaseStatisticsParamV2VO paramVO) {
        return success(baseV2Service.foldCoalAnalysisChart(paramVO));
    }
}
