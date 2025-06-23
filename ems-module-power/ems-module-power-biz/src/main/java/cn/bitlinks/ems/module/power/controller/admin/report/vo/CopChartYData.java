package cn.bitlinks.ems.module.power.controller.admin.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/06/22 17:08
 **/
@Schema(description = "管理后台 - COP 图Y轴数据 VO")
@Data
@EqualsAndHashCode(callSuper = false)
public class CopChartYData {

    @Schema(description = "低温冷机-现在")
    private List<BigDecimal> ltcNow;
    @Schema(description = "低温冷机-去年")
    private List<BigDecimal> ltcPre;

    @Schema(description = "低温系统-现在")
    private List<BigDecimal> ltsNow;
    @Schema(description = "低温系统-去年")
    private List<BigDecimal> ltsPre;

    @Schema(description = "中温冷机-现在")
    private List<BigDecimal> mtcNow;
    @Schema(description = "中温冷机-去年")
    private List<BigDecimal> mtcPre;

    @Schema(description = "中温系统-现在")
    private List<BigDecimal> mtsNow;
    @Schema(description = "中温系统-去年")
    private List<BigDecimal> mtsPre;

}
