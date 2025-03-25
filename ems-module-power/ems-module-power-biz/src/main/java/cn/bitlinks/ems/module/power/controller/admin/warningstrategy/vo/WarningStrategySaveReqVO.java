package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 告警策略新增/修改 Request VO")
@Data
public class WarningStrategySaveReqVO {

    @Schema(description = "编号", example = "27747")
    private Long id;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "zzc")
    @NotNull(message = "规则名称不能为空")
    private String name;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "描述不能为空")
    private String description;

    @Schema(description = "设备范围选择")
    private List<DeviceScopeVO> selectScope;


    @Schema(description = "告警条件", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "告警条件不能为空")
    private List<ConditionVO> condition;

    @Schema(description = "告警等级：紧急4 重要3 次要2 警告1 提示0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "告警等级：紧急4 重要3 次要2 警告1 提示0不能为空")
    private Integer level;

    @Schema(description = "站内信模板id", requiredMode = Schema.RequiredMode.REQUIRED, example = "30741")
    @NotNull(message = "站内信模板id不能为空")
    private Long siteTemplateId;

    @Schema(description = "邮件模板id", example = "3143")
    private Long mailTemplateId;

    @Schema(description = "站内信人员", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "站内信人员不能为空")
    private List<Long> siteStaff;

    @Schema(description = "邮件人员")
    private List<Long> mailStaff;

    @Schema(description = "告警间隔", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "告警间隔不能为空")
    private String interval;

    @Schema(description = "告警间隔单位", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "告警间隔单位不能为空")
    private Integer intervalUnit;

}
