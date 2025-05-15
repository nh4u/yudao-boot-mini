package cn.bitlinks.ems.module.power.dal.dataobject.usagecost;

import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author wangl
 * @date 2025年05月09日 13:39
 */
@TableName(value = "usage_cost", autoResultMap = true)
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageCostDO {

    /**
     * 台账ID
     */
    private Long standingbookId;

    /**
     * 聚合时间
     */
    private LocalDateTime aggregateTime;

    /**
     * 当前用量
     */
    private BigDecimal currentUsage;

    /**
     * 截至当前总用量
     */
    private BigDecimal totalUsage;

    /**
     * 成本
     */
    private BigDecimal cost;

    /**
     * 折标煤
     */
    private BigDecimal standardCoalEquivalent;

    /**
     * 能源
     */
    private Long energyId;

    /**
     * 标签ID
     */
    private Long tagId;


}

