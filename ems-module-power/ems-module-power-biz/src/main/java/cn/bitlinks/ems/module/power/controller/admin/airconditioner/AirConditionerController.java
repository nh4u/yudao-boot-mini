package cn.bitlinks.ems.module.power.controller.admin.airconditioner;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.airconditioner.vo.AirConditionerSettingsReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartYInfo;
import cn.bitlinks.ems.module.power.service.airconditioner.AirConditionerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 空调工况报表设置表")
@RestController
@RequestMapping("/power/air-conditioner-settings")
@Validated
public class AirConditionerController {


    @Resource
    private AirConditionerService airConditionerService;

    @GetMapping("/getOptions")
    @Operation(summary = "统计项下拉")
    public CommonResult<List<String>> getOptions() {
        return success(airConditionerService.getOptions());
    }


    @PostMapping("/chart")
    @Operation(summary = "图")
    public CommonResult<ConsumptionStatisticsChartResultVO<ConsumptionStatisticsChartYInfo>> getChart(@Valid @RequestBody AirConditionerSettingsReqVO paramVO) {
        return success(airConditionerService.getChart(paramVO));
    }

}
