package cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 双谈接口 设置upd VO")
@Data
@ExcelIgnoreUnannotated
public class DoubleCarbonSettingsUpdVO {
    @Schema(description = "id")
    @NotNull
    private Long id;

    @Schema(description = "更新频率")
    private Integer updateFrequency;
    @Schema(description = "更新频率单位")
    private Integer updateFrequencyUnit;

}
