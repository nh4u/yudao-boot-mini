package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 设备关联配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DeviceAssociationConfigurationPageReqVO extends PageParam {

    @Schema(description = "能源id", example = "7602")
    private Long energyId;

    @Schema(description = "计量器具id", example = "10771")
    private Long measurementInstrumentId;

    @Schema(description = "设备id", example = "22446")
    private Long deviceId;

    @Schema(description = "后置计量")
    private String postMeasurement;

    @Schema(description = "前置计量")
    private String preMeasurement;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}