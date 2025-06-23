package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Excel采集点数据
 *
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
public class AcqDataExcelResultVO implements Serializable {
    /**
     * 采集点编码
     */
    @Schema(description = "采集点编码")
    private String acqCode;


    /**
     * 用量数据
     */
    @Schema(description = "用量数据")
    private BigDecimal acqValue;

    /**
     * 采集时间
     */
    @Schema(description = "采集时间")
    private String acqTime;

    /**
     * 错误类型
     */
    @Schema(description = "错误类型")
    private String mistake;

    /**
     * 具体错误
     */
    @Schema(description = "具体错误")
    private String mistakeDetail;
}
