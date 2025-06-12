package cn.bitlinks.ems.module.acquisition.service.partition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartitionDayRange {
    private String partitionName;
    private String startTime;
    private String endTime;
}
