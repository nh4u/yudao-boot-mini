package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Schema(description = "管理后台 - 告警策略设备选择 Request VO")
@Data
public class DeviceScopeVO {
    @Schema(description = "设备范围选择id（设备id+设备分类id）")
    private Long scopeId;

    @Schema(description = "设备范围名称")
    private String scopeName;

    @Schema(description = "设备范围选择 true-1 设备 false-0 设备分类")
    private Boolean deviceFlag;
}
