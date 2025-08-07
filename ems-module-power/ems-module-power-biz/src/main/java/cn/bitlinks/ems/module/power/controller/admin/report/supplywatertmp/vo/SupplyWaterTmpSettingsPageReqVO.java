package cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 供应分析分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SupplyWaterTmpSettingsPageReqVO extends PageParam {


    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;

    @Schema(description = "系统")
    private String system;

    @Schema(description = "台账id")
    private Long standingbookId;

    @Schema(description = "能源参数名称")
    private String energyParamName;

    @Schema(description = "上限")
    private Integer max;

    @Schema(description = "下限")
    private Integer min;
}