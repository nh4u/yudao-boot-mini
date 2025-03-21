package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Schema(description = "管理后台 - 台账属性 Response VO")
@Data
@ExcelIgnoreUnannotated
public class StandingbookAssociationReqVO {


    @Schema(description = "topType 2-计量器具 1-重点设备")
    @NotNull
    private Integer topType;

    @Schema(description = "台账id")
    @NotNull
    private Long sbId;

    @Schema(description = "多条件查询台账条件")
    private Map<String, String> pageReqVO;
}
