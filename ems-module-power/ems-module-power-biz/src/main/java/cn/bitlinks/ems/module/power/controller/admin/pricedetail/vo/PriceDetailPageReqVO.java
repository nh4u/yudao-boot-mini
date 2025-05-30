package cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo;

import lombok.*;

import java.time.LocalTime;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 单价详细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PriceDetailPageReqVO extends PageParam {

    @Schema(description = "单价id", example = "9614")
    private Long priceId;

    @Schema(description = "时段类型", example = "1")
    private Integer periodType;

    @Schema(description = "时段开始时间")
    private LocalTime periodStart;

    @Schema(description = "时段结束时间")
    private LocalTime periodEnd;

    @Schema(description = "档位用量下限")
    private BigDecimal usageMin;

    @Schema(description = "档位用量上限")
    private BigDecimal usageMax;

    @Schema(description = "单价", example = "4236")
    private BigDecimal unitPrice;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}