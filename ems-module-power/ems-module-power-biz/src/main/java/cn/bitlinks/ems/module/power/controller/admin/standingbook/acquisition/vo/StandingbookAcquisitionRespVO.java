package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 台账-数采设置 Response VO")
@Data
public class StandingbookAcquisitionRespVO extends StandingbookRespVO {

    @Schema(description = "关联的数采设置编号")
    private Long acquisitionId;

    @Schema(description = "设备数采启停开关（0：关；1开。）")
    private Boolean status;

    @Schema(description = "采集频率")
    private Long frequency;

    @Schema(description = "采集频率单位(秒、分钟、小时、天)")
    private Integer frequencyUnit;

    @Schema(description = "采集频率(展示)")
    private String frequencyLabel;

    @Schema(description = "服务设置id")
    private Long serviceSettingsId;

    @Schema(description = "数据连接服务(展示)")
    private String serviceSettingsLabel;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

}