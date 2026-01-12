package cn.bitlinks.ems.module.power.dal.dataobject.airconditioner;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 空调工况数据表DO
 */
@TableName(value = "air_conditioner_status_data", autoResultMap = true)
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirConditionerStatusDataDO {

    @JsonProperty("sync_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime syncTime;

    @JsonProperty("data_site")
    private String dataSite;

    @JsonProperty("raw_value")
    private String rawValue;

    @JsonProperty("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
