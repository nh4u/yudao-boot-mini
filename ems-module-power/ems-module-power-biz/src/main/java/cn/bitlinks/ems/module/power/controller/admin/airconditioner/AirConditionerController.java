package cn.bitlinks.ems.module.power.controller.admin.airconditioner;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.service.airconditioner.AirConditionerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
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

}
