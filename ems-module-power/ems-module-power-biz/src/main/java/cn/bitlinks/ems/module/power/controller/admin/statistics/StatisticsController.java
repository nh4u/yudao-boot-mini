package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsOverviewResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsOverviewService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsRatioService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 用能分析")
@RestController
@RequestMapping("/power/statistics")
@Validated
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @Resource
    private StatisticsOverviewService statisticsOverviewService;

    @Resource
    private StatisticsRatioService statisticsRatioService;

    @PostMapping("/overview")
    @Operation(summary = "统计总览")
    public CommonResult<StatisticsOverviewResultVO> overview(@Valid @RequestBody StatisticsParamVO paramVO) {
        StatisticsOverviewResultVO statisticsOverviewResultVO = statisticsOverviewService.overview(paramVO);
        return success(statisticsOverviewResultVO);
    }


    @PostMapping("/energyFlowAnalysis")
    @Operation(summary = "能流分析")
    public CommonResult<Map<String, Object>> energyFlowAnalysis(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsService.energyFlowAnalysis(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/standardCoalAnalysisTable")
    @Operation(summary = "折标煤分析（表）")
    public CommonResult<Map<String, Object>> standardCoalAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsService.standardCoalAnalysisTable(paramVO));
    }

    @PostMapping("/standardCoalAnalysisChart")
    @Operation(summary = "折标煤分析（图）")
    public CommonResult<Object> standardCoalAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsService.standardCoalAnalysisChart(paramVO));
    }

    @PostMapping("/moneyAnalysisTable")
    @Operation(summary = "折价分析（表）")
    public CommonResult<Map<String, Object>> moneyAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsService.moneyAnalysisTable(paramVO));
    }


    @PostMapping("/moneyAnalysisChart")
    @Operation(summary = "折价分析（图）")
    public CommonResult<Object> moneyAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsService.moneyAnalysisChart(paramVO));
    }

    @PostMapping("/standardCoalMomAnalysisTable")
    @Operation(summary = "环比-折标煤用量环比分析")
    public CommonResult<Map<String, Object>> standardCoalMomAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.standardCoalMomAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/moneyMomAnalysisTable")
    @Operation(summary = "环比-折价环比分析")
    public CommonResult<Map<String, Object>> moneyMomAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.moneyMomAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/utilizationRatioMomAnalysisTable")
    @Operation(summary = "环比-利用率环比分析")
    public CommonResult<Map<String, Object>> utilizationRatioMomAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.utilizationRatioMomAnalysisTable(paramVO);
        return success(jsonObject);
    }
}