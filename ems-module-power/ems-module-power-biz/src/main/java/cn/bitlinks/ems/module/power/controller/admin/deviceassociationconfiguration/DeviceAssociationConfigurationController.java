package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration;

import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
import javax.validation.*;
import javax.servlet.http.*;
import java.util.*;
import java.io.IOException;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.*;

import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration.DeviceAssociationConfigurationDO;
import cn.bitlinks.ems.module.power.service.deviceassociationconfiguration.DeviceAssociationConfigurationService;

@Tag(name = "管理后台 - 设备关联配置")
@RestController
@RequestMapping("/power/device-association-configuration")
@Validated
public class DeviceAssociationConfigurationController {

    @Resource
    private DeviceAssociationConfigurationService deviceAssociationConfigurationService;

    @PostMapping("/create")
    @Operation(summary = "创建设备关联配置")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:create')")
    public CommonResult<Long> createDeviceAssociationConfiguration(@Valid @RequestBody DeviceAssociationConfigurationSaveReqVO createReqVO) {
        return success(deviceAssociationConfigurationService.createDeviceAssociationConfiguration(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新设备关联配置")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:update')")
    public CommonResult<Boolean> updateDeviceAssociationConfiguration(@Valid @RequestBody DeviceAssociationConfigurationSaveReqVO updateReqVO) {
        deviceAssociationConfigurationService.updateDeviceAssociationConfiguration(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除设备关联配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:delete')")
    public CommonResult<Boolean> deleteDeviceAssociationConfiguration(@RequestParam("id") Long id) {
        deviceAssociationConfigurationService.deleteDeviceAssociationConfiguration(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得设备关联配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:query')")
    public CommonResult<DeviceAssociationConfigurationRespVO> getDeviceAssociationConfiguration(@RequestParam("id") Long id) {
        DeviceAssociationConfigurationDO deviceAssociationConfiguration = deviceAssociationConfigurationService.getDeviceAssociationConfiguration(id);
        return success(BeanUtils.toBean(deviceAssociationConfiguration, DeviceAssociationConfigurationRespVO.class));
    }

    @GetMapping("/get-by-measurement-instrument-id")
    @Operation(summary = "通过计量器具ID获得设备关联配置")
    @Parameter(name = "measurementInstrumentId", description = "计量器具ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:query')")
    public CommonResult<DeviceAssociationConfigurationRespVO> getDeviceAssociationConfigurationByMeasurementInstrumentId(@RequestParam("measurementInstrumentId") Long measurementInstrumentId) {
        DeviceAssociationConfigurationDO deviceAssociationConfiguration = deviceAssociationConfigurationService.getDeviceAssociationConfigurationByMeasurementInstrumentId(measurementInstrumentId);
        return CommonResult.success(BeanUtils.toBean(deviceAssociationConfiguration, DeviceAssociationConfigurationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得设备关联配置分页")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:query')")
    public CommonResult<PageResult<DeviceAssociationConfigurationRespVO>> getDeviceAssociationConfigurationPage(@Valid DeviceAssociationConfigurationPageReqVO pageReqVO) {
        PageResult<DeviceAssociationConfigurationDO> pageResult = deviceAssociationConfigurationService.getDeviceAssociationConfigurationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DeviceAssociationConfigurationRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备关联配置 Excel")
    @PreAuthorize("@ss.hasPermission('power:device-association-configuration:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDeviceAssociationConfigurationExcel(@Valid DeviceAssociationConfigurationPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DeviceAssociationConfigurationDO> list = deviceAssociationConfigurationService.getDeviceAssociationConfigurationPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "设备关联配置.xls", "数据", DeviceAssociationConfigurationRespVO.class,
                        BeanUtils.toBean(list, DeviceAssociationConfigurationRespVO.class));
    }

}