package cn.bitlinks.ems.module.power.controller.admin.devicemonitor.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备监控-设备名片 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeviceMonitorDeviceReqVO {

    @Schema(description = "设备id")
    private Long sbId;

}