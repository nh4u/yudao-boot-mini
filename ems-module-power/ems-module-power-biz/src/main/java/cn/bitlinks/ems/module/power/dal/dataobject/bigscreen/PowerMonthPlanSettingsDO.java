package cn.bitlinks.ems.module.power.dal.dataobject.bigscreen;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 供应分设置析 DO
 *
 * @author bitlinks
 */
@TableName("power_month_plan_settings")
@KeySequence("power_month_plan_settings_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerMonthPlanSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 能源名称
     */
    private String energyName;
    /**
     * 能源编号
     */
    private String energyCode;
    /**
     * 能源单位
     */
    private String energyUnit;
    /**
     * 计划用量
     */
    private BigDecimal plan;

}