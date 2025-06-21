package cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime aggregateTime;
    /**
     * 参数 code
     */
    @JsonProperty("param_code")
    private String paramCode;
    /**
     * 是否能源数采参数 0自定义 1能源参数
     */
    private Boolean energyFlag;

    @JsonProperty("energy_flag")
    public Integer getEnergyFlagInt() {
        if (energyFlag == null) {
            return null; // 或 return 0，根据业务是否允许 null
        }
        return energyFlag ? 1 : 0;
    }
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

    /**
     * 全量/增量（0：全量；1增量。）
     **/
    @JsonProperty("full_increment")
    private Integer fullIncrement;
    /**
     * 数据特征 1累计值2稳态值3状态值
     **/
    @JsonProperty("data_feature")
    private Integer dataFeature;
    /**
     * 数据类型 1数字2文本
     **/
    @JsonProperty("data_type")
    private Integer dataType;
    /**
     * 是否用量
     */
    @JsonProperty("usage")
    private Integer usage;

}
