package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 室外工况 VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class OutsideEnvData {

    @Schema(description = "风向")
    private String windDirection;

    @Schema(description = "风向值")
    private BigDecimal windDirectionValue;

    @Schema(description = "风速")
    private BigDecimal windSpeed;

    @Schema(description = "温度")
    private BigDecimal temperature;

    @Schema(description = "湿度")
    private BigDecimal humidity;

    @Schema(description = "露点")
    private BigDecimal dewPoint;

    @Schema(description = "气压")
    private BigDecimal atmosphericPressure;

    @Schema(description = "噪音")
    private BigDecimal noise;
}