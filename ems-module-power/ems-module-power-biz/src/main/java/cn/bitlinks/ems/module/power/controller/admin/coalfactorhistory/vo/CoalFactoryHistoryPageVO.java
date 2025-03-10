package cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class CoalFactoryHistoryPageVO {
    private CoalFactorHistoryPageReqVO coalFactorHistoryPageReqVO;
    private Long energyId;
}
