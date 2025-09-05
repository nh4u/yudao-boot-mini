package cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo;

import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 大屏 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class BigScreenRespVO {

    @Schema(description = "台账list")
    private CopChartResultVO copChart;

}
