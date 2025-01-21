package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 设备关联配置新增/修改 Request VO")
@Data
public class DeviceAssociationConfigurationSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "20982")
    private Long id;

    @Schema(description = "能源id", example = "29619")
    private Long energyId;

    @Schema(description = "计量器具id", example = "17669")
    private Long measurementInstrumentId;

    @Schema(description = "关联下级计量", example = "2485")
    private String measurement;

    @Schema(description = "关联设备", example = "15562")
    private String device;
}