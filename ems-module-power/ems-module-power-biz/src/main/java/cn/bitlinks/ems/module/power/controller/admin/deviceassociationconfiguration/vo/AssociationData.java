package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AssociationData {

    @Schema(description = "计量器具id")
    private  Long standingbookId;
    @Schema(description = "计量器具名称")
    private  String standingbookName;
    @Schema(description = "计量器具编码")
    private  String standingbookCode;
    @Schema(description = "计量器具环节")
    private  String stage;
    
}
