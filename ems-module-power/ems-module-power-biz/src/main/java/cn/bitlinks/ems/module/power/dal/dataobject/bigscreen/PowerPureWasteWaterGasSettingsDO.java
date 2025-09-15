package cn.bitlinks.ems.module.power.dal.dataobject.bigscreen;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * 供应分设置析 DO
 *
 * @author bitlinks
 */
@TableName("power_pure_waste_water_gas_settings")
@KeySequence("power_pure_waste_water_gas_settings_seq")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerPureWasteWaterGasSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 类型
     */
    private String system;
    /**
     * 编码
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 能源codes
     */
    private String energyCodes;
    /**
     * 台账ids
     */
    private String standingbookIds;

    @TableField(exist = false)
    private String wasteWaterName;

}