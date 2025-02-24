package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Title: identifier-carrier
 * @description:
 * @Author: Jiayun CUI
 * @Date 2025/02/24 16:10
 **/
@Data
@ToString(callSuper = true)
public class AdditionalRecordingLastVO {
    @Schema(description = "上次采集时间")
    private LocalDateTime lastCollectTime;

    @Schema(description = "上次数值")
    private BigDecimal lastValue;
}
