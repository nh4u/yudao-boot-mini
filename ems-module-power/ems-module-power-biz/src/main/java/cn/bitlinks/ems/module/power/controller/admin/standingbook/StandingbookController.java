package cn.bitlinks.ems.module.power.controller.admin.standingbook;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.enums.ErrorCodeConstants;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账")
@RestController
@RequestMapping("/power/standingbook")
@Validated
public class StandingbookController {
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;

    @PostMapping("/create")
    @Operation(summary = "创建台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:create')")
    public CommonResult<Long> createStandingbook(@Valid  @RequestBody StandingbookSaveReqVO createReqVO) {
        return success(standingbookService.createStandingbook(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<Boolean> updateStandingbook(@Valid @RequestBody StandingbookSaveReqVO updateReqVO) {
        standingbookService.updateStandingbook(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除台账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:standingbook:delete')")
    public CommonResult<Boolean> deleteStandingbook(@RequestParam("id") Long id) {
        standingbookService.deleteStandingbook(id);
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
    public CommonResult<List<StandingbookRespVO>> getStandingbookPage(@Valid @RequestBody StandingbookPageReqVO pageReqVO) {
        List<StandingbookDO> list = standingbookService.getStandingbookList(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookRespVO.class));
    }
    @PostMapping(value = "importStandingbook")
    @Operation(summary = "导入台账 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    @ApiAccessLog(operateType = IMPORT)
    public  CommonResult importStandingbook(@RequestParam("file")MultipartFile file, @RequestBody StandingbookRespVO pageReqVO) {
        if(pageReqVO.getTypeId() == null){
            return CommonResult.error(ErrorCodeConstants.STANDINGBOOK_TYPE_NOT_EXISTS.getCode(),"台账类型不能为空");
        }
        return success(standingbookService.importStandingbook(file,pageReqVO));
    }
    @PostMapping("/export-excel")
    @Operation(summary = "导出台账 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandingbookExcel(@Valid @RequestBody StandingbookPageReqVO pageReqVO,
              HttpServletResponse response) {
        if (pageReqVO == null && pageReqVO.getTypeId() == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        standingbookService.exportStandingbookExcel(pageReqVO, response);
    }
     @GetMapping("/export-excel-template")
    @Operation(summary = "下载台账导入模板 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
     @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @ApiAccessLog(operateType = EXPORT)
    public void template(@RequestParam("typeId") Long typeId, HttpServletResponse response)  {
        if (typeId == null) {
            throw new IllegalArgumentException("台账编号不能为空");
        }
         standingbookService.template(typeId, response);

    }

}
