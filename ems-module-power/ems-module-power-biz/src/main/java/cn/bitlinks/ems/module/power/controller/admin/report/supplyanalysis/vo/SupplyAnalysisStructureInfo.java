package cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StructureInfoData;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 供应分析结果信息 VO")
public class SupplyAnalysisStructureInfo {

    @Schema(description = "id 用于排序")
    private Long id;

    @Schema(description = "系统")
    @NotEmpty(message = "系统不能为空")
    private String system;

    @Schema(description = "分析项")
    @NotEmpty(message = "分析项不能为空")
    private String item;

    @Schema(description = "数据", example = "数据")
    private List<StructureInfoData> structureInfoDataList;

    @Schema(description = "合计折标煤/折价", example = "0.00")
    private BigDecimal sumNum;

    @Schema(description = "合计占比", example = "0.00")
    private BigDecimal sumProportion;
}
