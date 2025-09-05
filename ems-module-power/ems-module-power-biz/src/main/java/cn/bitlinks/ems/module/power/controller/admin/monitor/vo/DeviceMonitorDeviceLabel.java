package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceMonitorDeviceLabel {
    @Schema(description = "标签key")
    private String name;
    @Schema(description = "标签值")
    private String value;
}