package cn.bitlinks.ems.module.power.controller.admin.externalapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 外部数据接口管理 Response VO")
@Data
public class ExternalApiRespVO{

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    private Long id;

    @Schema(description = "接口名称")
    private String name;

    @Schema(description = "接口编码")
    private String code;

    @Schema(description = "接口地址")
    private String url;

    @Schema(description = "请求方式")
    private String method;

    @Schema(description = "body")
    private String body;

}