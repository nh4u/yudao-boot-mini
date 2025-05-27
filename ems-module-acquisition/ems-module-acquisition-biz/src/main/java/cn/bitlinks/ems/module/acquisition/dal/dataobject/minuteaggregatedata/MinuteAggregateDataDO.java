package cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分钟聚合数据
 */
@TableName(value = "minute_aggregate_data", autoResultMap = true)
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinuteAggregateDataDO {
    /**
     * 聚合时间
     */
    @JsonProperty("aggregate_time")
    private LocalDateTime aggregateTime;
    /**
     * 参数 code
     */
    @JsonProperty("param_code")
    private String paramCode;
    /**
     * 是否能源数采参数 0自定义 1能源参数
     */
    @JsonProperty("energy_flag")
    private Boolean energyFlag;
    /**
     * OPC_DA:IO地址/
     */
    @JsonProperty("data_site")
    private String dataSite;
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
     * 增量（累积值）
     */
    @JsonProperty("incremental_value")
    private BigDecimal incrementalValue;

}
