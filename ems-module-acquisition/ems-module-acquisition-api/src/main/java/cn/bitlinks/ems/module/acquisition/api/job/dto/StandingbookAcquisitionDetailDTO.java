package cn.bitlinks.ems.module.acquisition.api.job.dto;

import lombok.Data;

/**
 * 管理后台 - 台账-数采设置详细 VO
 */
@Data
public class StandingbookAcquisitionDetailDTO {

    /**
     * 设备数采启停开关（0：关；1开。）
     **/
    private Boolean status;

    /**
     * OPCDA：io地址/MODBUS：
     **/
    private String dataSite;

    /**
     * 实际公式,无嵌套
     **/
    private String actualFormula;

    /**
     * 全量/增量（0：全量；1增量。）
     **/
    private Integer fullIncrement;

    /**
     * 参数编码
     **/
    private String code;

    /**
     * 是否能源数采参数 0自定义数采 1能源数采
     **/
    private Boolean energyFlag;

    /* 参数部分START */
    /**
     * 参数名称
     */
    /**
     * 参数名称
     **/
    private String parameter;
    /**
     * 数据特征
     **/
    private Integer dataFeature;

    /**
     * 单位
     **/
    private String unit;

    /**
     * 数据类型
     **/
    private Integer dataType;

    /**
     * 用量
     **/
    private Integer usage;

}
