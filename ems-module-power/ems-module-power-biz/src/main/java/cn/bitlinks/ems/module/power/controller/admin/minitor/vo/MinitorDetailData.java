package cn.bitlinks.ems.module.power.controller.admin.minitor.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MinitorDetailData {

    @Schema(description = "时间")
    String time;

    @Schema(description = "参数值")
    private BigDecimal value;


}
