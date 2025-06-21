package cn.bitlinks.ems.module.power.dal.dataobject.copsettings;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName(value = "cop_hour_aggregate_data", autoResultMap = true)
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopHourAggDataDO {
    /**
     * 聚合时间
     */
    private LocalDateTime aggregateTime;
    /**
     * 低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS
     */
    private String copType;
    /**
     * cop值
     */
    private BigDecimal copValue;
}
