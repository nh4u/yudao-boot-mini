package cn.bitlinks.ems.module.power.controller.admin.unitpricehistory;

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

import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpricehistory.UnitPriceHistoryDO;
import cn.bitlinks.ems.module.power.service.unitpricehistory.UnitPriceHistoryService;

@Tag(name = "管理后台 - 单价历史")
@RestController
@RequestMapping("/power/unit-price-history")
@Validated
public class UnitPriceHistoryController {

    @Resource
    private UnitPriceHistoryService unitPriceHistoryService;

    @PostMapping("/create")
    @Operation(summary = "创建单价历史")
    @PreAuthorize("@ss.hasPermission('power:unit-price-history:create')")
    public CommonResult<Long> createUnitPriceHistory(@Valid @RequestBody UnitPriceHistorySaveReqVO createReqVO) {
        return success(unitPriceHistoryService.createUnitPriceHistory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新单价历史")
    @PreAuthorize("@ss.hasPermission('power:unit-price-history:update')")
    public CommonResult<Boolean> updateUnitPriceHistory(@Valid @RequestBody UnitPriceHistorySaveReqVO updateReqVO) {
        unitPriceHistoryService.updateUnitPriceHistory(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除单价历史")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:unit-price-history:delete')")
    public CommonResult<Boolean> deleteUnitPriceHistory(@RequestParam("id") Long id) {
        unitPriceHistoryService.deleteUnitPriceHistory(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得单价历史")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:unit-price-history:query')")
    public CommonResult<UnitPriceHistoryRespVO> getUnitPriceHistory(@RequestParam("id") Long id) {
        UnitPriceHistoryDO unitPriceHistory = unitPriceHistoryService.getUnitPriceHistory(id);
        return success(BeanUtils.toBean(unitPriceHistory, UnitPriceHistoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得单价历史分页")
    @PreAuthorize("@ss.hasPermission('power:unit-price-history:query')")
    public CommonResult<PageResult<UnitPriceHistoryRespVO>> getUnitPriceHistoryPage(@Valid UnitPriceHistoryPageReqVO pageReqVO) {
        PageResult<UnitPriceHistoryDO> pageResult = unitPriceHistoryService.getUnitPriceHistoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, UnitPriceHistoryRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出单价历史 Excel")
    @PreAuthorize("@ss.hasPermission('power:unit-price-history:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUnitPriceHistoryExcel(@Valid UnitPriceHistoryPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<UnitPriceHistoryDO> list = unitPriceHistoryService.getUnitPriceHistoryPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "单价历史.xls", "数据", UnitPriceHistoryRespVO.class,
                        BeanUtils.toBean(list, UnitPriceHistoryRespVO.class));
    }

}