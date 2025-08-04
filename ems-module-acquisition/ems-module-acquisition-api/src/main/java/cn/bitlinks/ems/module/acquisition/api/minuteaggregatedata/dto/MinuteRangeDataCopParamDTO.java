package cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MinuteRangeDataCopParamDTO implements Serializable {
    private static final long serialVersionUID = 1L; // 推荐指定序列化版本
    /**
     * 台账id
     */
    private List<Long> sbIds;

    private List<String> paramCodes;
    /**
     * 聚合时间
     */
    private LocalDateTime starTime;
    /**
     * 聚合时间
     */
    private LocalDateTime endTime;
}