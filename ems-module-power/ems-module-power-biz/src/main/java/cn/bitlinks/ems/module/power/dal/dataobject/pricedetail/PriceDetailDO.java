package cn.bitlinks.ems.module.power.dal.dataobject.pricedetail;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

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
     * 单价配置id
     */
    private Long priceId;
    /**
     * 时段类型（分时段计价）
     */
    private Integer periodType;
    /**
     * 时段开始时间（分时段计价）
     */
    private LocalTime periodStart;
    /**
     * 时段结束时间（分时段计价）
     */
    private LocalTime periodEnd;
    /**
     * 档位用量下限（阶梯计价）
     */
    private BigDecimal usageMin;
    /**
     * 档位用量上限（阶梯计价）
     */
    private BigDecimal usageMax;
    /**
     * 单价（阶梯计价、分时段计价、统一计价）
     */
    private BigDecimal unitPrice;

}
