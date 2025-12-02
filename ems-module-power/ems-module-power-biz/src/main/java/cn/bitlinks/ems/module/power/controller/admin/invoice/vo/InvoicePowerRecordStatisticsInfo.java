package cn.bitlinks.ems.module.power.controller.admin.invoice.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 发票电量记录统计信息 VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class InvoicePowerRecordStatisticsInfo {

    @Schema(description = "id，用于排序")
    private Long id;

    @Schema(description = "统计项名称（比如电表编码）")
    private String name;

    @Schema(description = "按月份展开的数据列表")
    private List<InvoicePowerRecordStatisticsData> statisticsDateDataList;

}
