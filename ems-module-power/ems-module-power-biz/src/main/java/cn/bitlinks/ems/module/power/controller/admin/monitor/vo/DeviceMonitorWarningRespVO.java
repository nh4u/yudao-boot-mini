package cn.bitlinks.ems.module.power.controller.admin.monitor.vo;

import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatisticsRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 设备监控-告警 Response VO")
@Data
@ExcelIgnoreUnannotated
public class DeviceMonitorWarningRespVO {

    @Schema(description = "左侧统计数量")
    private WarningInfoStatisticsRespVO statistics;

    @Schema(description = "告警信息列表")
    private List<WarningInfoDO> list;


}