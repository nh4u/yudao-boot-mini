package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PowerPureWasteWaterGasSettingsPageReqVO extends PageParam {

    @Schema(description = "类型")
    private List<String> systems;

    @Schema(description = "名称", example = "王五")
    private String name;



}