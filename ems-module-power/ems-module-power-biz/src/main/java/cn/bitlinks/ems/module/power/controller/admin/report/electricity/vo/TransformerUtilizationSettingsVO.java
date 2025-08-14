package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

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

    @Schema(description = "变压器节点选择名称（code）", requiredMode = Schema.RequiredMode.REQUIRED, example = "12042")
    private String transformerNodeName;
    /**
     * '负载电流'
     */
    @Schema(description = "负载电流", example = "12042")
    private Long loadCurrentId;

    @Schema(description = "负载电流节点选择名称（code）")
    private String loadCurrentNodeName;
    /**
     * '电压等级'
     */
    @Schema(description = "电压等级", example = "12042")
    private String voltageLevel;
    /**
     * '额定容量
     */
    @Schema(description = "额定容量", example = "12042")
    private BigDecimal ratedCapacity;

}
