package cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration;

import com.sun.xml.bind.v2.TODO;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;


/**
 * 能源配置 DO
 *
 * @author bitlinks
 */
@TableName("ems_energy_configuration")
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
     * 能源名称
     */
    private String energyName;
    /**
     * 编码
     */
    private String code;
    /**
     * 能源分类
     *
     * 枚举 {@link TODO energy_classify 对应的类}
     */
    private Integer energyClassify;
    /**
     * 能源图标
     */
    private String energyIcon;
    /**
     * 能源参数
     */
    private String energyParameter;
    /**
     * 折标煤系数
     */
    private BigDecimal factor;
    /**
     * 折标煤公式
     */
    private String coalFormula;
    /**
     * 折标煤小数位数
     */
    private String coalScale;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 计费方式
     *
     * 枚举 {@link TODO billing_method 对应的类}
     */
    private Integer billingMethod;
    /**
     * 单价详细
     */
    private String unitPrice;
    /**
     * 单价公式
     */
    private String unitPriceFormula;
    /**
     * 单价小数位
     */
    private String unitPriceScale;
}