package cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.acquisition.vo.StandingbookAcquisitionRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
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

//    @PostMapping("/create")
//    @Operation(summary = "创建台账-数采设置")
//    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:create')")
//    public CommonResult<Long> createStandingbookAcquisition(@Valid @RequestBody StandingbookAcquisitionSaveReqVO createReqVO) {
//        return success(standingbookAcquisitionService.createStandingbookAcquisition(createReqVO));
//    }
//
//    @PutMapping("/update")
//    @Operation(summary = "更新台账-数采设置")
//    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:update')")
//    public CommonResult<Boolean> updateStandingbookAcquisition(@Valid @RequestBody StandingbookAcquisitionSaveReqVO updateReqVO) {
//        standingbookAcquisitionService.updateStandingbookAcquisition(updateReqVO);
//        return success(true);
//    }
//
//    @DeleteMapping("/delete")
//    @Operation(summary = "删除台账-数采设置")
//    @Parameter(name = "id", description = "编号", required = true)
//    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:delete')")
//    public CommonResult<Boolean> deleteStandingbookAcquisition(@RequestParam("id") Long id) {
//        standingbookAcquisitionService.deleteStandingbookAcquisition(id);
//        return success(true);
//    }
//
//    @GetMapping("/get")
//    @Operation(summary = "获得台账-数采设置")
//    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:query')")
//    public CommonResult<StandingbookAcquisitionRespVO> getStandingbookAcquisition(@RequestParam("id") Long id) {
//        StandingbookAcquisitionDO standingbookAcquisition = standingbookAcquisitionService.getStandingbookAcquisition(id);
//        return success(BeanUtils.toBean(standingbookAcquisition, StandingbookAcquisitionRespVO.class));
//    }

//    @GetMapping("/page")
//    @Operation(summary = "获得台账-数采设置分页")
//    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:query')")
//    public CommonResult<PageResult<StandingbookAcquisitionRespVO>> getStandingbookAcquisitionPage(@Valid StandingbookAcquisitionPageReqVO pageReqVO) {
//        PageResult<StandingbookAcquisitionDO> pageResult = standingbookAcquisitionService.getStandingbookAcquisitionPage(pageReqVO);
//        return success(BeanUtils.toBean(pageResult, StandingbookAcquisitionRespVO.class));
//    }
    @PostMapping("/list")
    @Operation(summary = "获得台账-蔬菜设置列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook-acquisition:query')")
    public CommonResult<List<StandingbookAcquisitionRespVO>> getStandingbookAcquisitionList(@Valid @RequestBody Map<String,
            String> queryReqVO) {
        return success(standingbookAcquisitionService.getStandingbookAcquisitionList(queryReqVO));
    }


}