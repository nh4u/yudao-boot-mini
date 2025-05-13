package cn.bitlinks.ems.module.power.controller.admin.daparamformula;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaRespVO;
import cn.bitlinks.ems.module.power.controller.admin.daparamformula.vo.DaParamFormulaSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.service.daparamformula.DaParamFormulaService;
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

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 参数公式")
@RestController
@RequestMapping("/power/paramFormula")
@Validated
public class DaParamFormulaController {

    @Resource
    private DaParamFormulaService daParamFormulaService;

    @PostMapping("/create")
    @Operation(summary = "创建参数公式")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:create')")
    public CommonResult<Long> createDaParamFormula(@Valid @RequestBody DaParamFormulaSaveReqVO createReqVO) {
        return success(daParamFormulaService.createDaParamFormula(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新参数公式")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:update')")
    public CommonResult<Boolean> updateDaParamFormula(@Valid @RequestBody DaParamFormulaSaveReqVO updateReqVO) {
        daParamFormulaService.updateDaParamFormula(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除参数公式")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:delete')")
    public CommonResult<Boolean> deleteDaParamFormula(@RequestParam("id") Long id) {
        daParamFormulaService.deleteDaParamFormula(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得参数公式")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:query')")
    public CommonResult<DaParamFormulaRespVO> getDaParamFormula(@RequestParam("id") Long id) {
        DaParamFormulaDO daParamFormula = daParamFormulaService.getDaParamFormula(id);
        return success(BeanUtils.toBean(daParamFormula, DaParamFormulaRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得参数公式分页")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:query')")
    public CommonResult<PageResult<DaParamFormulaRespVO>> getDaParamFormulaPage(@Valid DaParamFormulaPageReqVO pageReqVO) {
        PageResult<DaParamFormulaDO> pageResult = daParamFormulaService.getDaParamFormulaPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, DaParamFormulaRespVO.class));
    }

    @PostMapping("/change")
    @Operation(summary = "公式新增、修改、删除")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:create')")
    public CommonResult<Boolean> change(@Valid @RequestBody List<DaParamFormulaSaveReqVO> formulas) {
        return success(daParamFormulaService.change(formulas));
    }

    @GetMapping("/getFormulaList")
    @Operation(summary = "获取公式list")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:query')")
    public CommonResult<List<DaParamFormulaRespVO>> getDaParamFormulaList(DaParamFormulaSaveReqVO reqVO) {
        List<DaParamFormulaDO> list = daParamFormulaService.getDaParamFormulaList(reqVO);
        return success(BeanUtils.toBean(list, DaParamFormulaRespVO.class));
    }

    @GetMapping("/isDuplicate")
    @Operation(summary = "公式是否重复true:重复；false：不重复")
    @Parameter(name = "id", description = "公式id", example = "1024")
    @Parameter(name = "energyId", description = "能源id", required = true, example = "1024")
    @Parameter(name = "formulaType", description = "公式类型 1折标煤公式，2用能成本公式", required = true, example = "1")
    @Parameter(name = "energyFormula", description = "公式", required = true, example = "用量X折标煤系数X0.9")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:query')")
    public CommonResult<Boolean> isDuplicate(@RequestParam(value = "id", required = false) Long id,
                                             @RequestParam(value = "energyId") Long energyId,
                                             @RequestParam(value = "formulaType") Integer formulaType,
                                             @RequestParam(value = "energyFormula") String energyFormula) {
        return success(daParamFormulaService.isDuplicate(id, energyId, formulaType, energyFormula));
    }

    @GetMapping("/isDelete")
    @Operation(summary = "公式是否可删除")
    @Parameter(name = "id", description = "公式id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:query')")
    public CommonResult<Boolean> isDelete(@RequestParam(value = "id") Long id) {
        return success(daParamFormulaService.isDelete(id));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出参数公式 Excel")
    @PreAuthorize("@ss.hasPermission('power:da-param-formula:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDaParamFormulaExcel(@Valid DaParamFormulaPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<DaParamFormulaDO> list = daParamFormulaService.getDaParamFormulaPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "参数公式.xls", "数据", DaParamFormulaRespVO.class,
                BeanUtils.toBean(list, DaParamFormulaRespVO.class));
    }

}