package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 设备监控-设备名片 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeviceMonitorDeviceReqVO {

    @Schema(description = "设备id")
    @NotNull(message = "设备不能为空")
    private Long sbId;

    @Schema(description = "刷新标识")
    @NotNull(message = "刷新标识不能为空")
    private Integer refresh;

}