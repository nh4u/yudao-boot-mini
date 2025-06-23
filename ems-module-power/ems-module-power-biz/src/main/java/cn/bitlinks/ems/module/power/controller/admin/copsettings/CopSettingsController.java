package cn.bitlinks.ems.module.power.controller.admin.copsettings;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopSettingsDO;
import cn.bitlinks.ems.module.power.service.copsettings.CopSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "管理后台 - 配置标签")
@RestController
@RequestMapping("/power/copSettings")
@Validated
public class CopSettingsController {

    @Resource
    private CopSettingsService copSettingsService;

    @PostMapping("/create")
    @Operation(summary = "创建COP参数配置")
    //@PreAuthorize("@ss.hasPermission('power:power_cop_settings:create')")
    public CommonResult<Long> createCopSettings(@Valid @RequestBody CopSettingsSaveReqVO createReqVO) {
        return success(copSettingsService.createCopSettings(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新COP参数配置")
    //@PreAuthorize("@ss.hasPermission('power:power_cop_settings:update')")
    public CommonResult<Boolean> updateCopSettings(@Valid @RequestBody CopSettingsSaveReqVO updateReqVO) {
        copSettingsService.updateCopSettings(updateReqVO);
        return success(true);
    }

    @PutMapping("/updateBatch")
    @Operation(summary = "更新COP参数配置批量")
    //@PreAuthorize("@ss.hasPermission('power:power_cop_settings:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody List<CopSettingsSaveReqVO> copSettingsList) {
        copSettingsService.updateBatch(copSettingsList);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除COP参数配置")
    @Parameter(name = "id", description = "编号", required = true)
    //@PreAuthorize("@ss.hasPermission('power:power_cop_settings:delete')")
    public CommonResult<Boolean> deleteCopSettings(@RequestParam("id") Long id) {
        copSettingsService.deleteCopSettings(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得单个COP参数配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    //@PreAuthorize("@ss.hasPermission('power:power_cop_settings:query')")
    public CommonResult<CopSettingsRespVO> getCopSettings(@RequestParam("id") Long id) {
        CopSettingsDO copSettingsDO = copSettingsService.getCopSettings(id);
        return success(BeanUtils.toBean(copSettingsDO, CopSettingsRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得COP参数配置分页")
    //@PreAuthorize("@ss.hasPermission('power:power_cop_settings:query')")
    public CommonResult<PageResult<CopSettingsRespVO>> getCopSettingsPage(@Valid CopSettingsPageReqVO pageReqVO) {
        PageResult<CopSettingsDO> pageResult = copSettingsService.getCopSettingsPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CopSettingsRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得COP参数配置List")
    //@PreAuthorize("@ss.hasPermission('power:power_cop_settings:query')")
    public CommonResult<List<CopSettingsRespVO>> getCopSettingsList(@Valid CopSettingsPageReqVO pageReqVO) {
        List<CopSettingsDO> list = copSettingsService.getCopSettingsListByCopType(pageReqVO);
        return success(BeanUtils.toBean(list, CopSettingsRespVO.class));
    }

}