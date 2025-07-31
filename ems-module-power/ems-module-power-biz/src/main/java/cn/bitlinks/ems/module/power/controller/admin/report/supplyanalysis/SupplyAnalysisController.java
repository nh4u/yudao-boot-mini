package cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplyanalysis.SupplyAnalysisSettingsDO;
import cn.bitlinks.ems.module.power.service.report.supplyanalysis.SupplyAnalysisSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/06/21 19:23
 **/

@Tag(name = "管理后台 - 供应分析报表")
@RestController
@RequestMapping("/power/supplyAnalysis")
@Validated
public class SupplyAnalysisController {


    @Resource
    private SupplyAnalysisSettingsService supplyAnalysisSettingsService;

    @PutMapping("/updateBatch")
    @Operation(summary = "更新供应分析设置批量")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_analysis_settings:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody List<SupplyAnalysisSettingsSaveReqVO> supplyAnalysisSettingsList) {
        supplyAnalysisSettingsService.updateBatch(supplyAnalysisSettingsList);
        return success(true);
    }


    @GetMapping("/list")
    @Operation(summary = "获得供应分析设置List")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_analysis_settings:query')")
    public CommonResult<List<SupplyAnalysisSettingsRespVO>> getSupplyAnalysisSettingsList(@Valid SupplyAnalysisSettingsPageReqVO pageReqVO) {
        List<SupplyAnalysisSettingsDO> list = supplyAnalysisSettingsService.getSupplyAnalysisSettingsList(pageReqVO);
        return success(BeanUtils.toBean(list, SupplyAnalysisSettingsRespVO.class));
    }

    @GetMapping("/getSystem")
    @Operation(summary = "获得供应分析系统")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_analysis_settings:query')")
    public CommonResult<List<String>> getSystem() {
        List<String> list = supplyAnalysisSettingsService.getSystem();
        return success(list);
    }


    @PostMapping("/supplyAnalysisTable")
    @Operation(summary = "供应分析表")
    public CommonResult<StatisticsResultV2VO> supplyAnalysisTable(@Valid @RequestBody SupplyAnalysisReportParamVO paramVO) {
        return success(supplyAnalysisSettingsService.supplyAnalysisTable(paramVO));
    }

//    @PostMapping("/supplyAnalysisChart")
//    @Operation(summary = "供应分析图")
//    public CommonResult<CopChartResultVO> supplyAnalysisChart(@Valid @RequestBody SupplyAnalysisReportParamVO paramVO) {
//        return success(supplyAnalysisSettingsService.supplyAnalysisChart(paramVO));
//    }

}
