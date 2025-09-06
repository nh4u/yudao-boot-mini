package cn.bitlinks.ems.module.power.dal.dataobject.collectrawdata;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    @JsonProperty("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

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

}
