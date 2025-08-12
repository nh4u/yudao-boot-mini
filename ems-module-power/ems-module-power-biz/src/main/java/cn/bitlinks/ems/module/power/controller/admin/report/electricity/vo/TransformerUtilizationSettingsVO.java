package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 变压器设置
 */
@Data
public class TransformerUtilizationSettingsVO {
    /**
     * id
     */
    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long id;
    /**
     * '变压器'
     */
    @Schema(description = "变压器", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Long transformerId;
    /**
     * '负载电流'
     */
    @Schema(description = "负载电流", example = "12042")
    private Long loadCurrentId;
    /**
     * '电压等级'
     */
    @Schema(description = "电压等级", example = "12042")
    private Integer voltageLevel;
    /**
     * '额定容量
     */
    @Schema(description = "额定容量", example = "12042")
    private String ratedCapacity;

    @Schema(description = "顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private Integer sort;
}
