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

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "5111")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", example = "10576")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "中文名")
    @ExcelProperty("中文名")
    private String chinese;

    @Schema(description = "编码")
    @ExcelProperty("编码")
    private String code;

    @Schema(description = "数据特征值")
    @ExcelProperty("数据特征值")
    private Integer characteristic;

    @Schema(description = "单位")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "数据类型", example = "2")
    @ExcelProperty("数据类型")
    private Integer type;

    @Schema(description = "对应数采参数")
    @ExcelProperty("对应数采参数")
    private String acquisition;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}