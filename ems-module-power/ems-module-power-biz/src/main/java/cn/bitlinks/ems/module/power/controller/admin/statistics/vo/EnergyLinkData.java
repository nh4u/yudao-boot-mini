package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/25 18:45
 **/

@Data
@Schema(description = "能流图link数据")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class EnergyLinkData {

    @Schema(description = "能流link开始")
    private String source;

    @Schema(description = "能流link结束")
    private String target;

    @Schema(description = "指标煤数据")
    private BigDecimal value;

}
