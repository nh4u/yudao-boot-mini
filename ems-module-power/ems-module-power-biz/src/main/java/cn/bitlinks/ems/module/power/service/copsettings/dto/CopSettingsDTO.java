package cn.bitlinks.ems.module.power.service.copsettings.dto;

import lombok.Data;

@Data
public class CopSettingsDTO {

    /**
     * 低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS
     */
    private String copType;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 公式对应的参数
     */
    private String param;
    /**
     * 参数编码
     */
    private String paramCode;
    /**
     * 参数中文名
     */
    private String paramCnName;
    /**
     * 数据特征 1累计值2稳态值3状态值
     **/
    private Integer dataFeature;
}
