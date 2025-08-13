package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import lombok.Data;

/**
 * 气化科计量器具信息
 *
 * @author bmqi
 */
@Data
public class GasMeasurementInfo {

    /**
     * 台账ID
     */
    private Long standingbookId;

    /**
     * 台账类型ID
     */
    private Long typeId;

    /**
     * 计量器具名称
     */
    private String measurementName;

    /**
     * 计量器具编码
     */
    private String measurementCode;

    /**
     * 参数编码
     */
    private String paramCode;

    /**
     * 计算类型
     */
    private Integer calculateType;

    /**
     * 能源参数中文名
     */
    private String energyParam;

    /**
     * 排序号
     */
    private Integer sortNo;
}
