package cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinuteAggDataSplitDTO {

    private MinuteAggregateDataDTO startDataDO;
    private MinuteAggregateDataDTO endDataDO;
}
