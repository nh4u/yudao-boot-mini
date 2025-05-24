package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.service.statistics.*;
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

@Tag(name = "管理后台 - 首页")
@RestController
@RequestMapping("/power/statistics/home/v2")
@Validated
public class StatisticsHomeController {

    @Resource
    private StatisticsHomeService statisticsHomeService;

    @PostMapping("/overview")
    @Operation(summary = "统计总览")
    public CommonResult<StatisticsHomeResultVO> overview(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(statisticsHomeService.overview(paramVO));
    }

    @PostMapping("/costChart")
    @Operation(summary = "折价分析图")
    public CommonResult<ComparisonChartResultVO> costChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(statisticsHomeService.costChart(paramVO));
    }

    @PostMapping("/coalChart")
    @Operation(summary = "折标煤分析图")
    public CommonResult<ComparisonChartResultVO> coalChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(statisticsHomeService.coalChart(paramVO));
    }
}
