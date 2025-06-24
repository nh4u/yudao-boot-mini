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
public class AdditionalRecordingExistAcqDataRespVO {

    @Schema(description = "场景")
    private Integer scene;

    @Schema(description = "上一个业务点时间")
    private LocalDateTime preTime;

    @Schema(description = "当前时间业务点时间")
    private LocalDateTime curTime;

    @Schema(description = "下一个业务点时间")
    private LocalDateTime nextTime;
}
