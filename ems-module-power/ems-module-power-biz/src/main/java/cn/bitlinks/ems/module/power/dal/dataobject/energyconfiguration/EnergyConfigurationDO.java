package cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyparameters.EnergyParametersDO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration.UnitPriceConfigurationDO;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.sun.xml.bind.v2.TODO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 能源配置 DO
 *
 * @author bitlinks
 */
@TableName(value = "ems_energy_configuration", autoResultMap = true)
@KeySequence("ems_energy_configuration_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnergyConfigurationDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 分組id
     */
    private Long groupId;
    /**
     * 能源名称
     */
    private String energyName;
    /**
     * 能源名称
     */
    @TableField(exist = false)
    private String name;
    /**
     * 编码
     */
    private String code;
    /**
     * 能源分类
     * <p>
     * 枚举 {@link TODO energy_classify 对应的类}
     */
    private Integer energyClassify;
    /**
     * 能源图标
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, String> energyIcon;
    /**
     * 能源参数
     */
    @TableField(exist = false)
    private List<EnergyParametersDO> energyParameters;
    /**
     * 折标煤系数
     */
    private BigDecimal factor;
    /**
     * 单价详细
     */
    //private String unitPrice;
    @TableField(exist = false)
    private UnitPriceConfigurationDO unitPrice;

    @TableField(exist = false)
    List<EnergyConfigurationDO> children = new ArrayList<>();

    public void addChild(EnergyConfigurationDO child) {
        children.add(child);
    }
}