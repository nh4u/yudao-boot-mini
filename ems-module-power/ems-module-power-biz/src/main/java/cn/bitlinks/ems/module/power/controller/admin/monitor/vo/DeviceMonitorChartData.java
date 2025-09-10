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
public class DeviceMonitorChartData {

    @Schema(description = "关联的计量器具的名称")
    private String name;
    @Schema(description = "关联的计量器具的id")
    private Long sbId;
    @Schema(description = "时间数据")
    private List<BigDecimal> dataList;
    @Schema(description = "bar/line")
    private String type;

}