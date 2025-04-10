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
@KeySequence("ems_energy_parameters_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
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
     * 中文名
     */
    private String chinese;
    /**
     * 编码
     */
    private String code;
    /**
     * 数据特征值
     */
    private Integer characteristic;
    /**
     * 单位
     */
    private String unit;
    /**
     * 数据类型
     */
    private Integer type;
    /**
     * 对应数采参数
     */
    private String acquisition;

}