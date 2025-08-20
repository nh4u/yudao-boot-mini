package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 用能统计结果信息 VO")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class StructureInfo extends StatisticsInfoBase {

    @Schema(description = "数据", example = "数据")
    private List<StructureInfoData> structureInfoDataList;

    @Schema(description = "合计折标煤/折价", example = "0.00")
    private BigDecimal sumNum;

    @Schema(description = "合计占比", example = "0.00")
    private BigDecimal sumProportion;
}
