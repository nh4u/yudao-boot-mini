package cn.bitlinks.ems.framework.common.core;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 管理后台 - 台账-数采设置详细
 */
@Data
public class StandingbookAcquisitionDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L; // 推荐指定序列化版本

    /**
     * 设备数采启停开关（0：关；1开。）
     **/
    @NotNull
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
    @NotEmpty
    private String code;

    /**
     * 是否能源数采参数 0自定义数采 1能源数采
     **/
    @NotNull
    private Boolean energyFlag;
    /**
     * modbus从地址
     */
    private String modbusSalve;
    /**
     * modbus寄存器地址
     */
    private String modbusRegisterType;

    /* 参数部分START */
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
    @NotNull
    private Integer usage;

}
