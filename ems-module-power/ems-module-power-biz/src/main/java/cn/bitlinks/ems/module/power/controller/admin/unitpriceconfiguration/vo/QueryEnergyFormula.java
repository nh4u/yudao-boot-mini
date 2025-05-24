package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author wangl
 * @date 2025年05月22日 19:42
 */
@Data
public class QueryEnergyFormula {

    private Long energyId;

    private LocalDateTime aggregateTime;
}
