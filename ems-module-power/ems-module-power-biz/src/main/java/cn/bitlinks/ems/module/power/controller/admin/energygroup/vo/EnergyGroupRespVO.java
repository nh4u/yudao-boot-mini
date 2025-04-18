package cn.bitlinks.ems.module.power.controller.admin.energygroup.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 能源分组 Response VO")
@Data
@ExcelIgnoreUnannotated
public class EnergyGroupRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3495")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @ExcelProperty("分组名称")
    private String name;

    @Schema(description = "分组是否可编辑 true：可编辑；false：不可编辑", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean edited;

}