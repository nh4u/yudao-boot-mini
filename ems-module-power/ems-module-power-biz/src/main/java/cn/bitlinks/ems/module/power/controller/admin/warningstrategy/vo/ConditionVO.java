package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 告警策略条件 Request VO")
@Data
public class ConditionVO {
    @Schema(description = "条件参数-属性id，层级id+能源参数编码")
    private List<String> paramId;

    @Schema(description = "条件连接符")
    private Integer connector;

    @Schema(description = "条件值")
    private BigDecimal value;
}
