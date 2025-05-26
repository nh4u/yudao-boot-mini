package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 虚拟表关联计量器具修改 Request VO")
@Data
public class MeasurementVirtualAssociationSaveReqVO {


    @Schema(description = "计量器具id", example = "25507")
    private Long measurementInstrumentId;

    @Schema(description = "关联下级计量")
    private List<Long> measurementIds;

}