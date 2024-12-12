package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 用能统计返回结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsDateData {


    @Schema(description = "日期", example = "2024/12/11")
    private String date;

    @Schema(description = "用量", example = "0.00")
    private BigDecimal consumption;

    @Schema(description = "折价", example = "0.00")
    private BigDecimal money;

}