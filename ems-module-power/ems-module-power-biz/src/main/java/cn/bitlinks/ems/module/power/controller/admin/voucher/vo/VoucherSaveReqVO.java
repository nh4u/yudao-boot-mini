package cn.bitlinks.ems.module.power.controller.admin.voucher.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 凭证管理新增/修改 Request VO")
@Data
public class VoucherSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3687")
    private Long id;



    @Schema(description = "凭证编号")
    private String code;

    @Schema(description = "凭证名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @NotEmpty(message = "凭证名称不能为空")
    private String name;

    @Schema(description = "能源id", requiredMode = Schema.RequiredMode.REQUIRED, example = "5445")
    @NotNull(message = "能源id不能为空")
    private Long energyId;

    @Schema(description = "能源name", example = "bitlinks")
    private String energyName;

    @Schema(description = "购入时间")
    private LocalDateTime purchaseTime;

    @Schema(description = "经办人")
    private String attention;

    @Schema(description = "金额", example = "9217")
    private BigDecimal price;

    @Schema(description = "用量")
    private BigDecimal usage;

    @Schema(description = "用量单位")
    private String usageUnit;

    @Schema(description = "描述", example = "随便")
    private String description;

    @Schema(description = "附件名称", example = "王五")
    private String appendixName;

    @Schema(description = "附件地址", example = "https://www.bitlinks.cn")
    private String appendixUrl;

    @Schema(description = "识别结果")
    private String results;

    @Schema(description = "凭证附件")
    private String appendix;

    @Schema(description = "ids")
    private List<Long> ids;

}