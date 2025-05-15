package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.service.statistics.StandardCoalV2Service;
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

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 用能分析")
@RestController
@RequestMapping("/power/statistics/standardCoal/v2")
@Validated
public class StandardCoalStatisticsV2Controller {

    @Resource
    private StandardCoalV2Service standardCoalV2Service;


    @PostMapping("/standardCoalAnalysisTable")
    @Operation(summary = "折标煤分析（表）")
    public CommonResult<StatisticsResultV2VO<StandardCoalInfo>> standardCoalAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(standardCoalV2Service.standardCoalAnalysisTable(paramVO));
    }


    @PostMapping("/standardCoalAnalysisChart")
    @Operation(summary = "折标煤分析（图）")
    public CommonResult<StatisticsResultV2VO<StatisticsInfoV2>> standardCoalAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(standardCoalV2Service.standardCoalAnalysisChart(paramVO));
    }

}