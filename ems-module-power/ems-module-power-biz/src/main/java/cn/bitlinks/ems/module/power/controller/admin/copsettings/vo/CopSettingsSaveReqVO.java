package cn.bitlinks.ems.module.power.controller.admin.copsettings.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 配置标签新增/修改 Request VO")
@Data
public class CopSettingsSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS", requiredMode = Schema.RequiredMode.REQUIRED, example = "LTC")
    @NotEmpty(message = "COP类型不能为空")
    private String copType;

    @Schema(description = "数据特征 1累计值2稳态值3状态值", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "数据特征不能为空")
    private Integer dataFeature;

    @Schema(description = "公式对应的参数", requiredMode = Schema.RequiredMode.REQUIRED, example = "m1")
    @NotEmpty(message = "公式对应的参数不能为空")
    private String param;

    @Schema(description = "公式对应的能源参数中文名", requiredMode = Schema.RequiredMode.REQUIRED, example = "正累积")
    @NotEmpty(message = "公式对应的能源参数中文名不能为空")
    private String paramCnName;

    @Schema(description = "台账id")
    private Long standingbookId;



}