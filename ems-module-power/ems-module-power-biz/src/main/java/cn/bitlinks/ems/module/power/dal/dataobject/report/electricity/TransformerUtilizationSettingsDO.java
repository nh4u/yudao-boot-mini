package cn.bitlinks.ems.module.power.dal.dataobject.report.electricity;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * '变压器利用率设置'
 */
@TableName("power_transformer_utilization_settings")
@KeySequence("power_transformer_utilization_settings_seq")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformerUtilizationSettingsDO extends BaseDO {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * '变压器'
     */
    private Long transformerId;
    /**
     * '负载电流'
     */
    private Long loadCurrentId;
    /**
     * '电压等级'
     */
    private String voltageLevel;
    /**
     * '额定容量
     */
    private BigDecimal ratedCapacity;
    /**
     * 顺序
     */
    private Integer sort;

}
