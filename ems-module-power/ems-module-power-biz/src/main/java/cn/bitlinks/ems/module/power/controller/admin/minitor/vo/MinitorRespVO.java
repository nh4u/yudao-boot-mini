package cn.bitlinks.ems.module.power.controller.admin.minitor.vo;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class MinitorRespVO {

    @Schema(description = "台账list")
    List<StandingbookRespVO> standingbookRespVOList;

    @Schema(description = "全部")
    private Integer total;

    @Schema(description = "能耗正常")
    private Integer normal;

    @Schema(description = "能耗异常")
    private Integer warning;

}
