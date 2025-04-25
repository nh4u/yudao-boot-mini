package cn.bitlinks.ems.module.power.controller.admin.standingbook;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookAssociationReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.enums.ErrorCodeConstants;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账")
@RestController
@RequestMapping("/power/standingbook")
@Validated
public class StandingbookController {
    @Resource
    private StandingbookService standingbookService;


    @PostMapping("/create")
    @Operation(summary = "创建台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:create')")
    public CommonResult<Long> createStandingbook(@Valid @RequestBody Map<String, String> createReqVO) {
        return success(standingbookService.createStandingbook(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<Boolean> updateStandingbook(@Valid @RequestBody Map<String, String> updateReqVO) {
        standingbookService.updateStandingbook(updateReqVO);
        return success(true);
    }


    @DeleteMapping("/delete")
    @Operation(summary = "删除台账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:standingbook:delete')")
    public CommonResult<Boolean> deleteStandingbook(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return CommonResult.error(ErrorCodeConstants.STANDINGBOOK_NOT_EXISTS.getCode(), "台账编号不能为空");
        }
        for (Long aLong : ids) {
            standingbookService.deleteStandingbook(aLong);
        }
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得台账")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<StandingbookRespVO> getStandingbook(@RequestParam("id") Long id) {
        StandingbookDO standingbook = standingbookService.getStandingbook(id);
        return success(BeanUtils.toBean(standingbook, StandingbookRespVO.class));
    }

    @PostMapping("/list")
    @Operation(summary = "获得台账列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> getStandingbookPage(@Valid @RequestBody Map<String, String> pageReqVO) {
        List<StandingbookDO> list = standingbookService.getStandingbookList(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookRespVO.class));
    }

    @PostMapping("/listSbAllWithAssociations")
    @Operation(summary = "关联计量器具：关联下级计量器具/关联设备接口（topType=2）或者重点设备（topType=1）")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> listSbAllWithAssociations(@RequestBody StandingbookAssociationReqVO reqVO) {
        return success(standingbookService.listSbAllWithAssociations(reqVO));
    }

    @PostMapping("/listWithAssociations")
    @Operation(summary = "关联计量器具：根据条件获得台账列表和计量器具联系")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookWithAssociations>> getStandingbookListWithAssociations(@RequestBody Map<String, String> pageReqVO) {
        List<StandingbookWithAssociations> list = standingbookService.getStandingbookListWithAssociations(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookWithAssociations.class));
    }


}
