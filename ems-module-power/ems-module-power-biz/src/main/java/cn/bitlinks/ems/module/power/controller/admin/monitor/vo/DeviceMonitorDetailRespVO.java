package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 设备监控图表总数据 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceMonitorDetailRespVO {
    @Schema(description = "用量")
    private BigDecimal sumUsage;

    @Schema(description = "折标煤")
    private BigDecimal sumCoal;

    @Schema(description = "成本")
    private BigDecimal sumCost;

    @Schema(description = "动态表头标题")
    List<String> tableHeaders;

    @Schema(description = "动态图表数据-用量")
    List<DeviceMonitorRowData> usageData;
    @Schema(description = "动态图表数据-折标煤")
    List<DeviceMonitorRowData> coalData;
    @Schema(description = "动态图表数据-成本")
    List<DeviceMonitorRowData> costData;


    @Schema(description = "图-x轴")
    List<String> xdata;


}
