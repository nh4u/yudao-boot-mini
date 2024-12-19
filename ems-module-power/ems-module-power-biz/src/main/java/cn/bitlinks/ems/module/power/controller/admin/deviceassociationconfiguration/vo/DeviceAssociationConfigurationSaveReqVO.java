package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 设备关联配置新增/修改 Request VO")
@Data
public class DeviceAssociationConfigurationSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "26859")
    private Long id;

    @Schema(description = "能源id", example = "7602")
    private Long energyId;

    @Schema(description = "计量器具id", example = "10771")
    private Long measurementInstrumentId;

    @Schema(description = "设备id", example = "22446")
    private Long deviceId;

    @Schema(description = "后置计量 [{  \"id \":1, \"name \": \"燃气总表 \" ,\"energyId \":1,},...]")
    private String postMeasurement;

    @Schema(description = "前置计量  [{  \"id \":1, \"name \": \"燃气总表 \" ,\"energyId \":1,},...]")
    private String preMeasurement;

}