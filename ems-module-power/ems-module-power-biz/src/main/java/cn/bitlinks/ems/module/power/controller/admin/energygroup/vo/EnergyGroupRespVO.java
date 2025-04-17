package cn.bitlinks.ems.module.power.controller.admin.energygroup.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

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

}