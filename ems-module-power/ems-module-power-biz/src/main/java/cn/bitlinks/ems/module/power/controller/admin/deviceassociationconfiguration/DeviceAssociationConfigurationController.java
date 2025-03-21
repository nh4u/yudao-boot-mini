package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.DeviceAssociationSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.MeasurementAssociationSaveReqVO;
import cn.bitlinks.ems.module.power.service.deviceassociationconfiguration.DeviceAssociationConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备关联配置")
@RestController
@RequestMapping("/power/device-association-configuration")
@Validated
public class DeviceAssociationConfigurationController {

    @Resource
    private DeviceAssociationConfigurationService deviceAssociationConfigurationService;

    @PostMapping("/measurementInstrument")
    @Operation(summary = "关联下级计量")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:update')")
    public CommonResult<Boolean> updAssociationMeasurementInstrument(@Valid @RequestBody MeasurementAssociationSaveReqVO createReqVO) {
        deviceAssociationConfigurationService.updAssociationMeasurementInstrument(createReqVO);
        return success(true);
    }

    @PostMapping("/device")
    @Operation(summary = "关联设备")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:update')")
    public CommonResult<Boolean> updAssociationDevice(@Valid @RequestBody DeviceAssociationSaveReqVO createReqVO) {
        deviceAssociationConfigurationService.updAssociationDevice(createReqVO);
        return success(true);
    }


}