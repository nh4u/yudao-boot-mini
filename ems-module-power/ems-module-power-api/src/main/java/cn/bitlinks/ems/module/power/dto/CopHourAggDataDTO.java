package cn.bitlinks.ems.module.power.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CopHourAggDataDTO {
    /**
     * 聚合时间
     */
    @JsonProperty("aggregate_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aggregateTime;
    /**
     * 低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS
     */
    @JsonProperty("cop_type")
    private String copType;
    /**
     * cop值
     */
    @JsonProperty("cop_value")
    private BigDecimal copValue;
}
