package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author liumingqiang
 */
@Data
public class TransformerUtilizationSettingsSortedDTO extends TransformerUtilizationSettingsDTO {
    /**
     * 顶部台账分类排序
     */
    private Long sort;
    /**
     * 下级台账分类排序
     */
    private Long childSort;

    /**
     * 台账创凯你时间
     */
    private Long sbId;

}
