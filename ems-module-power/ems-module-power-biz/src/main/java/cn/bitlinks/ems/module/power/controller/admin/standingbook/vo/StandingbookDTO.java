package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
public class StandingbookDTO {
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 台账分类id
     */
    private Long typeId;
    /**
     * 台账编码
     */
    private String code;
    /**
     * 台账名称
     */
    private String name;
}
