package cn.bitlinks.ems.module.power.controller.admin.warningstrategy;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.SbDataTriggerVO;
import cn.bitlinks.ems.module.power.service.warningstrategy.WarningStrategyTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 告警策略触发测试")
@RestController
@RequestMapping("/power/warning-strategy-trigger")
@Validated
public class WarningStrategyTriggerController {

    @Resource
    private WarningStrategyTriggerService warningStrategyTriggerService;

    @PostMapping("/testTrigger")
    @Operation(summary = "测试告警策略触发")
    public CommonResult<Boolean> triggerWarning(@Valid @RequestBody List<SbDataTriggerVO> sbDataTriggerVOList) {
        warningStrategyTriggerService.triggerWarning(sbDataTriggerVOList);
        return success(true);
    }

}