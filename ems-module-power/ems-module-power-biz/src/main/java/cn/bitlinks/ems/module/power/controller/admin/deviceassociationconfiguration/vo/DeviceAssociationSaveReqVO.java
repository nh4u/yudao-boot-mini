package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 关联上级设备修改 Request VO")
@Data
public class DeviceAssociationSaveReqVO {


    @Schema(description = "计量器具id", example = "25507")
    private Long measurementInstrumentId;

    @Schema(description = "关联设备", example = "32363")
    private Long deviceId;

}