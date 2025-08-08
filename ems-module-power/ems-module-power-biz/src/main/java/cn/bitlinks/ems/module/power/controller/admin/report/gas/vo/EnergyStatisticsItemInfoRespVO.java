package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author bmqi
 */
@Schema(description = "管理后台 - 能源统计项 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class EnergyStatisticsItemInfoRespVO {

    @Schema(description = "台账id", example = "[]")
    private Long standingbookId;

    @Schema(description = "计量器具名称", example = "123")
    private String measurementName;
}