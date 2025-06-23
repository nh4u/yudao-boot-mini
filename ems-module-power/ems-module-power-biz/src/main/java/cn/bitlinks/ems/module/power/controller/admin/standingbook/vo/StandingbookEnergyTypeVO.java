package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 台账 / 分类 / 能源关系
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookEnergyTypeVO {


    /**
     * 台账ID
     */
    private Long standingbookId;

    /**
     * 分类Id
     */
    private Long typeId;


    /**
     * 能源Id
     */
    private Long energyId;




}
