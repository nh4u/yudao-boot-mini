package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 台账-数采设置测试部分 Request VO")
@Data
public class StandingbookAcquisitionTestReqVO extends StandingbookAcquisitionVO {

    @Schema(description = "当前设备的参数设置", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "当前设备的参数设置不能为空")
    StandingbookAcquisitionDetailVO currentDetail;
}
