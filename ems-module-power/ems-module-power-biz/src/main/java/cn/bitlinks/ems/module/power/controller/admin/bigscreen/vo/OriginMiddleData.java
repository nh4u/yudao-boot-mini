package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 室外工况 VO")
@Data
public class OriginMiddleData {

    @Schema(description = "低温冷机")
    private List<BigDecimal> ltcNow;

    @Schema(description = "低温系统")
    private List<BigDecimal> ltsNow;

    @Schema(description = "中温冷机")
    private List<BigDecimal> mtcNow;

    @Schema(description = "中温系统")
    private List<BigDecimal> mtsNow;

    @Schema(description = "中温系统")
    private List<String> xdata;
}