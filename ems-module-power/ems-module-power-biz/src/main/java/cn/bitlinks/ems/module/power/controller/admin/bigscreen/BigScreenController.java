package cn.bitlinks.ems.module.power.controller.admin.bigscreen;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenRespVO;
import cn.bitlinks.ems.module.power.service.bigscreen.BigScreenService;
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
@Tag(name = "管理后台 - 大屏")
@RestController
@RequestMapping("/power/bigScreen")
@Validated
public class BigScreenController {
    @Resource
    private BigScreenService bigScreenService;

    @PostMapping("/details")
    @Operation(summary = "获得大屏数据")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<BigScreenRespVO> getBigScreenDetails(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        BigScreenRespVO minitorRespVO = bigScreenService.getBigScreenDetails(paramVO);
        return success(minitorRespVO);
    }

}
