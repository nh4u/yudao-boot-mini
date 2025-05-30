package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author wangl
 * @date 2025年05月12日 9:53
 */
@Schema(description = "管理后台 - 能流图统计结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class EnergyFlowResultVO {

    /**
     * 能流图能源数据
     */
    @Schema(description = "能流图能源数据")
    private List<EnergyItemData> data;

    /**
     * 能流图link数据
     */
    @Schema(description = "能流图link数据")
    private List<EnergyLinkData> links;

    /**
     * 数据最后更新时间
     */
    @Schema(description = "数据最后更新时间")
    private LocalDateTime dataTime;


}
