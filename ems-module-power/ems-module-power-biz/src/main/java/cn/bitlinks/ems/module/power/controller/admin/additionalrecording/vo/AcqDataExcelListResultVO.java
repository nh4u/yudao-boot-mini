package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @author bmqi
 * @date 2023/7/18 10:06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@Builder
public class AcqDataExcelListResultVO implements Serializable {
    /**
     * 成功项
     */
    @Schema(description = "成功项")
    private List<AcqDataExcelResultVO> successList;

    /**
     * 失败项
     */
    @Schema(description = "失败项")
    private List<AcqDataExcelResultVO> failList;

    /**
     * 成功项数量
     */
    @Schema(description = "成功项数量")
    private int successTotal;

    /**
     * 失败项数量
     */
    @Schema(description = "失败项数量")
    private int failTotal;

    /**
     * 成功采集点数量
     */
    @Schema(description = "成功采集点数量")
    private int successAcqTotal;

    /**
     * 失败采集点数量
     */
    @Schema(description = "失败采集点数量")
    private int failAcqTotal;
}
