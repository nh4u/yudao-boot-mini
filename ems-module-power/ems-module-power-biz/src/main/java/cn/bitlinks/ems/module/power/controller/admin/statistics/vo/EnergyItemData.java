package cn.bitlinks.ems.module.power.controller.admin.statistics.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/05/25 18:45
 **/

@Data
@Schema(description = "能流图能源数据")
public class EnergyItemData {

    @Schema(description = "能源名称")
    private String name;

    // 可能会加个顺序 第几栏第几栏

}
