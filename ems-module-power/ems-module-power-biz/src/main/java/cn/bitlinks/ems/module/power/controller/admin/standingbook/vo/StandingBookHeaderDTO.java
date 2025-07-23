package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
public class StandingBookHeaderDTO extends StandingbookDTO {

    /**
     * Excel表头
     */
    private String header;

}
