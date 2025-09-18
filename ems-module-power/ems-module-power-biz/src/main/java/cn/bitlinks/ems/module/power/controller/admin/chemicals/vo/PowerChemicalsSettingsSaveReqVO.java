package cn.bitlinks.ems.module.power.controller.admin.chemicals.vo;

import cn.bitlinks.ems.module.power.config.LocalDateToLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 化学品设置新增/修改 Request VO")
@Data
public class PowerChemicalsSettingsSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    @NotNull(message = "id不能为空")
    private Long id;

    @Schema(description = "类型", example = "随便")
    @NotBlank(message = "类型不能为空")
    private String code;

    @Schema(description = "日期")
    @NotNull(message = "日期不能为空")
    @JsonDeserialize(using = LocalDateToLocalDateTimeDeserializer.class)
    private LocalDateTime time;

    @Schema(description = "金额")
    private BigDecimal price;
}