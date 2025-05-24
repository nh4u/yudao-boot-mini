package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.poi.ss.formula.functions.T;

/**
 * @author wangl
 * @date 2025年05月12日 9:53
 */
@Schema(description = "管理后台 - 用能统计结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsResultV2VO<T> {

    /**
     * 统计信息
     */
    private List<T> statisticsInfoList;

    /**
     * 表头
     */
    private List<String> header;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;


}
