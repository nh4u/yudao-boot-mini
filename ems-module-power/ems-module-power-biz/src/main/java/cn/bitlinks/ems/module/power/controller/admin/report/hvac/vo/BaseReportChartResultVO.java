package cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 报表统计简单图、折线、柱形结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class BaseReportChartResultVO<T> {
    /**
     * y轴
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
