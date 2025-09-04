package cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class LabelConfigDTO {
    private String topLevelLabelId;
    private String curLabelId;
}
