package cn.bitlinks.ems.module.power.dal.dataobject.report.gas;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * @TableName v_power_measurement_attributes
 */
@TableName("v_power_measurement_attributes")
@KeySequence("v_power_measurement_attributes_seq")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString()
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VPowerMeasurementAttributesDO {

    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 台账类型id
     */
    private Long typeId;
    /**
     * 计量器具名称
     */
    private String measurementName;
    /**
     * 计量器具编码
     */
    private String measurementCode;
    /**
     * 参数编码
     */
    private String paramCode;
    /**
     * 计算类型
     */
    private Integer calculateType;
    /**
     * 多租户编号
     */
    private Long tenantId;

}
