package cn.bitlinks.ems.module.power.dal.dataobject.report.gas;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 气化科固定43条计量器具配置
 *
 * @author bmqi
 */
@TableName("power_gas_measurement")
@KeySequence("power_gas_measurement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PowerGasMeasurementDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 计量器具名称（可变，不作为依据）
     */
    private String measurementName;

    /**
     * 计量器具编号（即台账code）
     */
    private String measurementCode;


    /**
     * 排序
     */
    private Integer sortNo;

    /**
     * 能源参数中文名
     */
    private String energyParam;
}
