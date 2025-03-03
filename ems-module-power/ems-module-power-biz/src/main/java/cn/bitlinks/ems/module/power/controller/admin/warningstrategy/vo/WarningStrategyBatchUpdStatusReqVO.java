package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 告警策略批量修改告警状态 Request VO")
@Data
public class WarningStrategyBatchUpdStatusReqVO {

    @Schema(description = "编号s", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @Schema(description = "禁用状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

}