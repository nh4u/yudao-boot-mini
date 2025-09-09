package cn.bitlinks.ems.module.power.controller.admin.monitor;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.service.devicemonitor.DeviceMonitorService;
import cn.bitlinks.ems.module.power.service.monitor.MonitorService;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DEVICE_MONITOR_EXPORT_FLAG_ERROR;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.getConvertData;

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
    public CommonResult<MonitorRespVO> getMinitorList(@Valid @RequestBody Map<String, String> pageReqVO) {
        return success(monitorService.getMinitorList(pageReqVO));
    }

    @PostMapping("/deviceDetail")
    @Operation(summary = "监控详情")
    @PermitAll
    //@PreAuthorize("@ss.hasPermission('power:minitor:query')")
    public CommonResult<MonitorDetailRespVO> deviceDetail(@Valid @RequestBody MonitorParamReqVO paramVO) {
        return success(monitorService.deviceDetail(paramVO));
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
    @PermitAll
    public void exportDetailTable(@Valid @RequestBody MonitorParamReqVO paramVO,
                                  HttpServletResponse response) throws IOException {
        List<MonitorDetailData> list = monitorService.getDetailTable(paramVO);
        // 导出 Excel
        ExcelUtils.write(response, "设备监控详情数据表.xlsx", "数据", MonitorDetailData.class, list);
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


    @PostMapping("/energyList")
    @Operation(summary = "重点设备查询能源参数列表")
    @PermitAll
    public CommonResult<List<DeviceMonitorDeviceEnergyRespVO>> energyList(@RequestBody @Valid DeviceMonitorDeviceReqVO reqVO) {
        return success(deviceMonitorService.energyList(reqVO.getSbId()));
    }

    @PostMapping("/deviceTableAndChart")
    @Operation(summary = "重点设备查询图表")
    @PermitAll
    public CommonResult<DeviceMonitorDetailRespVO> deviceTableAndChart(@Valid @RequestBody DeviceMonitorParamReqVO paramVO) {
        return success(deviceMonitorService.deviceTableAndChart(paramVO));
    }

    @PostMapping("/deviceTableAndChartExport")
    @Operation(summary = "重点设备查询图表-导出")
    //@PreAuthorize("@ss.hasPermission('power:minitor:export')")
    @ApiAccessLog(operateType = EXPORT)
    @PermitAll
    public void deviceTableAndChartExport(@Valid @RequestBody DeviceMonitorExportParamReqVO paramVO,
                                          HttpServletResponse response) throws IOException {
        DeviceMonitorDetailRespVO respVO = deviceMonitorService.deviceTableAndChart(paramVO);
        List<String> sbHeaders = respVO.getTableHeaders();
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("时间"));
        if (CollUtil.isNotEmpty(sbHeaders)) {
            head.addAll(sbHeaders.stream().map(Collections::singletonList).collect(Collectors.toList()));

        }
        head.add(Collections.singletonList("汇总值"));
//        List<String> header = new ArrayList<>();
//        List<String> sbHeaders = respVO.getTableHeaders();
//        header.add("时间");
//        if (CollUtil.isNotEmpty(sbHeaders)) {
//            header.addAll(sbHeaders);
//        }
//        header.add("汇总值");
//        headers.add(header);
        List<DeviceMonitorRowData> tableResult;
        Integer energyFlag = paramVO.getExportFlag();
        if (energyFlag == 0) {
            tableResult = respVO.getUsageData();
        } else if (energyFlag == 1) {
            tableResult = respVO.getCoalData();
        } else if (energyFlag == 2) {
            tableResult = respVO.getCostData();
        } else {
            throw exception(DEVICE_MONITOR_EXPORT_FLAG_ERROR);
        }

        List<List<Object>> dataList = new ArrayList<>();
        if (CollUtil.isNotEmpty(tableResult)) {
            for (DeviceMonitorRowData row : tableResult) {
                List<Object> rowList = new ArrayList<>();
                rowList.add(row.getTime()); // 第一列 时间
                if (CollUtil.isNotEmpty(row.getDataList())) {
                    for (DeviceMonitorTimeRowData timeRowData : row.getDataList()) {
                        rowList.add(getConvertData(timeRowData.getValue())); // 中间动态列
                    }
                }
                rowList.add(getConvertData(row.getSum())); // 最后一列 汇总值
                dataList.add(rowList);
            }
//            for (DeviceMonitorRowData rowData : tableResult) {
//                List<Object> data = ListUtils.newArrayList();
//                data.add(rowData.getTime());
//                if (CollUtil.isNotEmpty(rowData.getDataList())) {
//                    for (DeviceMonitorTimeRowData timeRowData : rowData.getDataList()) {
//                        data.add(timeRowData.getValue());
//                    }
//                }
//                data.add(rowData.getSum());
//            }
        }

        // 文件名字处理
        String filename = "设备监控详情数据表.xlsx";


        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("File-Name", URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));


        EasyExcel.write(response.getOutputStream())
                //自适应宽度
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(15))
                // 动态头
                .head(head)
                .sheet("数据")
                // 表格数据
                .doWrite(dataList);
    }


}
