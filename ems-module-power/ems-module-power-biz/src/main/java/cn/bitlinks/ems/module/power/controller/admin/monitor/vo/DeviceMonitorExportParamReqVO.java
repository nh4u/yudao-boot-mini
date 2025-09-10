package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class DeviceMonitorExportParamReqVO extends DeviceMonitorParamReqVO {


    @Schema(description = "0：用量；1：折标煤。2：成本")
    @NotNull(message = "用量、折标煤、成本不能为空")
    private Integer exportFlag;
}
