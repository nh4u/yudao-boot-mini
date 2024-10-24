package cn.bitlinks.ems.module.power.controller.admin.standingbook;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
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

@Tag(name = "管理后台 - 台账")
@RestController
@RequestMapping("/power/standingbook")
@Validated
public class StandingbookController {

    @Resource
    private StandingbookService standingbookService;

    @PostMapping("/create")
    @Operation(summary = "创建台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:create')")
    public CommonResult<Long> createStandingbook(@Valid  @RequestBody StandingbookSaveReqVO createReqVO) {
        return success(standingbookService.createStandingbook(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<Boolean> updateStandingbook(@Valid @RequestBody StandingbookSaveReqVO updateReqVO) {
        standingbookService.updateStandingbook(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除台账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:standingbook:delete')")
    public CommonResult<Boolean> deleteStandingbook(@RequestParam("id") Long id) {
        standingbookService.deleteStandingbook(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得台账")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<StandingbookRespVO> getStandingbook(@RequestParam("id") Long id) {
        StandingbookDO standingbook = standingbookService.getStandingbook(id);
        return success(BeanUtils.toBean(standingbook, StandingbookRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得台账分页")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<PageResult<StandingbookRespVO>> getStandingbookPage(@Valid StandingbookPageReqVO pageReqVO) {
        PageResult<StandingbookDO> pageResult = standingbookService.getStandingbookPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, StandingbookRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出台账 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandingbookExcel(@Valid StandingbookPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandingbookDO> list = standingbookService.getStandingbookPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "台账.xls", "数据", StandingbookRespVO.class,
                        BeanUtils.toBean(list, StandingbookRespVO.class));
    }

}
