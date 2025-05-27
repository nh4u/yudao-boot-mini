package cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 折标煤系数和公式
 * @author wangl
 * @date 2025年05月22日 14:35
 */
@Data
public class CoalFactorFormulaData {

    /**
     * 能源ID
     */
    private Long energyId;           // h.energy_id

    /**
     * 折标煤系数
     */
    private BigDecimal factor;       // h.factor

    /**
     * 折标煤系数有效期-开始时间
     */
    private LocalDateTime startTime; // h.start_time

    /**
     * 折标煤系数有效期-结束时间
     */
    private LocalDateTime endTime;   // h.end_time

    /**
     * 公式ID
     */
    private Long formulaId;          // h.formula_id

    /**
     * 计算公式
     */
    private String energyFormula;    // f.energy_formula

    /**
     * 小数点
     */
    private Integer formulaScale;    // f.formula_scale

    /**
     * 时间
     */
    private LocalDateTime aggregateTime;
}
