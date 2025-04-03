package cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import cn.bitlinks.ems.framework.mybatis.core.type.StringListTypeHandler;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.util.List;

/**
 * 告警策略条件 DO
 *
 * @author bitlinks
 */
@TableName(value = "power_warning_strategy_condition", autoResultMap = true)
@KeySequence("power_warning_strategy_condition_seq")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningStrategyConditionDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 策略id
     */
    private Long strategyId;

    /**
     * 条件参数-属性id，层级id+能源参数编码
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> paramId;
    /**
     * 条件连接符
     */
    private Integer connector;
    /**
     * 条件值
     */
    private String value;

}