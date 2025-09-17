package cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 双碳接口 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DoubleCarbonSettingsRespVO {
    @Schema(description = "id")
    private Long id;
    @Schema(description = "名称")
    private String name;
    @Schema(description = "url")
    private String url;
    @Schema(description = "更新频率")
    private Integer updateFrequency;
    @Schema(description = "更新频率单位")
    private Integer updateFrequencyUnit;

    @Schema(description = "上次执行时间")
    private LocalDateTime lastSyncTime;

}
