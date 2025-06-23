package cn.bitlinks.ems.module.power.controller.admin.report;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StandardCoalInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/06/21 19:23
 **/
public class CopController {


    @Resource
    private CopHourAggDataService copHourAggDataService;


    @PostMapping("/copTable")
    @Operation(summary = "COP报表")
    public CommonResult<StatisticsResultV2VO<StandardCoalInfo>> copTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(copHourAggDataService.copTable(paramVO));
    }


}
