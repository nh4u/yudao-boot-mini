package cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 双谈接口 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DoubleCarbonMappingRespVO {
    @Schema(description = "映射id")
    private Long id;
    @Schema(description = "台账编码")
    private String standingbookCode;
    @Schema(description = "双碳编码")
    private Integer doubleCarbonCode;

}
