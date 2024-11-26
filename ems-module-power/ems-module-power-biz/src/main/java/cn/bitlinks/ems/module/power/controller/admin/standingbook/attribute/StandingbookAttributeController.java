package cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账属性")
@RestController
@RequestMapping("/power/standingbook-attribute")
@Validated
public class StandingbookAttributeController {

    @Resource
    private StandingbookAttributeService standingbookAttributeService;

    @PostMapping("/create")
    @Operation(summary = "创建台账属性")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:create')")
    public CommonResult<Long> createStandingbookAttribute(@Valid @RequestBody StandingbookAttributeSaveReqVO createReqVO) {
        return success(standingbookAttributeService.createStandingbookAttribute(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账属性")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:update')")
    public CommonResult<Boolean> updateStandingbookAttribute(@Valid @RequestBody StandingbookAttributeSaveReqVO updateReqVO) {
        standingbookAttributeService.updateStandingbookAttribute(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除台账属性")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:delete')")
    public CommonResult<Boolean> deleteStandingbookAttribute(@RequestParam("id") Long id) {
        standingbookAttributeService.deleteStandingbookAttribute(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得台账属性")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:query')")
    public CommonResult<StandingbookAttributeRespVO> getStandingbookAttribute(@RequestParam("id") Long id) {
        StandingbookAttributeDO standingbookAttribute = standingbookAttributeService.getStandingbookAttribute(id);
        return success(BeanUtils.toBean(standingbookAttribute, StandingbookAttributeRespVO.class));
    }
  @GetMapping("/getByTypeId")
    @Operation(summary = "获得台账属性")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:query')")
    public CommonResult<List<StandingbookAttributeRespVO>> getByTypeId(@RequestParam("typeId") Long typeId) {
        List<StandingbookAttributeDO> standingbookAttributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);
        return success(BeanUtils.toBean(standingbookAttributes, StandingbookAttributeRespVO.class));
    }

    @PostMapping("/page")
    @Operation(summary = "获得台账属性分页")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:query')")
    public CommonResult<PageResult<StandingbookAttributeRespVO>> getStandingbookAttributePage(@Valid @RequestBody StandingbookAttributePageReqVO pageReqVO) {
        PageResult<StandingbookAttributeDO> pageResult = standingbookAttributeService.getStandingbookAttributePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, StandingbookAttributeRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出台账属性 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook-attribute:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandingbookAttributeExcel(@Valid @RequestBody StandingbookAttributePageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<StandingbookAttributeDO> list = standingbookAttributeService.getStandingbookAttributePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "台账属性.xls", "数据", StandingbookAttributeRespVO.class,
                        BeanUtils.toBean(list, StandingbookAttributeRespVO.class));
    }

}
