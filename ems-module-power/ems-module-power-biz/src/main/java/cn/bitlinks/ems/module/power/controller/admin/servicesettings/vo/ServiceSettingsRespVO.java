package cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 服务设置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ServiceSettingsRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "14766")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "服务名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("服务名称")
    private String serviceName;

    @Schema(description = "协议类型(0：OPCDA 1:MODBUS-TCP)")
    @ExcelProperty("协议类型(0：OPCDA 1:MODBUS-TCP)")
    private Integer protocol;

    @Schema(description = "IP地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("IP地址")
    private String ipAddress;

    @Schema(description = "SMTP 服务器端口", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("SMTP 服务器端口")
    private Integer port;

    @Schema(description = "重试次数，默认3", requiredMode = Schema.RequiredMode.REQUIRED, example = "19528")
    @ExcelProperty("重试次数，默认3")
    private Integer retryCount;

    @Schema(description = "注册表ID", example = "3042")
    @ExcelProperty("注册表ID")
    private String registryId;

    @Schema(description = "用户名", example = "赵六")
    @ExcelProperty("用户名")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("密码")
    private String password;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}