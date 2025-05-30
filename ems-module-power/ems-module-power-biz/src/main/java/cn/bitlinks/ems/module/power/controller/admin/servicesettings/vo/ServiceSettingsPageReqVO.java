package cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 服务设置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ServiceSettingsPageReqVO extends PageParam {

    @Schema(description = "服务名称", example = "李四")
    private String serviceName;

    @Schema(description = "协议类型(0：OPC-DA 1:MODBUS-TCP)")
    private Integer protocol;

    @Schema(description = "IP地址")
    private String ipAddress;

}