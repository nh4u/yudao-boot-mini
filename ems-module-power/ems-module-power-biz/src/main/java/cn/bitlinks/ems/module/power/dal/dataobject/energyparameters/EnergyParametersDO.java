package cn.bitlinks.ems.module.power.dal.dataobject.energyparameters;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 能源参数 DO
 *
 * @author bitlinks
 */
@TableName("ems_energy_parameters")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyParametersDO extends BaseDO {

    /**
     * id
     */
    @TableId
    private Long id;
    /**
     * 能源id
     */
    private Long energyId;
    /**
     * 参数名称
     */
    private String parameter;
    /**
     * 编码
     */
    private String code;
    /**
     * 数据特征
     */
    private Integer dataFeature;
    /**
     * 单位
     */
    private String unit;
    /**
     * 数据类型
     */
    private Integer dataType;
    /**
     * 用量
     */
    @TableField("`usage`")
    private Integer usage;

}