package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 台账-数采设置详细参数属性 VO")
@Data
public class StandingbookAcquisitionDetailAttrDTO {

    @Schema(description = "参数编码")
    private String code;

    @Schema(description = "是否能源数采参数 0自定义数采 1能源数采")
    private Boolean energyFlag;

    @Schema(description = "参数名称")
    private String parameter;
    @Schema(description = "数据特征")
    private Integer dataFeature;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "数据类型")
    private Integer dataType;

    @Schema(description = "用量")
    private Integer usage;

}
