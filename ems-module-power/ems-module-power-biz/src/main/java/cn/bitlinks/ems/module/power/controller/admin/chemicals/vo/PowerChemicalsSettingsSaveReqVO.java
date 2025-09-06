package cn.bitlinks.ems.module.power.controller.admin.chemicals.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String system;

    @Schema(description = "日期")
    @NotBlank(message = "日期不能为空")
    private LocalDateTime time;

    @Schema(description = "金额")
    private BigDecimal price;
}