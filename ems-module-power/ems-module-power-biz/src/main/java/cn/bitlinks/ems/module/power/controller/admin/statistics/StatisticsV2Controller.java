package cn.bitlinks.ems.module.power.controller.admin.statistics;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsOverviewResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsOverviewService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsRatioService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsStructureService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 用能分析")
@RestController
@RequestMapping("/power/statistics/v2")
@Validated
public class StatisticsV2Controller {

    @Resource
    private StatisticsV2Service statisticsV2Service;


    @PostMapping("/moneyAnalysisTable")
    @Operation(summary = "折价分析（表）")
    public CommonResult<StatisticsResultV2VO> moneyAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(statisticsV2Service.moneyAnalysisTable(paramVO));
    }


 /*   @PostMapping("/moneyAnalysisChart")
    @Operation(summary = "折价分析（图）")
    public CommonResult<Object> moneyAnalysisChart(@Valid @RequestBody StatisticsParamVO paramVO) {
        return success(statisticsV2Service.moneyAnalysisTable(paramVO));
    }*/

}