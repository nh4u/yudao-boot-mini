package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author wangl
 * @date 2025年05月14日 15:06
 */
@Schema(description = "管理后台 - 用能统计结果图 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsChartResultV2VO {

    /**
     * 表头
     */
    private List<StatisticsChartYInfoV2VO> ydata;


    /**
     * 表头
     */
    private List<String> xdata;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;
}
