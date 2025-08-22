package cn.bitlinks.ems.module.power.controller.admin.externalapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;


/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 外部数据接口管理 Request VO")
@Data
public class ExternalApiSaveReqVO {

    @Schema(description = "编号", example = "3687")
    private Long id;

    @Schema(description = "接口名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "接口名称不能为空")
    private String name;

    @Schema(description = "接口编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "接口编码不能为空")
    private String code;

    @Schema(description = "接口地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "接口地址不能为空")
    private String url;

    @Schema(description = "请求方式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请求方式不能为空")
    private String method;

    @Schema(description = "body")
    private String body;

}