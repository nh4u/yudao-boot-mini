package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsService;
import com.alibaba.fastjson.JSONObject;
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
@Tag(name = "管理后台 - 用能分析")
@RestController
@RequestMapping("/power/statistics")
@Validated
public class StatisticsController {

    @Resource
    private StatisticsService statisticsService;

    @PostMapping("/energyFlowAnalysis")
    @Operation(summary = "能流分析")
    public CommonResult<JSONObject> energyFlowAnalysis(@Valid @RequestBody StatisticsParamVO paramVO) {
        JSONObject jsonObject = statisticsService.energyFlowAnalysis(paramVO);
        return success(jsonObject);
    }


}