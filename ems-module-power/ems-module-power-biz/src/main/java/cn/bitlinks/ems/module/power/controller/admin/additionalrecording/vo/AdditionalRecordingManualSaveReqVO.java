package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.hutool.core.date.DatePattern.NORM_DATETIME_MINUTE_PATTERN;

@Schema(description = "管理后台 - 手动补录新增 Request VO")
@Data
public class AdditionalRecordingManualSaveReqVO {

    @Schema(description = "计量器具id", example = "21597")
    private Long standingbookId;

    @Schema(description = "增量1/全量0")
    private Integer valueType;

    @Schema(description = "增量：开始时间")

    @DateTimeFormat(pattern = NORM_DATETIME_MINUTE_PATTERN)
    private LocalDateTime preCollectTime;

    @Schema(description = "全量：补录时间点/增量：结束时间")
    @DateTimeFormat(pattern = NORM_DATETIME_MINUTE_PATTERN)
    private LocalDateTime thisCollectTime;

    @Schema(description = "补录数值")
    @NotNull(message = "补录数值不能为空")
    private BigDecimal thisValue;

    @Schema(description = "补录人")
    private String recordPerson;

    @Schema(description = "补录原因", example = "不好")
    private String recordReason;

}