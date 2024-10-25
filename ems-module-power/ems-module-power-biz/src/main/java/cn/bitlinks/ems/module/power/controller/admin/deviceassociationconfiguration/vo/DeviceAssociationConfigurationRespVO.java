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

    @Schema(description = "id", requiredMode = Schema.RequiredMode.REQUIRED, example = "26859")
    @ExcelProperty("id")
    private Long id;

    @Schema(description = "能源id", example = "7602")
    @ExcelProperty("能源id")
    private Long energyId;

    @Schema(description = "计量器具id", example = "10771")
    @ExcelProperty("计量器具id")
    private Long measurementInstrumentId;

    @Schema(description = "设备id", example = "22446")
    @ExcelProperty("设备id")
    private Long deviceId;

    @Schema(description = "后置计量")
    @ExcelProperty("后置计量")
    private String postMeasurement;

    @Schema(description = "前置计量")
    @ExcelProperty("前置计量")
    private String preMeasurement;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}