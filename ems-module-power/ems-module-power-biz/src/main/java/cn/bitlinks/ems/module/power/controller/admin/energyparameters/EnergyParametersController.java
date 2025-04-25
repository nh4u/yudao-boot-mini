package cn.bitlinks.ems.module.power.controller.admin.energyparameters;

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

import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.service.energyparameters.EnergyParametersService;

@Tag(name = "管理后台 - 能源参数")
@RestController
@RequestMapping("/power/energy-parameters")
@Validated
public class EnergyParametersController {

    @Resource
    private EnergyParametersService energyParametersService;

    @PostMapping("/create")
    @Operation(summary = "创建能源参数")
    @PreAuthorize("@ss.hasPermission('power:energy-parameters:create')")
    public CommonResult<Long> createEnergyParameters(@Valid @RequestBody EnergyParametersSaveReqVO createReqVO) {
        return success(energyParametersService.createEnergyParameters(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新能源参数")
    @PreAuthorize("@ss.hasPermission('power:energy-parameters:update')")
    public CommonResult<Boolean> updateEnergyParameters(@Valid @RequestBody EnergyParametersSaveReqVO updateReqVO) {
        energyParametersService.updateEnergyParameters(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除能源参数")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:energy-parameters:delete')")
    public CommonResult<Boolean> deleteEnergyParameters(@RequestParam("id") Long id) {
        energyParametersService.deleteEnergyParameters(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得能源参数")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:energy-parameters:query')")
    public CommonResult<EnergyParametersRespVO> getEnergyParameters(@RequestParam("id") Long id) {
        EnergyParametersDO energyParameters = energyParametersService.getEnergyParameters(id);
        return success(BeanUtils.toBean(energyParameters, EnergyParametersRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得能源参数分页")
    @PreAuthorize("@ss.hasPermission('power:energy-parameters:query')")
    public CommonResult<PageResult<EnergyParametersRespVO>> getEnergyParametersPage(@Valid EnergyParametersPageReqVO pageReqVO) {
        PageResult<EnergyParametersDO> pageResult = energyParametersService.getEnergyParametersPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, EnergyParametersRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出能源参数 Excel")
    @PreAuthorize("@ss.hasPermission('power:energy-parameters:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEnergyParametersExcel(@Valid EnergyParametersPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<EnergyParametersDO> list = energyParametersService.getEnergyParametersPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "能源参数.xls", "数据", EnergyParametersRespVO.class,
                        BeanUtils.toBean(list, EnergyParametersRespVO.class));
    }

    @GetMapping("/getByEnergyId")
    @Operation(summary = "根据能源id获得能源参数")
    @Parameter(name = "energyId", description = "能源id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:energy-parameters:getByEnergyId')")
    public CommonResult<List<EnergyParametersRespVO>> getEnergyParametersByEnergyId(@RequestParam("energyId") Long energyId) {
        List<EnergyParametersDO> energyParameters = energyParametersService.getEnergyParametersByEnergyId(energyId);
        return success(BeanUtils.toBean(energyParameters, EnergyParametersRespVO.class));
    }

}