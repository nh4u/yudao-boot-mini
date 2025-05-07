package cn.bitlinks.ems.module.power.controller.admin.servicesettings.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 服务设置 Response VO")
@Data
public class ServiceSettingsOptionsRespVO extends ServiceSettingsRespVO {


    @Schema(description = "标签名称")
    private String serviceFormatName;

}