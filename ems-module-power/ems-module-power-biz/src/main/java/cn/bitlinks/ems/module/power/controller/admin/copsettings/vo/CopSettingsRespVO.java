package cn.bitlinks.ems.module.power.controller.admin.copsettings.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 配置标签 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CopSettingsRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS", requiredMode = Schema.RequiredMode.REQUIRED, example = "LTC")
    private String copType;

    @Schema(description = "数据特征 1累计值2稳态值3状态值", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer dataFeature;

    @Schema(description = "公式对应的参数", requiredMode = Schema.RequiredMode.REQUIRED, example = "m1")
    private String param;

    @Schema(description = "公式对应的能源参数中文名", requiredMode = Schema.RequiredMode.REQUIRED, example = "正累积")
    private String paramCnName;

    @Schema(description = "台账id")
    private Long standingbookId;

}