package cn.bitlinks.ems.module.power.controller.admin.bigscreen;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerMonthPlanSettingsDO;
import cn.bitlinks.ems.module.power.service.bigscreen.PowerMonthPlanSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 本月计划设置")
@RestController
@RequestMapping("/power/monthPlan")
@Validated
public class PowerMonthPlanSettingsController {
    @Resource
    private PowerMonthPlanSettingsService powerMonthPlanSettingsService;

    @PutMapping("/updateBatch")
    @Operation(summary = "更新本月计划设置批量")
    //@PreAuthorize("@ss.hasPermission('power:power_month_plan_settings:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody List<PowerMonthPlanSettingsSaveReqVO> powerMonthPlanSettingsList) {
        powerMonthPlanSettingsService.updateBatch(powerMonthPlanSettingsList);
        return success(true);
    }


    @GetMapping("/list")
    @Operation(summary = "获得本月计划设置List")
    //@PreAuthorize("@ss.hasPermission('power:power_month_plan_settings:query')")
    public CommonResult<List<PowerMonthPlanSettingsRespVO>> getPowerMonthPlanSettingsList(@Valid PowerMonthPlanSettingsPageReqVO pageReqVO) {
        List<PowerMonthPlanSettingsDO> list = powerMonthPlanSettingsService.getPowerMonthPlanSettingsList(pageReqVO);
        return success(BeanUtils.toBean(list, PowerMonthPlanSettingsRespVO.class));
    }

}
