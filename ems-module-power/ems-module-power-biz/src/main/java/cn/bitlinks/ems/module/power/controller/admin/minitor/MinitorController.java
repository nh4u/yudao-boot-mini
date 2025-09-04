package cn.bitlinks.ems.module.power.controller.admin.minitor;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorDetailData;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorDetailRespVO;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.minitor.vo.MinitorRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.service.minitor.MinitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
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
    //@PreAuthorize("@ss.hasPermission('power:minitor:query')")
    public CommonResult<MinitorRespVO> getMinitorList(@Valid @RequestBody Map<String, String> pageReqVO) {
        MinitorRespVO minitorRespVO = minitorService.getMinitorList(pageReqVO);
        return success(minitorRespVO);
    }

    @PostMapping("/deviceDetail")
    @Operation(summary = "监控详情")
    //@PreAuthorize("@ss.hasPermission('power:minitor:query')")
    public CommonResult<MinitorDetailRespVO> deviceDetail(@Valid @RequestBody MinitorParamReqVO paramVO) {
        MinitorDetailRespVO minitorDetailRespVO = minitorService.deviceDetail(paramVO);
        return success(minitorDetailRespVO);
    }


    @GetMapping("/getDaqAttrs")
    @Operation(summary = "获取台账所有数采参数（能源+自定义）查询启用的")
    //@PreAuthorize("@ss.hasPermission('power:minitor:query')")
    public CommonResult<List<StandingbookTmplDaqAttrRespVO>> getDaqAttrs(@RequestParam("standingbookId") Long standingbookId) {
        return success(minitorService.getDaqAttrs(standingbookId));

    }

    @PostMapping("/exportDetailTable")
    @Operation(summary = "导出详情表数据")
    //@PreAuthorize("@ss.hasPermission('power:minitor:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDetailTable(@Valid @RequestBody MinitorParamReqVO paramVO,
                                   HttpServletResponse response) throws IOException {
        List<MinitorDetailData> list = minitorService.getDetailTable(paramVO);
        // 导出 Excel
        ExcelUtils.write(response, "设备监控详情数据表.xlsx", "数据", MinitorDetailData.class,list);
    }
}
