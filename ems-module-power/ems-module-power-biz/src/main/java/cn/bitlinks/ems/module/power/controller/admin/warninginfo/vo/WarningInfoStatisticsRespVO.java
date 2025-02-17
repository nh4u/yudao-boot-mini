package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 告警信息统计 Response VO")
@Data
public class WarningInfoStatisticsRespVO {

    @Schema(description = "总条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long total;

    @Schema(description = "告警等级提示0条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long count0;
    @Schema(description = "告警等级警告1条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long count1;
    @Schema(description = "告警等级次要2条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long count2;
    @Schema(description = "告警等级重要3条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long count3;
    @Schema(description = "告警等级紧急4条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long count4;

}