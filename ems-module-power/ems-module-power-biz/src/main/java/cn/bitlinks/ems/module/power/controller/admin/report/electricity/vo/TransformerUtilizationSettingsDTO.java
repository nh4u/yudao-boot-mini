package cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo;

import lombok.Data;

@Data
public class TransformerUtilizationSettingsDTO extends TransformerUtilizationSettingsVO {

    private String loadCurrentParamCode;

    private String type;

    private String childType;

    private String transformerName;
}
