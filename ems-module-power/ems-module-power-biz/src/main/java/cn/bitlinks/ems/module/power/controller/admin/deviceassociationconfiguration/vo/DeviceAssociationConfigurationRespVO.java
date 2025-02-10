package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import com.alibaba.excel.annotation.*;

@Schema(description = "管理后台 - 设备关联配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeviceAssociationConfigurationRespVO {

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "18547")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", example = "22906")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "计量器具id", example = "25507")
    @ExcelProperty("计量器具id")
    private Long measurementInstrumentId;

    @Schema(description = "关联下级计量")
    @ExcelProperty("关联下级计量")
    private String measurementIds;

    @Schema(description = "关联设备", example = "32363")
    @ExcelProperty("关联设备")
    private Long deviceId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}