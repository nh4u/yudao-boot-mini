package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import lombok.Data;

/**
 * 台账 / 分类 / 能源关系
 */
@Data
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
