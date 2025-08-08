package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author bmqi
 * @date 2025年07月30日 18:27
 */
@Schema(description = "管理后台 - 用电量统计结果图 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ConsumptionStatisticsChartResultVO<T> {

    /**
     * 统计信息
     */
    private List<T> ydata;

    /**
     * 表头
     */
    private List<String> xdata;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;
}
