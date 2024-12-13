package cn.bitlinks.ems.module.power.controller.admin.labelconfig;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.framework.idempotent.core.annotation.Idempotent;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigRespVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.hutool.core.lang.tree.Tree;
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

@Tag(name = "管理后台 - 配置标签")
@RestController
@RequestMapping("/power/label-config")
@Validated
public class LabelConfigController {

    @Resource
    private LabelConfigService labelConfigService;

    @Idempotent(timeout = 5)
    @PostMapping("/create")
    @Operation(summary = "创建配置标签")
    @PreAuthorize("@ss.hasPermission('power:label-config:create')")
    public CommonResult<Long> createLabelConfig(@Valid @RequestBody LabelConfigSaveReqVO createReqVO) {
        return success(labelConfigService.createLabelConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新配置标签")
    @PreAuthorize("@ss.hasPermission('power:label-config:update')")
    public CommonResult<Boolean> updateLabelConfig(@Valid @RequestBody LabelConfigSaveReqVO updateReqVO) {
        labelConfigService.updateLabelConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除配置标签")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:label-config:delete')")
    public CommonResult<Boolean> deleteLabelConfig(@RequestParam("id") Long id) {
        labelConfigService.deleteLabelConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得配置标签")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:label-config:query')")
    public CommonResult<LabelConfigRespVO> getLabelConfig(@RequestParam("id") Long id,
                                                          @RequestParam(value = "database", required = false) Integer database) {

        // TODO: 2024/11/1 多数据源测试后续可删
        LabelConfigDO labelConfig;
        if (database != null && database == 3307){
             labelConfig = labelConfigService.getLabelConfig07(id);
        }else {
             labelConfig = labelConfigService.getLabelConfig(id);
        }


        return success(BeanUtils.toBean(labelConfig, LabelConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得配置标签分页")
    @PreAuthorize("@ss.hasPermission('power:label-config:query')")
    public CommonResult<PageResult<LabelConfigRespVO>> getLabelConfigPage(@Valid LabelConfigPageReqVO pageReqVO) {
        PageResult<LabelConfigDO> pageResult = labelConfigService.getLabelConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, LabelConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出配置标签 Excel")
    @PreAuthorize("@ss.hasPermission('power:label-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLabelConfigExcel(@Valid LabelConfigPageReqVO pageReqVO,
                                       HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<LabelConfigDO> list = labelConfigService.getLabelConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "配置标签.xls", "数据", LabelConfigRespVO.class,
                BeanUtils.toBean(list, LabelConfigRespVO.class));
    }


    /**
     * 获取标签tree
     *
     * @return labelTree
     */
    @GetMapping("/tree")
    @Operation(summary = "获取标签tree")
    @PreAuthorize("@ss.hasPermission('power:label-config:query')")
    public CommonResult<List<Tree<Long>>> getLabelTree(@RequestParam("lazy") boolean lazy,
                                                       @RequestParam(value = "parentId", required = false) Long parentId,
                                                       @RequestParam(value = "labelName", required = false) String labelName) {
        List<Tree<Long>> labelTree = labelConfigService.getLabelTree(lazy, parentId, labelName);
        return success(labelTree);
    }


}