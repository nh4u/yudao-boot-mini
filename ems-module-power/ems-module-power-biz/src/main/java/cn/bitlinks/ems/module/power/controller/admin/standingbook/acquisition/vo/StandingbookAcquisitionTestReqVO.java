package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 台账-数采设置测试部分 Request VO")
@Data
public class StandingbookAcquisitionTestReqVO {

    @Schema(description = "服务设置id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long serviceSettingsId;

    @Schema(description = "OPCDA：io地址/MODBUS：")
    private String dataSite;

    @Schema(description = "公式")
    private String formula;


}
