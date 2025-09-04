package cn.bitlinks.ems.module.power.controller.admin.devicemonitor;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.devicemonitor.vo.DeviceMonitorDeviceReqVO;
import cn.bitlinks.ems.module.power.controller.admin.devicemonitor.vo.DeviceMonitorDeviceRespVO;
import cn.bitlinks.ems.module.power.controller.admin.devicemonitor.vo.DeviceMonitorWarningReqVO;
import cn.bitlinks.ems.module.power.controller.admin.devicemonitor.vo.DeviceMonitorWarningRespVO;
import cn.bitlinks.ems.module.power.service.devicemonitor.DeviceMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;


import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 设备监控")
@RestController
@RequestMapping("/power/device-monitor")
@Validated
public class DeviceMonitorController {
    @Resource
    private DeviceMonitorService deviceMonitorService;

    @PostMapping("/warning")
    @Operation(summary = "查询告警信息")
    public CommonResult<DeviceMonitorWarningRespVO> getWarningInfo(@Valid DeviceMonitorWarningReqVO reqVO) {
        return success(deviceMonitorService.getWarningInfo(reqVO));
    }

    @PostMapping("/deviceInfo")
    @Operation(summary = "根据设备id查询设备名片")
    public CommonResult<DeviceMonitorDeviceRespVO> getDeviceInfo(@Valid DeviceMonitorDeviceReqVO reqVO) {
        return success(deviceMonitorService.getDeviceInfo(reqVO));
    }
}
