package cn.bitlinks.ems.module.power.controller.admin.servicesettings;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.servicesettings.ServiceSettingsDO;
import cn.bitlinks.ems.module.power.service.servicesettings.ServiceSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 服务设置")
@RestController
@RequestMapping("/power/service-settings")
@Validated
public class ServiceSettingsController {

    @Resource
    private ServiceSettingsService serviceSettingsService;

    @PostMapping("/create")
    @Operation(summary = "创建服务设置")
    @PreAuthorize("@ss.hasPermission('power:service-settings:create')")
    public CommonResult<Long> createServiceSettings(@Valid @RequestBody ServiceSettingsSaveReqVO createReqVO) {
        return success(serviceSettingsService.createServiceSettings(createReqVO));
    }

    @PostMapping("/test")
    @Operation(summary = "测试服务连通")
    @PreAuthorize("@ss.hasPermission('power:service-settings:create')")
    public CommonResult<Boolean> testLink(@Valid @RequestBody ServiceSettingsTestReqVO createReqVO) {
        return success(serviceSettingsService.testLink(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新服务设置")
    @PreAuthorize("@ss.hasPermission('power:service-settings:update')")
    public CommonResult<Boolean> updateServiceSettings(@Valid @RequestBody ServiceSettingsSaveReqVO updateReqVO) {
        serviceSettingsService.updateServiceSettings(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除服务设置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:service-settings:delete')")
    public CommonResult<Boolean> deleteServiceSettings(@RequestParam("id") Long id) {
        serviceSettingsService.deleteServiceSettings(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得服务设置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:service-settings:query')")
    public CommonResult<ServiceSettingsRespVO> getServiceSettings(@RequestParam("id") Long id) {
        ServiceSettingsDO serviceSettings = serviceSettingsService.getServiceSettings(id);
        return success(BeanUtils.toBean(serviceSettings, ServiceSettingsRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得服务设置全部列表")
    @PreAuthorize("@ss.hasPermission('power:service-settings:query')")
    public CommonResult<List<ServiceSettingsOptionsRespVO>> getServiceSettingsList() {
        return success(serviceSettingsService.getServiceSettingsList());
    }


    @GetMapping("/page")
    @Operation(summary = "获得服务设置分页")
    @PreAuthorize("@ss.hasPermission('power:service-settings:query')")
    public CommonResult<PageResult<ServiceSettingsRespVO>> getServiceSettingsPage(@Valid ServiceSettingsPageReqVO pageReqVO) {
        PageResult<ServiceSettingsDO> pageResult = serviceSettingsService.getServiceSettingsPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ServiceSettingsRespVO.class));
    }

}