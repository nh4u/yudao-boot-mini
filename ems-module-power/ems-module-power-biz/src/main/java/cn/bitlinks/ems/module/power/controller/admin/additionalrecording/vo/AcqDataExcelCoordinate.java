package cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/09/26 09:57
 **/
@Schema(description = "管理后台 - excel坐标 Response VO")
@Data
public class AcqDataExcelCoordinate {

    @Schema(description = "采集点起始")
    private String acqNameStart;

    @Schema(description = "采集点结束")
    private String acqNameEnd;

    @Schema(description = "时间起始")
    private String acqTimeStart;

    @Schema(description = "时间结束")
    private String acqTimeEnd;
}

