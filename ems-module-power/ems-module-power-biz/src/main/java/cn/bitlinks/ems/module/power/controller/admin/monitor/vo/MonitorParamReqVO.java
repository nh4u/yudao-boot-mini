package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * @author liumingqiang
 */
@Data
public class MonitorParamReqVO {

    @Schema(description = "统计周期,最长不超1年", example = "[\"2025-06-23 10:17:00\", \"2025-06-29 10:17:00\" ]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Size(min = 2, max = 2, message = "统计周期不能为空")
    private LocalDateTime[] range;

    @Schema(description = "时间类型 0：日；1：月；2：年；3：时；4：分。")
    private Integer dateType;

    @Schema(description = "台账")
    @NotNull(message = "台账不能为空")
    private Long standingbookId;

    @Schema(description = "台账参数")
    @NotBlank(message = "台账参数不能为空")
    private String paramCode;

    @Schema(description = "台账参数类型")
    @NotNull(message = "台账参数类型不能为空")
    private Integer energyFlag;

    @Schema(description = "数据特征 1累计值2稳态值3状态值")
    @NotNull(message = "数据特征不能为空")
    private Integer dataFeature;

    @Schema(description = "0：实时值；1：累计值。")
    @NotNull(message = "单选标记不能为空")
    private Integer flag;
}
