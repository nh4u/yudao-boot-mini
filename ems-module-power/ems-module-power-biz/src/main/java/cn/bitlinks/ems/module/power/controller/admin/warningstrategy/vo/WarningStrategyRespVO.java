package cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo;

import cn.bitlinks.ems.module.power.dal.dataobject.warningstrategy.WarningStrategyDO;
import cn.bitlinks.ems.module.system.api.user.dto.AdminUserRespDTO;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 告警策略查询详情 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WarningStrategyRespVO extends WarningStrategyDO {


    @Schema(description = "已选设备范围")
    private List<DeviceScopeVO> deviceScopeList;

    @Schema(description = "站内信所选用户")
    private List<AdminUserRespDTO> siteStaffList;
    @Schema(description = "邮箱通知所选用户")
    private List<AdminUserRespDTO> mailStaffList;
}
