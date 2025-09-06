package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;


/**
 * @author liumingqiang
 */
@Schema(description = "设备监控- 聚合数据")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class DeviceMonitorAggData {

    @Schema(description = "累计折标煤", example = "0.00")
    private BigDecimal accCoal;
    @Schema(description = "累计折标煤", example = "0.00")
    private BigDecimal accUsage;
    @Schema(description = "累计成本", example = "0.00")
    private BigDecimal accCost;
}