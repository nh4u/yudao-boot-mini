package cn.bitlinks.ems.module.power.controller.admin.bigscreen;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.BigScreenCopChartData;
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

import java.util.List;

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


    @PostMapping("/getBanner")
    @Operation(summary = "获得banner数据")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<BannerResultVO> getBannerData(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        BannerResultVO bannerResultVO = bigScreenService.getBannerData(paramVO);
        return success(bannerResultVO);
    }

    @PostMapping("/getRecentSevenDay")
    @Operation(summary = "获得近7日能源数据")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<List<RecentSevenDayResultVO>> getRecentSevenDay(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        List<RecentSevenDayResultVO> resultVOList = bigScreenService.getRecentSevenDay(paramVO);
        return success(resultVOList);
    }

    @PostMapping("/getRecentFifteenDayProduction")
    @Operation(summary = "获得近15日产品数据")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<ProductionFifteenDayResultVO> getRecentFifteenDayProduction(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        ProductionFifteenDayResultVO resultVO = bigScreenService.getRecentFifteenDayProduction(paramVO);
        return success(resultVO);
    }

    @PostMapping("/getOutsideEnv")
    @Operation(summary = "获取室外工况")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<OutsideEnvData> getOutsideEnvData(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        OutsideEnvData resultVO = bigScreenService.getOutsideEnvData(paramVO);
        return success(resultVO);
    }

    @PostMapping("/getCopChart")
    @Operation(summary = "获取COP")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<BigScreenCopChartData> getCopChartData(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        BigScreenCopChartData resultVO = bigScreenService.getCopChartData(paramVO);
        return success(resultVO);
    }

    @PostMapping("/getPureWasteWaterChart")
    @Operation(summary = "获取纯废水单价")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<BigScreenChartData> getPureWasteWaterChart(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        BigScreenChartData resultVO = bigScreenService.getPureWasteWaterChart(paramVO);
        return success(resultVO);
    }

    @PostMapping("/getCompressedGasChart")
    @Operation(summary = "获取压缩空气单价")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<BigScreenChartData> getCompressedGasChart(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        BigScreenChartData resultVO = bigScreenService.getCompressedGasChart(paramVO);
        return success(resultVO);
    }

    @PostMapping("/getMiddleData")
    @Operation(summary = "获取中间数据")
    //@PreAuthorize("@ss.hasPermission('power:bigScreen:query')")
    public CommonResult<MiddleData> getMiddleData(@Valid @RequestBody BigScreenParamReqVO paramVO) {
        MiddleData resultVO = bigScreenService.getMiddleData(paramVO);
        return success(resultVO);
    }

}
