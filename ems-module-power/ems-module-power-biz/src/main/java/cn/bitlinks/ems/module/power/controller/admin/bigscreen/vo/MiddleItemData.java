package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 中间数据Item VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MiddleItemData {

    @Schema(description = "成本")
    private BigDecimal cost;

    @Schema(description = "折标煤")
    private BigDecimal standardCoal;

}