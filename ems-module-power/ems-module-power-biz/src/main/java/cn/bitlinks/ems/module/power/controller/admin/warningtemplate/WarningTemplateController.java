package cn.bitlinks.ems.module.power.controller.admin.warningtemplate;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplatePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplateRespVO;
import cn.bitlinks.ems.module.power.controller.admin.warningtemplate.vo.WarningTemplateSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warningtemplate.WarningTemplateDO;
import cn.bitlinks.ems.module.power.service.warningtemplate.WarningTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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

@Tag(name = "管理后台 - 告警模板")
@RestController
@RequestMapping("/power/warning-template")
@Validated
public class WarningTemplateController {

    @Resource
    private WarningTemplateService warningTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建告警模板")
    @PreAuthorize("@ss.hasPermission('power:warning-template:create')")
    public CommonResult<Long> createWarningTemplate(@Valid @RequestBody WarningTemplateSaveReqVO createReqVO) {
        return success(warningTemplateService.createWarningTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新告警模板")
    @PreAuthorize("@ss.hasPermission('power:warning-template:update')")
    public CommonResult<Boolean> updateWarningTemplate(@Valid @RequestBody WarningTemplateSaveReqVO updateReqVO) {
        warningTemplateService.updateWarningTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/deleteBatch")
    @Operation(summary = "删除告警模板(批量)")
    @PreAuthorize("@ss.hasPermission('power:warning-template:delete')")
    public CommonResult<Boolean> deleteWarningTemplateBatch( @RequestBody List<Long> ids) {
        warningTemplateService.deleteWarningTemplateBatch(ids);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除告警模板")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:warning-template:delete')")
    public CommonResult<Boolean> deleteWarningTemplate(@RequestParam("id") Long id) {
        warningTemplateService.deleteWarningTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得告警模板")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:warning-template:query')")
    public CommonResult<WarningTemplateRespVO> getWarningTemplate(@RequestParam("id") Long id) {
        WarningTemplateDO warningTemplate = warningTemplateService.getWarningTemplate(id);
        return success(BeanUtils.toBean(warningTemplate, WarningTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得告警模板分页")
    @PreAuthorize("@ss.hasPermission('power:warning-template:query')")
    public CommonResult<PageResult<WarningTemplateRespVO>> getWarningTemplatePage(@Valid WarningTemplatePageReqVO pageReqVO) {
        PageResult<WarningTemplateDO> pageResult = warningTemplateService.getWarningTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, WarningTemplateRespVO.class));
    }

    @GetMapping("/listByType")
    @Operation(summary = "站内信/邮件模板列表，模板名称 模糊搜索，")
    @PreAuthorize("@ss.hasPermission('power:warning-template:query')")
    @Parameters({
            @Parameter(name = "type", description = "type：0-站内信 1-邮件", required = true, example = "0"),
            @Parameter(name = "name", description = "模板名称", required = false, example = "d")
    })
    public CommonResult<List<WarningTemplateRespVO>> getWarningTemplateList(@RequestParam Integer type,
                                                                            @RequestParam(value = "name", required = false) String name) {
        List<WarningTemplateDO> result = warningTemplateService.getWarningTemplateList(type, name);
        return success(BeanUtils.toBean(result, WarningTemplateRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出告警模板 Excel")
    @PreAuthorize("@ss.hasPermission('power:warning-template:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarningTemplateExcel(@Valid WarningTemplatePageReqVO pageReqVO,
                                           HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<WarningTemplateDO> list = warningTemplateService.getWarningTemplatePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "告警模板.xls", "数据", WarningTemplateRespVO.class,
                BeanUtils.toBean(list, WarningTemplateRespVO.class));
    }

}