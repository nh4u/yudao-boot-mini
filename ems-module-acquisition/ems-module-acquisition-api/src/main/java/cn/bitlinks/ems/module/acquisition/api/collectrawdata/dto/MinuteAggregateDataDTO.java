package cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月22日 17:48
 */
@Data
public class MinuteAggregateDataDTO implements Serializable {
    private static final long serialVersionUID = 1L; // 推荐指定序列化版本
    /**
     * 聚合时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
    /**
     * 全量/增量（0：全量；1增量。）
     **/
    private Integer fullIncrement;
    /**
     * 数据特征
     **/
    private Integer dataFeature;
    /**
     * 数据类型
     **/
    private Integer dataType;
    /**
     * 是否用量
     */
    private Integer usage;
    /**
     * 是否业务点
     */
    private Integer acqFlag;
}
