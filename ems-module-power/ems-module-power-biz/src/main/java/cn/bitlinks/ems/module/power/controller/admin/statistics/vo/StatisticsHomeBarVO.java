package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "首页-柱状图数据")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StatisticsHomeBarVO extends StatisticsBarVO {

    /**
     * 平均值
     */
    @Schema(description = "平均值")
    private BigDecimal avg;

}
