package cn.bitlinks.ems.module.power.controller.admin.standingbook.type;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
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

@Tag(name = "管理后台 - 台账类型")
@RestController
@RequestMapping("/power/standingbook-type")
@Validated
public class StandingbookTypeController {

    @Resource
    private StandingbookTypeService standingbookTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建台账类型")
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:create')")
    public CommonResult<Long> createStandingbookType(@Valid @RequestBody StandingbookTypeSaveReqVO createReqVO) {
        return success(standingbookTypeService.createStandingbookType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账类型")
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:update')")
    public CommonResult<StandingbookTypeRespVO> updateStandingbookType(@Valid StandingbookTypeSaveReqVO updateReqVO) {
        standingbookTypeService.updateStandingbookType(updateReqVO);
        return success(getStandingbookType(updateReqVO.getId()).getData());
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除台账类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:delete')")
    public CommonResult<Boolean> deleteStandingbookType(@RequestParam("id") Long id) {
        standingbookTypeService.deleteStandingbookType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得台账类型")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:query')")
    public CommonResult<StandingbookTypeRespVO> getStandingbookType(@RequestParam("id") Long id) {
        StandingbookTypeDO standingbookType = standingbookTypeService.getStandingbookType(id);
        return success(BeanUtils.toBean(standingbookType, StandingbookTypeRespVO.class));
    }
    @GetMapping("/getByName")
    @Operation(summary = "获得台账类型根据名称")
    @Parameter(name = "name", description = "名称", required = true, example = "锅炉")
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:query')")
    public CommonResult<List<StandingbookTypeRespVO>> getStandingbookTypeByName(@RequestParam("name") String name) {
        List<StandingbookTypeDO> standingbookType = standingbookTypeService.getStandingbookType(name);
        return success(BeanUtils.toBean(standingbookType, StandingbookTypeRespVO.class));
    }

    @PostMapping("/list")
    @Operation(summary = "获得台账类型列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:query')")
    public CommonResult<List<StandingbookTypeRespVO>> getStandingbookTypeList(@Valid @RequestBody StandingbookTypeListReqVO listReqVO) {
        List<StandingbookTypeDO> list = standingbookTypeService.getStandingbookTypeList(listReqVO);
        return success(BeanUtils.toBean(list, StandingbookTypeRespVO.class));
    }
    @GetMapping("/tree")
    @Operation(summary = "获得台账类型树形列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:query')")
    public CommonResult<List> getStandingbookTree() {
    List nodes = standingbookTypeService.getStandingbookTypeNode();
    return success(BeanUtils.toBean(nodes, StandingbookTypeRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出台账类型 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook-type:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandingbookTypeExcel(@Valid @RequestBody StandingbookTypeListReqVO listReqVO,
              HttpServletResponse response) throws IOException {
        List<StandingbookTypeDO> list = standingbookTypeService.getStandingbookTypeList(listReqVO);
        // 导出 Excel
        ExcelUtils.write(response, "台账类型.xls", "数据", StandingbookTypeRespVO.class,
                        BeanUtils.toBean(list, StandingbookTypeRespVO.class));
    }

}
