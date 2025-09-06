package cn.bitlinks.ems.module.power.dal.dataobject.production;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * @author liumingqiang
 */
@TableName("power_production")
@KeySequence("power_production_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionDO extends BaseDO {
    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 获取时间
     */
    private LocalDateTime time;
    /**
     * 原始时间
     */
    private String originTime;
    /**
     * 计划产量
     */
    private BigDecimal plan;
    /**
     * 实际产量
     */
    private BigDecimal lot;
    /**
     * 尺寸
     */
    private Integer size;
    /**
     * 间隔产量数
     */
    private Integer value;

}