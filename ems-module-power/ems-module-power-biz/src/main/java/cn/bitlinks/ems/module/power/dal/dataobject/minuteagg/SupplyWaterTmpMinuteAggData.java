package cn.bitlinks.ems.module.power.dal.dataobject.minuteagg;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 供水温度分钟聚合数据
 *
 * @author liumingqiang
 */

@Data
public class SupplyWaterTmpMinuteAggData {
    /**
     * 聚合时间
     */
    @JsonProperty("aggregate_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aggregateTime;
    /**
     * 参数 code
     */
    @JsonProperty("param_code")
    private String paramCode;
    /**
     * 台账id
     */
    @JsonProperty("standingbook_id")
    private Long standingbookId;
    /**
     * 全量（累积值）
     */
    @JsonProperty("full_value")
    private BigDecimal fullValue;
    /**
     * 点位 1：点位1；2：点位2.
     */
    @JsonProperty("point")
    private Integer point;


}
