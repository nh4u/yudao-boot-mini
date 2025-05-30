package cn.bitlinks.ems.module.power.controller.admin.warninginfo;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoRespVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatisticsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatusUpdReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.service.warninginfo.WarningInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

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
    @TenantIgnore
    public CommonResult<WarningInfoStatisticsRespVO> statistics() {
        return success(warningInfoService.statistics());
    }


    @PutMapping("/updateStatus")
    @Operation(summary = "更新告警信息状态：点击处理：1，点击处理完成：2")
    @PreAuthorize("@ss.hasPermission('power:warning-info:update')")
    @TenantIgnore
    public CommonResult<Boolean> updateWarningInfoStatus(@Valid @RequestBody WarningInfoStatusUpdReqVO updateReqVO) {
        warningInfoService.updateWarningInfoStatus(updateReqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得告警信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:warning-info:query')")
    @TenantIgnore
    public CommonResult<WarningInfoRespVO> getWarningInfo(@RequestParam("id") Long id) {
        WarningInfoDO warningInfo = warningInfoService.getWarningInfo(id);
        return success(BeanUtils.toBean(warningInfo, WarningInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得我的告警信息分页/小铃铛传status处理状态0-未处理，第一页，默认页数填5")
    @PreAuthorize("@ss.hasPermission('power:warning-info:query')")
    @TenantIgnore
    public CommonResult<PageResult<WarningInfoRespVO>> getWarningInfoPage(@Valid WarningInfoPageReqVO pageReqVO) {
        PageResult<WarningInfoDO> pageResult = warningInfoService.getWarningInfoPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WarningInfoRespVO.class));
    }


}