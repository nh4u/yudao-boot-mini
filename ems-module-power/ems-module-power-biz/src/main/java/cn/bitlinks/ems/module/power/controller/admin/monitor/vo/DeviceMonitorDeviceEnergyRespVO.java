package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 设备监控-重点设备-能源列表 Response VO")
@Data
@ExcelIgnoreUnannotated
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceMonitorDeviceEnergyRespVO {
    @Schema(description = "能源id")
    private Long id;
    @Schema(description = "能源名称")
    private String energyName;
    @Schema(description = "能源单位")
    private String unit;

}

