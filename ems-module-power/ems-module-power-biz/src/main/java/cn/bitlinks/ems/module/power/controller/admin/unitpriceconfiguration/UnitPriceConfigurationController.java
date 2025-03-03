package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration;

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

import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import cn.bitlinks.ems.module.power.service.unitpriceconfiguration.UnitPriceConfigurationService;

@Tag(name = "管理后台 - 单价配置")
@RestController
@RequestMapping("/power/unit-price-configuration")
@Validated
public class UnitPriceConfigurationController {

    @Resource
    private UnitPriceConfigurationService unitPriceConfigurationService;

    @PostMapping("/create")
    @Operation(summary = "创建单价配置")
    @PreAuthorize("@ss.hasPermission('power:unit-price-configuration:create')")
    public CommonResult<List<Long>> createUnitPriceConfigurations(@Valid @RequestBody UnitPriceConfigurationBatchSaveReqVO batchSaveReqVO) {
        return success(unitPriceConfigurationService.createUnitPriceConfigurations(batchSaveReqVO.getEnergyId(), batchSaveReqVO.getList()));
    }

    @PutMapping("/update")
    @Operation(summary = "更新单价配置")
    @PreAuthorize("@ss.hasPermission('power:unit-price-configuration:update')")
    public CommonResult<List<Long>> updateUnitPriceConfiguration(@Valid @RequestBody UnitPriceConfigurationBatchSaveReqVO batchSaveReqVO) {
        return success(unitPriceConfigurationService.updateUnitPriceConfiguration(batchSaveReqVO.getEnergyId(), batchSaveReqVO.getList()));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除单价配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:unit-price-configuration:delete')")
    public CommonResult<Boolean> deleteUnitPriceConfiguration(@RequestParam("id") Long id) {
        unitPriceConfigurationService.deleteUnitPriceConfiguration(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得单价配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:unit-price-configuration:query')")
    public CommonResult<UnitPriceConfigurationRespVO> getUnitPriceConfiguration(@RequestParam("id") Long id) {
        UnitPriceConfigurationDO unitPriceConfiguration = unitPriceConfigurationService.getUnitPriceConfiguration(id);
        return success(BeanUtils.toBean(unitPriceConfiguration, UnitPriceConfigurationRespVO.class));
    }

    @GetMapping("/get-by-energy-id")
    @Operation(summary = "根据能源ID获得单价配置列表")
    @Parameter(name = "energyId", description = "能源ID", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:unit-price-configuration:query')")
    public CommonResult<List<UnitPriceConfigurationRespVO>> getUnitPriceConfigurationByEnergyId(@RequestParam("energyId") Long energyId) {
        List<UnitPriceConfigurationDO> unitPriceConfigurations = unitPriceConfigurationService.getUnitPriceConfigurationByEnergyId(energyId);
        List<UnitPriceConfigurationRespVO> result = BeanUtils.toBean(unitPriceConfigurations, UnitPriceConfigurationRespVO.class);
        return success(result);
    }

    @GetMapping("/page")
    @Operation(summary = "获得单价配置分页")
    @PreAuthorize("@ss.hasPermission('power:unit-price-configuration:query')")
    public CommonResult<PageResult<UnitPriceConfigurationRespVO>> getUnitPriceConfigurationPage(@Valid UnitPriceConfigurationPageReqVO pageReqVO) {
        PageResult<UnitPriceConfigurationDO> pageResult = unitPriceConfigurationService.getUnitPriceConfigurationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, UnitPriceConfigurationRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出单价配置 Excel")
    @PreAuthorize("@ss.hasPermission('power:unit-price-configuration:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUnitPriceConfigurationExcel(@Valid UnitPriceConfigurationPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<UnitPriceConfigurationDO> list = unitPriceConfigurationService.getUnitPriceConfigurationPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "单价配置.xls", "数据", UnitPriceConfigurationRespVO.class,
                        BeanUtils.toBean(list, UnitPriceConfigurationRespVO.class));
    }

}