package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;

@Schema(description = "管理后台 - 关联计量器具修改 Request VO")
@Data
public class MeasurementAssociationSaveReqVO {


    @Schema(description = "计量器具id", example = "25507")
    private Long measurementInstrumentId;

    @Schema(description = "关联下级计量")
    private List<Long> measurementIds;

}