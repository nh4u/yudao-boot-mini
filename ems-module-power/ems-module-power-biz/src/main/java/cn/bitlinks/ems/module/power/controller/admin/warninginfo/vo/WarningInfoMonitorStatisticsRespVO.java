package cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 告警信息统计 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class WarningInfoMonitorStatisticsRespVO {

    @Schema(description = "总条数", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private Long total;

    @Schema(description = "统计信息", requiredMode = Schema.RequiredMode.REQUIRED, example = "29279")
    private List<WarningInfoStatisticsDetailRespVO> list;

}
