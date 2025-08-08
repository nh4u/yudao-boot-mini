package cn.bitlinks.ems.module.power.dal.dataobject.report.gas;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 储罐液位设置表
 *
 * @TableName power_tank_settings
 */
@TableName("power_tank_settings")
@KeySequence("power_tank_settings_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerTankSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 储罐名称
     */
    private String name;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 密度ρ
     */
    private BigDecimal density;
    /**
     * 重力加速度g
     */
    private BigDecimal gravityAcceleration;

    /**
     * 设备压差id
     */
    private Long pressureDiffId;

    /**
     * 排序
     */
    private Long sortNo;

}
