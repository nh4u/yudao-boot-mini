package cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "管理后台 - 折标煤系数历史新增/修改 Request VO")
@Data
public class CoalFactorHistorySaveReqVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "11942")
    private Long id;

    @Schema(description = "能源id", example = "31884")
    private Long energyId;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "生效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "折标煤系数")
    private BigDecimal factor;

    @Schema(description = "关联计算公式")
    private String formula;

    @Schema(description = "修改人")
    private String updater;

}