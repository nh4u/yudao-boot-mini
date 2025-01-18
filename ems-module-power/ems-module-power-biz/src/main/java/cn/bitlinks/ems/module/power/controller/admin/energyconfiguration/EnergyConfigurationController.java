package cn.bitlinks.ems.module.power.controller.admin.energyconfiguration;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationRespVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 能源配置")
@RestController
@RequestMapping("/power/energy-configuration")
@Validated
public class EnergyConfigurationController {

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @PostMapping("/create")
    @Operation(summary = "创建能源配置")
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:create')")
    public CommonResult<Long> createEnergyConfiguration(@Valid @RequestBody EnergyConfigurationSaveReqVO createReqVO) {
        return success(energyConfigurationService.createEnergyConfiguration(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新能源配置")
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:update')")
    public CommonResult<Boolean> updateEnergyConfiguration(@Valid @RequestBody EnergyConfigurationSaveReqVO updateReqVO) {
        energyConfigurationService.updateEnergyConfiguration(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除能源配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:delete')")
    public CommonResult<Boolean> deleteEnergyConfiguration(@RequestParam("id") Long id) {
        energyConfigurationService.deleteEnergyConfiguration(id);
        return success(true);
    }

    @DeleteMapping("/deleteIds")
    @Operation(summary = "批量删除能源配置")
    @Parameter(name = "ids", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:deleteIds')")
    public CommonResult<Boolean> deleteEnergyConfigurations(@RequestBody List<Long> ids) {
        energyConfigurationService.deleteEnergyConfigurations(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得能源配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:query')")
    public CommonResult<EnergyConfigurationRespVO> getEnergyConfiguration(@RequestParam("id") Long id) {
        EnergyConfigurationDO energyConfiguration = energyConfigurationService.getEnergyConfiguration(id);
        return success(BeanUtils.toBean(energyConfiguration, EnergyConfigurationRespVO.class));
    }

    @PostMapping("/page")
    @Operation(summary = "获得能源配置分页")
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:query')")
    public CommonResult<PageResult<EnergyConfigurationRespVO>> getEnergyConfigurationPage(@Valid @RequestBody EnergyConfigurationPageReqVO pageReqVO) {
        PageResult<EnergyConfigurationDO> pageResult = energyConfigurationService.getEnergyConfigurationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, EnergyConfigurationRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出能源配置 Excel")
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEnergyConfigurationExcel(@Valid EnergyConfigurationPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<EnergyConfigurationDO> list = energyConfigurationService.getEnergyConfigurationPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "能源配置.xls", "数据", EnergyConfigurationRespVO.class,
                        BeanUtils.toBean(list, EnergyConfigurationRespVO.class));
    }


    @GetMapping("/searchEnergyConfigurations")
    @Operation(summary = "根据条件查询能源配置")
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:searchEnergyConfigurations')")
    @ApiAccessLog(operateType = EXPORT)
    public CommonResult<List<EnergyConfigurationDO>> searchEnergyConfigurations(
            @RequestParam(required = false) String energyName,
            @RequestParam(required = false) String energyClassify,
            @RequestParam(required = false) String code) {
        return success(energyConfigurationService.selectByCondition(energyName, energyClassify, code));
    }
//return success(BeanUtils.toBean(pageResult, EnergyConfigurationRespVO.class));
    @GetMapping("/getStatisticsEnergy")
    @Operation(summary = "用能分析下【统计能源条件】接口 1:外购2:园区")
    @PreAuthorize("@ss.hasPermission('power:energy-configuration:query')")
    public CommonResult<Map<Integer, List<EnergyConfigurationDO>>> getEnergyMenu() {
        return success(energyConfigurationService.getEnergyMenu());
    }
}