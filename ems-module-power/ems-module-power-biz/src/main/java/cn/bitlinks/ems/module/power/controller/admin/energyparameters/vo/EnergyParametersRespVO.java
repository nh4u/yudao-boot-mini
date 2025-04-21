package cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 能源参数 Response VO")
@Data
@ExcelIgnoreUnannotated
public class EnergyParametersRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "26158")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", example = "2682")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "参数名称")
    @ExcelProperty("参数名称")
    private String parameter;

    @Schema(description = "编码")
    @ExcelProperty("编码")
    private String code;

    @Schema(description = "数据特征")
    @ExcelProperty("数据特征")
    private Integer dataFeature;

    @Schema(description = "单位")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "数据类型")
    @ExcelProperty("数据类型")
    private Integer dataType;

    @Schema(description = "用量")
    @ExcelProperty("用量")
    private Integer usage;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}