package cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 实时数据
 */
@TableName(value = "collect_raw_data", autoResultMap = true)
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectRawDataDO {
    /**
     * OPC_DA:IO地址/
     */
    @JsonProperty("data_site")
    private String dataSite;
    /**
     * 数据同步时间
     */
    @JsonProperty("sync_time")
    private LocalDateTime syncTime;
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
     * 台账id
     */
    @JsonProperty("standingbook_id")
    private Long standingbookId;
    /**
     * 公式计算值
     */
    @JsonProperty("calc_value")
    private String calcValue;
    /**
     * 采集值（原始）
     */
    @JsonProperty("raw_value")
    private String rawValue;
    /**
     * 是否用量
     */
    @JsonProperty("usage")
    private Integer usage;
    /**
     * 数据采集时间（原始）
     */
    @JsonProperty("collect_time")
    private LocalDateTime collectTime;


}
