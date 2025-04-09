package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;
import cn.bitlinks.ems.framework.excel.core.annotations.DictFormat;
import cn.bitlinks.ems.framework.excel.core.convert.DictConvert;

@Schema(description = "管理后台 - 单价配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class UnitPriceConfigurationRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "21056")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", example = "29649")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "开始时间")
    @ExcelProperty("开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @ExcelProperty("结束时间")
    private LocalDateTime endTime;

    @Schema(description = "计费方式")
    @ExcelProperty(value = "计费方式", converter = DictConvert.class)
    @DictFormat("billing_method") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer billingMethod;

    @Schema(description = "核算频率")
    @ExcelProperty(value = "核算频率", converter = DictConvert.class)
    @DictFormat("accounting_frequency") // TODO 代码优化：建议设置到对应的 DictTypeConstants 枚举类中
    private Integer accountingFrequency;

    @Schema(description = "单价详细")
    private List<PriceDetailRespVO> priceDetails;

    @Schema(description = "计算公式")
    @ExcelProperty("计算公式")
    private String formula;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}