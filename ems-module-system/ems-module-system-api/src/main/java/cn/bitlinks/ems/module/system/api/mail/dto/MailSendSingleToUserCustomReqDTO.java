package cn.bitlinks.ems.module.system.api.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Schema(description = "RPC 服务 - 邮件发送给 Admin 或者 Member 用户 Request DTO")
@Data
public class MailSendSingleToUserCustomReqDTO {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED, example = "15601691300")
    @Email
    private String mail;

    @Schema(description = "邮件模板编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER_SEND")
    @NotNull(message = "邮件模板编号不能为空")
    private String templateCode;

    @Schema(description = "邮件模板标题")
    @NotNull(message = "邮件模板标题不能为空")
    private String title;

    @Schema(description = "邮件模板内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER_SEND")
    @NotNull(message = "邮件模板内容不能为空")
    private String content;

    @Schema(description = "邮件模板id", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER_SEND")
    @NotNull(message = "邮件模板id不能为空")
    private Long templateId;

    @Schema(description = "邮件模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "USER_SEND")
    @NotNull(message = "邮件模板名称不能为空")
    private String templateName;

}
