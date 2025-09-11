package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author liumingqiang
 */
@Schema(description = "统计总览 能源消耗 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class RecentSevenDayResultVO {

    @Schema(description = "名称", example = "天然气")
    private String name;

    @Schema(description = "用量单位", example = "m3")
    private String unit;

    @Schema(description = "能源图标")
    private Map<String, String> energyIcon;

    @Schema(description = "X轴")
    private List<String> x;

    @Schema(description = "Y轴")
    private List<BigDecimal> y;

}