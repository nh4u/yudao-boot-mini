package cn.bitlinks.ems.module.power.dal.dataobject.pricedetail;

import lombok.*;

import java.time.LocalTime;
import java.util.*;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 单价详细 DO
 *
 * @author bitlinks
 */
@TableName("ems_price_detail")
@KeySequence("ems_price_detail_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceDetailDO extends BaseDO {

    /**
     * id
     */
    @TableId
    private Long id;
    /**
     * 单价id
     */
    private Long priceId;
    /**
     * 时段类型
     */
    private Integer periodType;
    /**
     * 时段开始时间
     */
    private LocalTime periodStart;
    /**
     * 时段结束时间
     */
    private LocalTime periodEnd;
    /**
     * 档位用量下限
     */
    private BigDecimal usageMin;
    /**
     * 档位用量上限
     */
    private BigDecimal usageMax;
    /**
     * 单价
     */
    private BigDecimal unitPrice;

}