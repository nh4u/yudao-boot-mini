package cn.bitlinks.ems.module.power.controller.admin.energygroup;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.energygroup.vo.EnergyGroupPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energygroup.vo.EnergyGroupRespVO;
import cn.bitlinks.ems.module.power.controller.admin.energygroup.vo.EnergyGroupSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energygroup.EnergyGroupDO;
import cn.bitlinks.ems.module.power.service.energygroup.EnergyGroupService;
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

@Tag(name = "管理后台 - 能源分组")
@RestController
@RequestMapping("/power/energyGroup")
@Validated
public class EnergyGroupController {

    @Resource
    private EnergyGroupService energyGroupService;

    @PostMapping("/change")
    @Operation(summary = "创建能源分组")
    @PreAuthorize("@ss.hasPermission('power:energy-group:create')")
    public CommonResult<Boolean> change(@Valid @RequestBody List<EnergyGroupSaveReqVO> energyGroups) {
        return success(energyGroupService.change(energyGroups));
    }

    @GetMapping("/getEnergyGroups")
    @Operation(summary = "获取能源分组list")
    @PreAuthorize("@ss.hasPermission('power:energy-group:create')")
    public CommonResult<List<EnergyGroupRespVO>> getEnergyGroups() {
        List<EnergyGroupDO> energyGroups = energyGroupService.getEnergyGroups();
        return success(BeanUtils.toBean(energyGroups, EnergyGroupRespVO.class));
    }


    @GetMapping("/export-excel")
    @Operation(summary = "导出能源分组 Excel")
    @PreAuthorize("@ss.hasPermission('power:energy-group:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEnergyGroupExcel(@Valid EnergyGroupPageReqVO pageReqVO,
                                       HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<EnergyGroupDO> list = energyGroupService.getEnergyGroupPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "能源分组.xls", "数据", EnergyGroupRespVO.class,
                BeanUtils.toBean(list, EnergyGroupRespVO.class));
    }

}