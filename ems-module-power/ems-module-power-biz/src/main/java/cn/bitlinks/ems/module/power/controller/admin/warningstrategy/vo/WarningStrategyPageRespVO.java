package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import cn.bitlinks.ems.framework.excel.core.annotations.DictFormat;
import cn.bitlinks.ems.framework.excel.core.convert.DictConvert;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 告警策略 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarningStrategyPageRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "27747")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "规则名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "zzc")
    @ExcelProperty("规则名称")
    private String name;

    @Schema(description = "描述", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("描述")
    private String description;

    @Schema(description = "告警等级：紧急4 重要3 次要2 警告1 提示0", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "告警等级：紧急4 重要3 次要2 警告1 提示0", converter = DictConvert.class)
    @DictFormat("warning_level") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer level;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private String creatorName;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;
}