package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsChartPieResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StructureInfo;
import cn.bitlinks.ems.module.power.service.statistics.MoneyStructureV2Service;
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
@RequestMapping("/power/statistics/money/v2")
@Validated
public class MoneyStatisticsV2Controller {

    @Resource
    private MoneyStructureV2Service moneyStructureV2Service;


    @PostMapping("/moneyStructureAnalysisTable")
    @Operation(summary = "价格结构分析（表）V2")
    public CommonResult<StatisticsResultV2VO<StructureInfo>> moneyStructureAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(moneyStructureV2Service.moneyStructureAnalysisTable(paramVO));
    }

    @PostMapping("/moneyStructureAnalysisChart")
    @Operation(summary = "价格结构分析（图）V2")
    public CommonResult<StatisticsChartPieResultVO> moneyStructureAnalysisChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(moneyStructureV2Service.moneyStructureAnalysisChart(paramVO));
    }
}