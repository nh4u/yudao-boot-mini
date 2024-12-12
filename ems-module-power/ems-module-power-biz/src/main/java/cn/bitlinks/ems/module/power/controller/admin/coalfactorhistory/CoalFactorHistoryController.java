package cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.CoalFactorHistoryPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.CoalFactorHistoryRespVO;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.CoalFactorHistorySaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import cn.bitlinks.ems.module.power.service.coalfactorhistory.CoalFactorHistoryService;
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

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 折标煤系数历史")
@RestController
@RequestMapping("/power/coal-factor-history")
@Validated
public class CoalFactorHistoryController {

    @Resource
    private CoalFactorHistoryService coalFactorHistoryService;

//    @PostMapping("/create")
//    @Operation(summary = "创建折标煤系数历史")
//    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:create')")
//    public CommonResult<Long> createCoalFactorHistory(@Valid @RequestBody CoalFactorHistorySaveReqVO createReqVO) {
//        return success(coalFactorHistoryService.createCoalFactorHistory07(createReqVO));
//    }

    @PostMapping("/create")
    @Operation(summary = "创建折标煤系数历史")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:create')")
    public CommonResult<Long> createCoalFactorHistory(
            @Valid @RequestBody CoalFactorHistorySaveReqVO createReqVO,
            @RequestParam(value = "use3307", required = false, defaultValue = "false") boolean use3307) {
        Long id;
        if (use3307) {
            // 切换到 3307 数据源进行创建
            id = coalFactorHistoryService.createCoalFactorHistory07(createReqVO);
        } else {
            // 默认数据源创建
            id = coalFactorHistoryService.createCoalFactorHistory(createReqVO);
        }
        return success(id);
    }


//    @PutMapping("/update")
//    @Operation(summary = "更新折标煤系数历史")
//    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:update')")
//    public CommonResult<Boolean> updateCoalFactorHistory(@Valid @RequestBody CoalFactorHistorySaveReqVO updateReqVO) {
//        coalFactorHistoryService.updateCoalFactorHistory(updateReqVO);
//        return success(true);
//    }

    @PutMapping("/update")
    @Operation(summary = "更新折标煤系数历史")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:update')")
    public CommonResult<Boolean> updateCoalFactorHistory(@Valid @RequestBody CoalFactorHistorySaveReqVO updateReqVO,
                                                         @RequestParam(value = "use3307", required = false, defaultValue = "false") boolean use3307) {
        if (use3307) {
            // 切换到 3307 数据源更新
            coalFactorHistoryService.updateCoalFactorHistory07(updateReqVO);
        } else {
            // 默认数据源更新
            coalFactorHistoryService.updateCoalFactorHistory(updateReqVO);
        }
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除折标煤系数历史")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:delete')")
    public CommonResult<Boolean> deleteCoalFactorHistory(@RequestParam("id") Long id,
                                                         @RequestParam(value = "use3307", required = false, defaultValue = "false") boolean use3307) {
        if (use3307) {
            //切换到 3307 数据源
            coalFactorHistoryService.deleteCoalFactorHistory07(id);
        } else {
            //正常删除
            coalFactorHistoryService.deleteCoalFactorHistory(id);
        }
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得折标煤系数历史")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:coal-factor-history:query')")
    public CommonResult<CoalFactorHistoryRespVO> getCoalFactorHistory(@RequestParam("id") Long id,
                                                                      @RequestParam(value = "use3307", required = false, defaultValue = "false") boolean use3307) {
        CoalFactorHistoryDO coalFactorHistory;
        if (use3307) {
            //切换到 3307 数据源
            coalFactorHistory =coalFactorHistoryService.getCoalFactorHistory07(id);
        } else {
            //正常删除
            coalFactorHistory =coalFactorHistoryService.getCoalFactorHistory(id);
        }
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