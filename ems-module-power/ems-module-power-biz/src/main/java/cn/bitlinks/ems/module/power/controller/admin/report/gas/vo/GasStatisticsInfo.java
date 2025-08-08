package cn.bitlinks.ems.module.power.controller.admin.report.gas.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author bmqi
 * @date 2025年08月07日 14:14
 */
@Data
@NoArgsConstructor
@Schema(description = "管理后台 - 气化科报表统计结果信息 VO")
public class GasStatisticsInfo {

    @Schema(description = "能源统计项", example = "123")
    private String measurementName;

    @Schema(description = "计量器具编码", example = "123")
    private String measurementCode;

    @Schema(description = "数据", example = "数据")
    private List<GasStatisticsInfoData> statisticsDateDataList;
}
