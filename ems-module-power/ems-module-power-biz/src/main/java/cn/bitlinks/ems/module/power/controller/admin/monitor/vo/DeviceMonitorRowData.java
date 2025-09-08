package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;


@Schema(description = "设备监控- 时间对应数据")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceMonitorRowData {

    @Schema(description = "时间", example = "0.00")
    private String time;
    @Schema(description = "台账图", example = "0.00")
    private List<DeviceMonitorTimeRowData> dataList;
    @Schema(description = "汇总值", example = "0.00")
    private BigDecimal sum;
}