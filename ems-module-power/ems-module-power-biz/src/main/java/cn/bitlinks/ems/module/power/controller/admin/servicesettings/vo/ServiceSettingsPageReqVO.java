package cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 服务设置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ServiceSettingsPageReqVO extends PageParam {

    @Schema(description = "服务名称", example = "李四")
    private String serviceName;

    @Schema(description = "协议类型(0：OPCDA 1:MODBUS-TCP)")
    private Integer protocol;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "端口")
    private Integer port;

    @Schema(description = "重试次数，默认3", example = "19528")
    private Integer retryCount;

    @Schema(description = "注册表ID", example = "3042")
    private String registryId;

    @Schema(description = "用户名", example = "赵六")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}