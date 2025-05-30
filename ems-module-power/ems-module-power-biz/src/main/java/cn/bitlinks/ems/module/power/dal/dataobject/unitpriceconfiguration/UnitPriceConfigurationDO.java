package cn.bitlinks.ems.module.power.dal.dataobject.unitpriceconfiguration;

import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import com.sun.xml.bind.v2.TODO;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 单价配置 DO
 *
 * @author bitlinks
 */
@TableName("ems_unit_price_configuration")
@KeySequence("ems_unit_price_configuration_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitPriceConfigurationDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 能源id
     */
    private Long energyId;
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
    @TableField(exist = false)
    private List<PriceDetailDO> priceDetails;
    /**
     * 核算频率
     *
     * 枚举 {@link TODO accounting_frequency 对应的类}
     */
    private Integer accountingFrequency;
    /**
     * 计算公式
     */
    private String formula;
    /**
     * 公式id
     */
    private  Long formulaId;

}