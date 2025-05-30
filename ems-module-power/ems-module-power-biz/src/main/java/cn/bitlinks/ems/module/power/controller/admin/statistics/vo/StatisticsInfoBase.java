package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author wangl
 * @date 2025年05月12日 11:10
 */
@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 用能统计结果信息 VO")
public class StatisticsInfoBase {

    @Schema(description = "一级标签", example = "1#厂房")
    private String label1;

    @Schema(description = "二级标签", example = "一层")
    private String label2;

    @Schema(description = "三级标签", example = "101室")
    private String label3;

    @Schema(description = "能源", example = "能源")
    private String energyName;

    @Schema(description = "任意级标签Id", example = "1", hidden = true)
    private Long labelId;

    @Schema(description = "能源Id", example = "1", hidden = true)
    private Long energyId;

}
