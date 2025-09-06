package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Schema(description = "设备监控- 时间对应数据")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceMonitorTimeRowData {
    @Schema(description = "台账数据项", example = "0.00")
    private String name;
    @Schema(description = "数据项对应台账id", example = "0.00")
    private Long sbId;
    @Schema(description = "折标煤", example = "0.00")
    private BigDecimal value;
}