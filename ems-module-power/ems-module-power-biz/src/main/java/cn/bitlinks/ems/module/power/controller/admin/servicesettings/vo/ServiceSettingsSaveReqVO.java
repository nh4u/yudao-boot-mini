package cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 服务设置新增/修改 Request VO")
@Data
public class ServiceSettingsSaveReqVO extends ServiceSettingsTestReqVO{

    @Schema(description = "id", example = "14766")
    private Long id;

    @Schema(description = "服务名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "服务名称不能为空")
    private String serviceName;

}