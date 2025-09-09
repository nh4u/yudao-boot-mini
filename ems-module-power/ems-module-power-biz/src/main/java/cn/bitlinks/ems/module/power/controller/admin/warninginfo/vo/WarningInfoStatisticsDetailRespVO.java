package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 告警信息统计 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarningInfoStatisticsDetailRespVO {
    @Schema(description = "告警等级", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private String name;

    @Schema(description = "条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long value;
}
