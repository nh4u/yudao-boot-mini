package cn.bitlinks.ems.module.power.controller.admin.chemicals;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.chemicals.PowerChemicalsSettingsDO;
import cn.bitlinks.ems.module.power.service.chemicals.PowerChemicalsSettingsService;
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
@Tag(name = "管理后台 - 化学品设置")
@RestController
@RequestMapping("/power/powerChemicalsSettings")
@Validated
public class PowerChemicalsSettingsController {
    @Resource
    private PowerChemicalsSettingsService powerChemicalsSettingsService;

    @PutMapping("/updateBatch")
    @Operation(summary = "更新化学品设置批量")
    //@PreAuthorize("@ss.hasPermission('power:power_pure_waste_water_gas_settings:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody List<PowerChemicalsSettingsSaveReqVO> powerPureWasteWaterGasSettingsList) {
        powerChemicalsSettingsService.updateBatch(powerPureWasteWaterGasSettingsList);
        return success(true);
    }


    @GetMapping("/list")
    @Operation(summary = "获得化学品设置List")
    //@PreAuthorize("@ss.hasPermission('power:power_pure_waste_water_gas_settings:query')")
    public CommonResult<List<PowerChemicalsSettingsRespVO>> getPowerChemicalsSettingsList(@Valid PowerChemicalsSettingsPageReqVO pageReqVO) {
        List<PowerChemicalsSettingsDO> list = powerChemicalsSettingsService.getPowerChemicalsSettingsList(pageReqVO);
        return success(BeanUtils.toBean(list, PowerChemicalsSettingsRespVO.class));
    }

}
