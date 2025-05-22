package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 台账 / 分类 / 能源关系
 */
@Data
public class EnergyTimeResultVO {

    /**
     * 单价设置ID
     */
    private Long id;

    /**
     * 核算频率  |  1：按月   2：按季   3：按年
     */
    private Integer accountingArequency;


    /**
     * 计费方式  |  1：统一计价  2：分时段计价  3：阶梯计价
     */
    private Integer billingMethod;


    /**
     * 能源ID
     */
    private Long energyId;


    /**
     * 折标煤系数有效期-开始时间
     */
    private LocalDateTime startTime;

    /**
     * 折标煤系数有效期-结束时间
     */
    private LocalDateTime endTime;

    /**
     * 公式ID
     */
    private Long formulaId;

    /**
     * 计算公式
     */
    private String energyFormula;

    /**
     * 小数点
     */
    private Integer formulaScale;
}
