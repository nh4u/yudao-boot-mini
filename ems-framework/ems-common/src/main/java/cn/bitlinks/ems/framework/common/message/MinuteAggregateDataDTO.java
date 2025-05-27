package cn.bitlinks.ems.framework.common.message;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分钟聚合数据
 */
@Data
public class MinuteAggregateDataDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 聚合时间
     */
    private LocalDateTime aggregateTime;
    /**
     * 参数 code
     */
    private String paramCode;
    /**
     * 是否能源数采参数 0自定义 1能源参数
     */
    private Boolean energyFlag;
    /**
     * OPC_DA:IO地址/
     */
    private String dataSite;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 全量（累积值）
     */
    private BigDecimal fullValue;
    /**
     * 增量（累积值）
     */
    private BigDecimal incrementalValue;

}
