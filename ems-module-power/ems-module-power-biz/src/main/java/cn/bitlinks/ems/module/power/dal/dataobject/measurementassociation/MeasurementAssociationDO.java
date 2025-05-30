package cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 计量器具下级计量配置 DO
 *
 * @author bitlinks
 */
@TableName("power_measurement_association")
@KeySequence("power_measurement_association_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementAssociationDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 计量器具id
     */
    private Long measurementInstrumentId;
    /**
     * 关联下级计量
     */
    private Long measurementId;

}