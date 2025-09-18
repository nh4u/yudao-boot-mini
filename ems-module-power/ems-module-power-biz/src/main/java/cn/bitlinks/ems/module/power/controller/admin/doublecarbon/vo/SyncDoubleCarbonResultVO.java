package cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 *
 * 定时同步 全量和增量数据 （目前还未使用）
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 双碳接口 Response VO")
@Data
@ExcelIgnoreUnannotated
public class SyncDoubleCarbonResultVO {

    @Schema(description = "全量list")
    private List<SyncDoubleCarbonData> full;

    @Schema(description = "增量list")
    private List<SyncDoubleCarbonData> incremental;

}
