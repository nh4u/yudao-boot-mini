package cn.bitlinks.ems.module.power.controller.admin.minitor;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorDetailRespVO;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorRespVO;
import cn.bitlinks.ems.module.power.service.minitor.MinitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 设备监控")
@RestController
@RequestMapping("/power/minitor")
@Validated
public class MinitorController {
    @Resource
    private MinitorService minitorService;

    @PostMapping("/minitorList")
    @Operation(summary = "获得监控列表")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<MinitorRespVO> getMinitorList(@Valid @RequestBody Map<String, String> pageReqVO) {
        MinitorRespVO minitorRespVO = minitorService.getMinitorList(pageReqVO);
        return success(minitorRespVO);
    }


    @PostMapping("/deviceDetail")
    @Operation(summary = "监控详情")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<MinitorDetailRespVO> deviceDetail(@Valid @RequestBody MinitorParamReqVO paramVO) {
        MinitorDetailRespVO minitorDetailRespVO = minitorService.deviceDetail(paramVO);
        return success(minitorDetailRespVO);
    }




}
