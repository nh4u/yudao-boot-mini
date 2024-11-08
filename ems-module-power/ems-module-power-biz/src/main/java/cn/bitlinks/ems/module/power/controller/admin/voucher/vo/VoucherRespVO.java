package cn.bitlinks.ems.module.power.controller.admin.voucher.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 凭证管理 Response VO")
@Data
@ExcelIgnoreUnannotated
public class VoucherRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "凭证编号")
    @ExcelProperty("凭证编号")
    private String code;

    @Schema(description = "凭证名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @ExcelProperty("凭证名称")
    private String name;

    @Schema(description = "能源id", requiredMode = Schema.RequiredMode.REQUIRED, example = "5445")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "购入时间")
    @ExcelProperty("购入时间")
    private LocalDateTime purchaseTime;

    @Schema(description = "经办人")
    @ExcelProperty("经办人")
    private String attention;

    @Schema(description = "金额", example = "9217")
    @ExcelProperty("金额")
    private BigDecimal price;

    @Schema(description = "用量")
    @ExcelProperty("用量")
    private BigDecimal usage;

    @Schema(description = "用量单位")
    @ExcelProperty("用量单位")
    private String usageUnit;

    @Schema(description = "描述", example = "随便")
    @ExcelProperty("描述")
    private String description;

    @Schema(description = "附件名称", example = "王五")
    @ExcelProperty("附件名称")
    private String appendixName;

    @Schema(description = "附件地址", example = "https://www.bitlinks.cn")
    @ExcelProperty("附件地址")
    private String appendixUrl;

    @Schema(description = "识别结果")
    @ExcelProperty("识别结果")
    private String results;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("更新时间")
    private LocalDateTime updateTime;

}