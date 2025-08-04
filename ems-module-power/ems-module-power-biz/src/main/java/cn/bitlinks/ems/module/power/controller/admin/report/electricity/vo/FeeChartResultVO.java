package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

/**
 * @author wangl
 * @date 2025年05月15日 10:57
 */

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsChartYInfoV2VO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangl
 * @date 2025年05月14日 15:06
 */
@Schema(description = "管理后台 - 用能统计结果图 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeeChartResultVO<T> {

    /**
     * 表头
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
