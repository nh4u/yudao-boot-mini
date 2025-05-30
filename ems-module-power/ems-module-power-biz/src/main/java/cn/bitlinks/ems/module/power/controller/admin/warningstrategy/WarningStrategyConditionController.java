package cn.bitlinks.ems.module.power.controller.admin.warningstrategy;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.AttributeTreeNode;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.AttributeTreeReqVO;
import cn.bitlinks.ems.module.power.service.warningstrategy.WarningStrategyConditionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 告警策略条件")
@RestController
@RequestMapping("/power/warning-strategy-condition")
@Validated
public class WarningStrategyConditionController {
    @Resource
    private WarningStrategyConditionService warningStrategyConditionService;


    @PostMapping("/getConditionTree")
    @Operation(summary = "根据台账ids和台账类型ids查询台账属性树形结构")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy-condition:query')")
    public CommonResult<List<AttributeTreeNode>> queryDaqTreeNodeByTypeAndSb(@RequestBody AttributeTreeReqVO reqVO) {
        return success(warningStrategyConditionService.queryDaqTreeNodeByTypeAndSb(reqVO.getSbIds(),
                reqVO.getTypeIds()));
    }


}
