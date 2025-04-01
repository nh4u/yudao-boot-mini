package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 最新告警策略对应的告警信息 Response VO")
@Data
public class WarningInfoLatestStrategyRespVO {

    @Schema(description = "策略id")
    private Long strategyId;

    @Schema(description = "触发告警信息时间")
    private LocalDateTime triggerTime;


}