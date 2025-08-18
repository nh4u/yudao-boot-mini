package cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp;

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
@TableName("power_supply_water_tmp_settings")
@KeySequence("power_supply_water_tmp_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyWaterTmpSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 标识
     */
    private String code;
    /**
     * 系统
     */
    private String system;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 能源参数名称
     */
    private String energyParamName;
    /**
     * 能源参数编码
     */
    private String energyParamCode;
    /**
     * 上限
     */
    private BigDecimal max;
    /**
     * 下限
     */
    private BigDecimal min;
}