package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionTestReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionVO;
import cn.bitlinks.ems.module.power.service.standingbook.acquisition.StandingbookAcquisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账-数采设置")
@RestController
@RequestMapping("/power/standingbook-acquisition")
@Validated
public class StandingbookAcquisitionController {

    @Resource
    private StandingbookAcquisitionService standingbookAcquisitionService;

    @PostMapping("/update")
    @Operation(summary = "创建台账-数采设置")
    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:update')")
    public CommonResult<Long> createOrUpdateStandingbookAcquisition(@Valid @RequestBody StandingbookAcquisitionVO updateReqVO) {
        return success(standingbookAcquisitionService.createOrUpdateStandingbookAcquisition(updateReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "根据台账id获得台账-数采设置")
    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:query')")
    public CommonResult<StandingbookAcquisitionVO> getAcquisitionByStandingbookId(@RequestParam(
            "standingbookId") Long standingbookId) {
        return success(standingbookAcquisitionService.getAcquisitionByStandingbookId(standingbookId));
    }

    @PostMapping("/test")
    @Operation(summary = "数采根据公式进行采集测试")
    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:query')")
    public CommonResult<String> testData(@RequestBody StandingbookAcquisitionTestReqVO testReqVO) {
        return success(standingbookAcquisitionService.testData(testReqVO));
    }



    @PostMapping("/list")
    @Operation(summary = "获得台账-数采设置列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:query')")
    public CommonResult<List<StandingbookAcquisitionRespVO>> getStandingbookAcquisitionList(@Valid @RequestBody Map<String,
            String> queryReqVO) {
        return success(standingbookAcquisitionService.getStandingbookAcquisitionList(queryReqVO));
    }


}