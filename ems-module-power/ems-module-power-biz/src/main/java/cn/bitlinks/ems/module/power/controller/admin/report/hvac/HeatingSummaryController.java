package cn.bitlinks.ems.module.power.controller.admin.report.hvac;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportResultVO;
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


}
