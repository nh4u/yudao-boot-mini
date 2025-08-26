package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 首页顶部返回结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class StatisticsHomeTopResultVO {


    @Schema(description = "计量器具总数", example = "计量器具总数")
    private Long measurementInstrumentNum;

    @Schema(description = "重点设备总数", example = "重点设备总数")
    private Long keyEquipmentNum;

    @Schema(description = "其他设备总数", example = "其他设备总数")
    private Long otherEquipmentNum;

}
