package cn.bitlinks.ems.module.power.controller.admin.warninginfo;

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

import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.service.warninginfo.WarningInfoService;

@Tag(name = "管理后台 - 告警信息")
@RestController
@RequestMapping("/power/warning-info")
@Validated
public class WarningInfoController {

    @Resource
    private WarningInfoService warningInfoService;

    @PostMapping("/statistics")
    @Operation(summary = "告警信息统计")
    @PreAuthorize("@ss.hasPermission('power:warning-info:query')")
    public CommonResult<WarningInfoStatisticsRespVO> statistics() {
        return success(warningInfoService.statistics());
    }

    @PostMapping("/create")
    @Operation(summary = "创建告警信息")
    @PreAuthorize("@ss.hasPermission('power:warning-info:create')")
    public CommonResult<Long> createWarningInfo(@Valid @RequestBody WarningInfoSaveReqVO createReqVO) {
        return success(warningInfoService.createWarningInfo(createReqVO));
    }

    @PutMapping("/updateStatus")
    @Operation(summary = "更新告警信息状态：点击处理：1，点击处理完成：2")
    @PreAuthorize("@ss.hasPermission('power:warning-info:update')")
    public CommonResult<Boolean> updateWarningInfoStatus(@Valid @RequestBody WarningInfoStatusUpdReqVO updateReqVO) {
        warningInfoService.updateWarningInfoStatus(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除告警信息")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:warning-info:delete')")
    public CommonResult<Boolean> deleteWarningInfo(@RequestParam("id") Long id) {
        warningInfoService.deleteWarningInfo(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得告警信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:warning-info:query')")
    public CommonResult<WarningInfoRespVO> getWarningInfo(@RequestParam("id") Long id) {
        WarningInfoDO warningInfo = warningInfoService.getWarningInfo(id);
        return success(BeanUtils.toBean(warningInfo, WarningInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得我的告警信息分页/小铃铛传status处理状态0-未处理，第一页，默认页数填5")
    @PreAuthorize("@ss.hasPermission('power:warning-info:query')")
    public CommonResult<PageResult<WarningInfoRespVO>> getWarningInfoPage(@Valid WarningInfoPageReqVO pageReqVO) {
        PageResult<WarningInfoDO> pageResult = warningInfoService.getWarningInfoPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WarningInfoRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出告警信息 Excel")
    @PreAuthorize("@ss.hasPermission('power:warning-info:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarningInfoExcel(@Valid WarningInfoPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarningInfoDO> list = warningInfoService.getWarningInfoPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "告警信息.xls", "数据", WarningInfoRespVO.class,
                        BeanUtils.toBean(list, WarningInfoRespVO.class));
    }

}