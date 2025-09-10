package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PowerMonthPlanSettingsPageReqVO extends PageParam {

    @Schema(description = "能源名称")
    private String energyName;


}