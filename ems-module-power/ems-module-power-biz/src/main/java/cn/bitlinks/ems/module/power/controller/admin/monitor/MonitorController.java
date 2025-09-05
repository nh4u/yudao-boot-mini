package cn.bitlinks.ems.module.power.controller.admin.monitor;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.service.devicemonitor.DeviceMonitorService;
import cn.bitlinks.ems.module.power.service.monitor.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 设备监控")
@RestController
@RequestMapping("/power/monitor")
@Validated
public class MonitorController {
    @Resource
    private MonitorService monitorService;
    @Resource
    private DeviceMonitorService deviceMonitorService;

    @PostMapping("/monitorList")
    @Operation(summary = "获得监控列表")
    //@PreAuthorize("@ss.hasPermission('power:minitor:query')")
    public CommonResult<MinitorRespVO> getMinitorList(@Valid @RequestBody Map<String, String> pageReqVO) {
        MinitorRespVO minitorRespVO = monitorService.getMinitorList(pageReqVO);
        return success(minitorRespVO);
    }

    @PostMapping("/deviceDetail")
    @Operation(summary = "监控详情")
    //@PreAuthorize("@ss.hasPermission('power:minitor:query')")
    public CommonResult<MinitorDetailRespVO> deviceDetail(@Valid @RequestBody MinitorParamReqVO paramVO) {
        MinitorDetailRespVO minitorDetailRespVO = monitorService.deviceDetail(paramVO);
        return success(minitorDetailRespVO);
    }

    @GetMapping("/getDaqAttrs")
    @Operation(summary = "获取台账所有数采参数（能源+自定义）查询启用的")
    @PermitAll
    //@PreAuthorize("@ss.hasPermission('power:minitor:query')")
    public CommonResult<List<StandingbookTmplDaqAttrRespVO>> getDaqAttrs(@RequestParam("standingbookId") Long standingbookId) {
        return success(monitorService.getDaqAttrs(standingbookId));

    }

    @PostMapping("/exportDetailTable")
    @Operation(summary = "导出详情表数据")
    //@PreAuthorize("@ss.hasPermission('power:minitor:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDetailTable(@Valid @RequestBody MinitorParamReqVO paramVO,
                                  HttpServletResponse response) throws IOException {
        List<MinitorDetailData> list = monitorService.getDetailTable(paramVO);
        // 导出 Excel
        ExcelUtils.write(response, "设备监控详情数据表.xlsx", "数据", MinitorDetailData.class, list);
    }


    @PostMapping("/warning")
    @Operation(summary = "查询告警信息")
    @PermitAll
    public CommonResult<DeviceMonitorWarningRespVO> getWarningInfo(@RequestBody @Valid DeviceMonitorWarningReqVO reqVO) {
        return success(deviceMonitorService.getWarningInfo(reqVO));
    }

    @PostMapping("/deviceInfo")
    @Operation(summary = "根据设备id查询设备名片")
    @PermitAll
    public CommonResult<DeviceMonitorDeviceRespVO> getDeviceInfo(@RequestBody @Valid DeviceMonitorDeviceReqVO reqVO) {
        return success(deviceMonitorService.getDeviceInfo(reqVO));
    }

    @PostMapping("/qrCode")
    @Operation(summary = "生成设备二维码")
    @PermitAll
    public CommonResult<String> getQrCode(@RequestBody @Valid DeviceMonitorDeviceReqVO reqVO) {
        return success(deviceMonitorService.getQrCode(reqVO));
    }

    @GetMapping("/valid")
    @Operation(summary = "校验设备二维码")
    @PermitAll
    public CommonResult<Boolean> validQrCode(@RequestParam("code") String code) {
        return success(deviceMonitorService.validQrCode(code));
    }


}
