package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 台账-数采设置详细 VO")
@Data
public class StandingbookAcquisitionDetailVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "数采设置id", example = "4669")
    private Long acquisitionId;

    @Schema(description = "设备数采启停开关（0：关；1开。）", example = "1")
    private Boolean status;

    @Schema(description = "OPCDA：io地址/MODBUS：")
    private String dataSite;

    @Schema(description = "公式")
    private String formula;

    @Schema(description = "全量/增量（0：全量；1增量。）")
    private Integer fullIncrement;

    @Schema(description = "参数编码")
    private String code;

    @Schema(description = "是否能源数采参数 0自定义数采 1能源数采")
    private Boolean energyFlag;

    /* 参数部分START */
    /**
     * 参数名称
     */
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
