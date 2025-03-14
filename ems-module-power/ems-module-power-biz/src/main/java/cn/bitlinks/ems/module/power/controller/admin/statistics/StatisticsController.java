package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsOverviewResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsOverviewService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsRatioService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsStructureService;
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

    @Resource
    private StatisticsStructureService statisticsStructureService;

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
    @Operation(summary = "环比-折标煤用量环比分析（表）")
    public CommonResult<Map<String, Object>> standardCoalMomAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.standardCoalMomAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/moneyMomAnalysisTable")
    @Operation(summary = "环比-折价环比分析（表）")
    public CommonResult<Map<String, Object>> moneyMomAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.moneyMomAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/utilizationRatioMomAnalysisTable")
    @Operation(summary = "环比-利用率环比分析（表）")
    public CommonResult<Map<String, Object>> utilizationRatioMomAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.utilizationRatioMomAnalysisTable(paramVO);
        return success(jsonObject);
    }


    @PostMapping("/standardCoalMomAnalysisChart")
    @Operation(summary = "环比-折标煤用量环比分析（图）")
    public CommonResult<Object> standardCoalMomAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.standardCoalMomAnalysisChart(paramVO));
    }

    @PostMapping("/moneyMomAnalysisChart")
    @Operation(summary = "环比-折价环比分析（图）")
    public CommonResult<Object> moneyMomAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.moneyMomAnalysisChart(paramVO));
    }

    @PostMapping("/utilizationRatioMomAnalysisChart")
    @Operation(summary = "环比-利用率环比分析（图）")
    public CommonResult<Object> utilizationRatioMomAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.utilizationRatioMomAnalysisChart(paramVO));
    }

    @PostMapping("/standardCoalBenchmarkAnalysisTable")
    @Operation(summary = "定基比-折标煤用量定基比分析（表）")
    public CommonResult<Map<String, Object>> standardCoalBenchmarkAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.standardCoalBenchmarkAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/moneyBenchmarkAnalysisTable")
    @Operation(summary = "定基比-折价定基比分析（表）")
    public CommonResult<Map<String, Object>> moneyBenchmarkAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.moneyBenchmarkAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/utilizationRatioBenchmarkAnalysisTable")
    @Operation(summary = "定基比-利用率定基比分析（表）")
    public CommonResult<Map<String, Object>> utilizationRatioBenchmarkAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.utilizationRatioBenchmarkAnalysisTable(paramVO);
        return success(jsonObject);
    }


    @PostMapping("/standardCoalBenchmarkAnalysisChart")
    @Operation(summary = "定基比-折标煤用量定基比分析（图）")
    public CommonResult<Object> standardCoalBenchmarkAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.standardCoalBenchmarkAnalysisChart(paramVO));
    }

    @PostMapping("/moneyBenchmarkAnalysisChart")
    @Operation(summary = "定基比-折价定基比分析（图）")
    public CommonResult<Object> moneyBenchmarkAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.moneyBenchmarkAnalysisChart(paramVO));
    }

    @PostMapping("/utilizationRatioBenchmarkAnalysisChart")
    @Operation(summary = "定基比-利用率定基比分析（图）")
    public CommonResult<Object> utilizationRatioBenchmarkAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.utilizationRatioBenchmarkAnalysisChart(paramVO));
    }

    @PostMapping("/standardCoalYoyAnalysisTable")
    @Operation(summary = "同比-折标煤用量同比分析（表）")
    public CommonResult<Map<String, Object>> standardCoalYoyAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.standardCoalYoyAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/moneyYoyAnalysisTable")
    @Operation(summary = "同比-折价同比分析（表）")
    public CommonResult<Map<String, Object>> moneyYoyAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.moneyYoyAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/utilizationRatioYoyAnalysisTable")
    @Operation(summary = "同比-利用率同比分析（表）")
    public CommonResult<Map<String, Object>> utilizationRatioYoyAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsRatioService.utilizationRatioYoyAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/standardCoalYoyAnalysisChart")
    @Operation(summary = "同比-折标煤用量同比分析（图）")
    public CommonResult<Object> standardCoalYoyAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.standardCoalYoyAnalysisChart(paramVO));
    }

    @PostMapping("/moneyYoyAnalysisChart")
    @Operation(summary = "同比-折价同比分析（图）")
    public CommonResult<Object> moneyYoyAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.moneyYoyAnalysisChart(paramVO));
    }

    @PostMapping("/utilizationRatioYoyAnalysisChart")
    @Operation(summary = "同比-利用率同比分析（图）")
    public CommonResult<Object> utilizationRatioYoyAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsRatioService.utilizationRatioYoyAnalysisChart(paramVO));
    }

    @PostMapping("/standardCoalStructureAnalysisTable")
    @Operation(summary = "用能结构分析（表）")
    public CommonResult<Map<String, Object>> standardCoalStructureAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsStructureService.standardCoalStructureAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/standardCoalStructureAnalysisChart")
    @Operation(summary = "用能结构分析（图）")
    public CommonResult<Object> standardCoalStructureAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsStructureService.standardCoalStructureAnalysisChart(paramVO));
    }

    @PostMapping("/standardMoneyStructureAnalysisTable")
    @Operation(summary = "价格结构分析（表）")
    public CommonResult<Map<String, Object>> standardMoneyStructureAnalysisTable(@Valid @RequestBody StatisticsParamVO paramVO) {
        Map<String, Object> jsonObject = statisticsStructureService.standardMoneyStructureAnalysisTable(paramVO);
        return success(jsonObject);
    }

    @PostMapping("/standardMoneyStructureAnalysisChart")
    @Operation(summary = "价格结构分析（图）")
    public CommonResult<Object> standardMoneyStructureAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsStructureService.standardMoneyStructureAnalysisChart(paramVO));
    }
}