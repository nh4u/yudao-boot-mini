package cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory;

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

import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import cn.bitlinks.ems.module.power.service.coalfactorhistory.CoalFactorHistoryService;

@Tag(name = "管理后台 - 折标煤系数历史")
@RestController
@RequestMapping("/power/coal-factor-history")
@Validated
public class CoalFactorHistoryController {

    @Resource
    private CoalFactorHistoryService coalFactorHistoryService;

    @PostMapping("/create")
    @Operation(summary = "创建折标煤系数历史")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:create')")
    public CommonResult<Long> createCoalFactorHistory(@Valid @RequestBody CoalFactorHistorySaveReqVO createReqVO) {
        return success(coalFactorHistoryService.createCoalFactorHistory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新折标煤系数历史")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:update')")
    public CommonResult<Boolean> updateCoalFactorHistory(@Valid @RequestBody CoalFactorHistorySaveReqVO updateReqVO) {
        coalFactorHistoryService.updateCoalFactorHistory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除折标煤系数历史")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:delete')")
    public CommonResult<Boolean> deleteCoalFactorHistory(@RequestParam("id") Long id) {
        coalFactorHistoryService.deleteCoalFactorHistory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得折标煤系数历史")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:query')")
    public CommonResult<CoalFactorHistoryRespVO> getCoalFactorHistory(@RequestParam("id") Long id) {
        CoalFactorHistoryDO coalFactorHistory = coalFactorHistoryService.getCoalFactorHistory(id);
        return success(BeanUtils.toBean(coalFactorHistory, CoalFactorHistoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得折标煤系数历史分页")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:query')")
    public CommonResult<PageResult<CoalFactorHistoryRespVO>> getCoalFactorHistoryPage(@Valid CoalFactorHistoryPageReqVO pageReqVO) {
        PageResult<CoalFactorHistoryDO> pageResult = coalFactorHistoryService.getCoalFactorHistoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CoalFactorHistoryRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出折标煤系数历史 Excel")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCoalFactorHistoryExcel(@Valid CoalFactorHistoryPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<CoalFactorHistoryDO> list = coalFactorHistoryService.getCoalFactorHistoryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "折标煤系数历史.xls", "数据", CoalFactorHistoryRespVO.class,
                        BeanUtils.toBean(list, CoalFactorHistoryRespVO.class));
    }

}