package cn.bitlinks.ems.module.power.controller.admin.doublecarbon;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.*;
import cn.bitlinks.ems.module.power.service.doublecarbon.DoubleCarbonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 双碳接口")
@RestController
@RequestMapping("/power/double-carbon")
@Validated
public class DoubleCarbonController {
    @Resource
    private DoubleCarbonService doubleCarbonService;

    @PostMapping("/settings")
    @Operation(summary = "查询系统设置")
    public CommonResult<DoubleCarbonSettingsRespVO> getSettings() {
        return success(doubleCarbonService.getSettings());
    }

    @GetMapping("/mapping")
    @Operation(summary = "查询映射关系 分页")
    public CommonResult<PageResult<DoubleCarbonMappingRespVO>> getMappingPage(@Valid DoubleCarbonMappingPageReqVO pageReqVO) {
        PageResult<DoubleCarbonMappingRespVO> page = doubleCarbonService.getMappingPage(pageReqVO);

        return success(page);
    }

    @PostMapping("/updSettings")
    @Operation(summary = "更新设置")
    public CommonResult<Boolean> updSettings(@RequestBody DoubleCarbonSettingsUpdVO updVO) {
        doubleCarbonService.updSettings(updVO);
        return success(true);
    }

    @PostMapping("/updMapping")
    @Operation(summary = "更新映射")
    public CommonResult<Boolean> updMapping(@RequestBody DoubleCarbonMappingUpdVO updVO) {
        doubleCarbonService.updMapping(updVO);
        return success(true);
    }


    @PostMapping("/import")
    @Operation(summary = "导入双系统编码对应关系")
    public CommonResult<DoubleCarbonMappingImportRespVO> importExcel(
            @RequestParam("file")
            @Parameter(name = "file", description = "Excel 文件", required = true)
            MultipartFile file) {
        return success(doubleCarbonService.importExcel(file));
    }

}
