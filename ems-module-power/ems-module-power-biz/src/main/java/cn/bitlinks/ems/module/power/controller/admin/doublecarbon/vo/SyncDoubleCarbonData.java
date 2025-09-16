package cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 双碳接口 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SyncDoubleCarbonData {

    @Schema(description = "双碳编码")
    private String doubleCarbonCode;

    @Schema(description = "同步数采数据")
    private BigDecimal usage;

    @Schema(description = "采集时间")
    private Long acqTimeLong;
}
