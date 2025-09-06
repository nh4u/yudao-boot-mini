package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author liumingqiang
 */
@Data
public class StandingbookEnergyReqVO extends StandingbookEnergyParamReqVO {

    /**
     * 能源编码（完全匹配）
     */
    @Schema(description = "能源编码（完全匹配）")
    @NotEmpty(message = "能源编码不能为空")
    private List<String> energyCodes;
}
