package cn.bitlinks.ems.module.power.controller.admin.externalapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/08/28 09:38
 **/

@Schema(description = "管理后台 - 产量数据 Request VO")
@Data
public class ProductionSaveReqVO {
    @Schema(description = "编号", example = "3687")
    private Long id;

    @Schema(description = "获取时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime time;

    @Schema(description = "原始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originTime;

    @Schema(description = "计划产量")
    private BigDecimal plan;

    @Schema(description = "实际产量")
    private BigDecimal lot;

    @Schema(description = "尺寸", example = "8")
    private Integer size;
}
