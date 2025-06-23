package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 批量补录失败项
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
     * 失败项
     */
    @Schema(description = "失败项")
    private List<AcqDataExcelResultVO> failList;

    /**
     * 失败采集点数量
     */
    @Schema(description = "失败采集点数量")
    private int failAcqTotal;
}
