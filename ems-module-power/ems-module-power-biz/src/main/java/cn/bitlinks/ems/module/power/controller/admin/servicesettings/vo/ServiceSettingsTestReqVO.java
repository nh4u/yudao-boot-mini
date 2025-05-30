package cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 服务设置测试 Request VO")
@Data
public class ServiceSettingsTestReqVO {


    @Schema(description = "协议类型(0：OPC-DA 1:MODBUS-TCP)", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "协议类型不能为空")
    private Integer protocol;

    @Schema(description = "IP地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "IP地址不能为空")
    private String ipAddress;

    @Schema(description = "端口", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer port;

    @Schema(description = "重试次数，默认3", requiredMode = Schema.RequiredMode.REQUIRED, example = "19528")
    @NotNull(message = "重试次数，默认3不能为空")
    private Integer retryCount;

    @Schema(description = "注册表ID", example = "3042", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "注册表ID不能为空")
    private String clsid;

    @Schema(description = "用户名", example = "赵六")
    private String username;

    @Schema(description = "密码")
    private String password;

}