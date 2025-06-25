package cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MinuteAggDataSplitListDTO {

    List<MinuteAggDataSplitDTO> minuteAggDataSplitDTOList;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

}
