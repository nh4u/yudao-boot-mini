package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.poi.ss.formula.functions.T;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "原返回list结构的数据VO")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class ListDataVO {

    /**
     * Y轴环节对应的数据
     */
    @Schema(description = "Y轴数据")
    private List list;

    /**
     * 数据更新时间
     */
    @Schema(description = "数据更新时间")
    private LocalDateTime dataTime;
}
