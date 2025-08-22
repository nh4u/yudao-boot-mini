package cn.bitlinks.ems.module.power.controller.admin.statistics.deviation;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationChartYInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationStatisticsParamVO;
import cn.bitlinks.ems.module.power.service.statistics.deviation.DeviationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;


/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 用能分析/偏离度分析")
@RestController
@RequestMapping("/power/statistics")
@Validated
public class DeviationStatisticsController {

    @Resource
    private DeviationService deviationService;

    @PostMapping("/deviationChart")
    @Operation(summary = "偏离度分析（图）")
    public CommonResult<DeviationChartResultVO<DeviationChartYInfo>> deviationChart(@Valid @RequestBody DeviationStatisticsParamVO paramVO) {
        return success(deviationService.deviationChart(paramVO));

    }


}
