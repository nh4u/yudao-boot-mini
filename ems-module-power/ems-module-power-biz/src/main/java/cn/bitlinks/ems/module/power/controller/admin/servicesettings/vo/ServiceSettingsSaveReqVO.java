package cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

@Schema(description = "管理后台 - 服务设置新增/修改 Request VO")
@Data
public class ServiceSettingsSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "14766")
    private Long id;

    @Schema(description = "服务名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "服务名称不能为空")
    private String serviceName;

    @Schema(description = "协议类型(0：OPCDA 1:MODBUS-TCP)")
    private Integer protocol;

    @Schema(description = "IP地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "IP地址不能为空")
    private String ipAddress;

    @Schema(description = "SMTP 服务器端口", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "SMTP 服务器端口不能为空")
    private Integer port;

    @Schema(description = "重试次数，默认3", requiredMode = Schema.RequiredMode.REQUIRED, example = "19528")
    @NotNull(message = "重试次数，默认3不能为空")
    private Integer retryCount;

    @Schema(description = "注册表ID", example = "3042")
    private String registryId;

    @Schema(description = "用户名", example = "赵六")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "密码不能为空")
    private String password;

}