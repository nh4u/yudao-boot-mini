package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 补录新增/修改 Request VO")
@Data
public class AdditionalRecordingSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "86")
    private Long id;

    @Schema(description = "凭证id", example = "29399")
    private Long voucherId;

    @Schema(description = "计量器具id", example = "18976")
    private Long standingbookId;

    @Schema(description = "数值类型")
    private String valueType;

    @Schema(description = "上次采集时间")
    private LocalDateTime lastCollectTime;

    @Schema(description = "上次数值")
    private BigDecimal lastValue;

    @Schema(description = "本次采集时间")
    private LocalDateTime thisCollectTime;

    @Schema(description = "本次数值")
    private BigDecimal thisValue;

    @Schema(description = "补录人")
    private String recordPerson;

    @Schema(description = "补录原因", example = "不香")
    private String recordReason;

    @Schema(description = "补录方式")
    private Integer recordMethod;

    @Schema(description = "录入时间")
    private LocalDateTime enterTime;

}