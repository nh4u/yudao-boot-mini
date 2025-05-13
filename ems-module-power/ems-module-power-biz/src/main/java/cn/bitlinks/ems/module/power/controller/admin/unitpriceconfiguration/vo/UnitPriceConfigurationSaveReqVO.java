package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailSaveReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 单价配置新增/修改 Request VO")
@Data
public class UnitPriceConfigurationSaveReqVO {

//    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "21056")
//    private Long id;

//    @Schema(description = "能源id", example = "29649")
//    private Long energyId;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

//    @Schema(description = "时间范围")
//    private List<LocalDateTime> timeRange;

    @Schema(description = "计费方式")
    private Integer billingMethod;

    @Schema(description = "核算频率")
    private Integer accountingFrequency;

    @Schema(description = "单价详细")
    private List<PriceDetailSaveReqVO> priceDetails;

    @Schema(description = "计算公式")
    private String formula;

    @Schema(description = "关联计算公式id")
    private Long formulaId;

}