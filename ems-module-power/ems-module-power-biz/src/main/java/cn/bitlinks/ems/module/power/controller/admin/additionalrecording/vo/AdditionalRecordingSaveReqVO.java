package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 补录新增/修改 Request VO")
@Data
public class AdditionalRecordingSaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "11841")
    private Long id;

    @Schema(description = "凭证id", example = "4781")
    private Long voucherId;

    @Schema(description = "计量器具id", example = "21597")
    private Long standingbookId;

    @Schema(description = "增量/全量")
    private Integer valueType;

    @Schema(description = "上次采集时间")
    private LocalDateTime preCollectTime;

    @Schema(description = "上次数值")
    private BigDecimal preValue;

    @Schema(description = "本次采集时间")
    @NotNull(message = "本次采集时间不能为空")
    private LocalDateTime thisCollectTime;

    @Schema(description = "本次数值")
    @NotNull(message = "本次数值数值不能为空")
    private BigDecimal thisValue;

    @Schema(description = "单位")
    @NotBlank(message = "用量单位不能为空")
    private String unit;

    @Schema(description = "补录人")
    private String recordPerson;

    @Schema(description = "补录原因", example = "不好")
    private String recordReason;

    @Schema(description = "补录方式")
    private Integer recordMethod;

    @Schema(description = "录入时间")
    private LocalDateTime enterTime;

}