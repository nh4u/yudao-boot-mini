package cn.bitlinks.ems.module.power.controller.admin.bigscreen;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerPureWasteWaterGasSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import cn.bitlinks.ems.module.power.service.bigscreen.PowerPureWasteWaterGasSettingsService;
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
@Tag(name = "管理后台 - 纯废水压缩空气设置")
@RestController
@RequestMapping("/power/powerPureWasteWaterGasSettings")
@Validated
public class PowerPureWasteWaterGasSettingsController {
    @Resource
    private PowerPureWasteWaterGasSettingsService powerPureWasteWaterGasSettingsService;

    @PutMapping("/updateBatch")
    @Operation(summary = "更新纯废水压缩空气设置批量")
    //@PreAuthorize("@ss.hasPermission('power:power_pure_waste_water_gas_settings:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody List<PowerPureWasteWaterGasSettingsSaveReqVO> powerPureWasteWaterGasSettingsList) {
        powerPureWasteWaterGasSettingsService.updateBatch(powerPureWasteWaterGasSettingsList);
        return success(true);
    }


    @GetMapping("/list")
    @Operation(summary = "获得纯废水压缩空气设置List")
    //@PreAuthorize("@ss.hasPermission('power:power_pure_waste_water_gas_settings:query')")
    public CommonResult<List<PowerPureWasteWaterGasSettingsRespVO>> getPowerPureWasteWaterGasSettingsList(@Valid PowerPureWasteWaterGasSettingsPageReqVO pageReqVO) {
        List<PowerPureWasteWaterGasSettingsDO> list = powerPureWasteWaterGasSettingsService.getPowerPureWasteWaterGasSettingsList(pageReqVO);
        return success(BeanUtils.toBean(list, PowerPureWasteWaterGasSettingsRespVO.class));
    }

}
