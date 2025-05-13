package cn.bitlinks.ems.module.power.controller.admin.energygroup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 能源分组新增/修改 Request VO")
@Data
public class EnergyGroupSaveReqVO {

    @Schema(description = "编号", example = "3495")
    private Long id;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "水")
    @NotEmpty(message = "分组名称不能为空")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

}