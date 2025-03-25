package cn.bitlinks.ems.module.power.controller.admin.warningstrategy;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.warningstrategy.vo.*;
import cn.bitlinks.ems.module.power.service.warningstrategy.WarningStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 告警策略")
@RestController
@RequestMapping("/power/warning-strategy")
@Validated
public class WarningStrategyController {

    @Resource
    private WarningStrategyService warningStrategyService;

    @PostMapping("/create")
    @Operation(summary = "创建告警策略")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:create')")
    public CommonResult<Long> createWarningStrategy(@Valid @RequestBody WarningStrategySaveReqVO createReqVO) {
        return success(warningStrategyService.createWarningStrategy(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新告警策略")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:update')")
    public CommonResult<Boolean> updateWarningStrategy(@Valid @RequestBody WarningStrategySaveReqVO updateReqVO) {
        warningStrategyService.updateWarningStrategy(updateReqVO);
        return success(true);
    }

    @PutMapping("/statusBatch")
    @Operation(summary = "告警策略(批量启停)")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:update')")
    public CommonResult<Boolean> updateWarningStrategyStatusBatch(@RequestBody WarningStrategyBatchUpdStatusReqVO updateReqVO) {
        warningStrategyService.updateWarningStrategyStatusBatch(updateReqVO);
        return success(true);
    }

    @PutMapping("/intervalBatch")
    @Operation(summary = "告警策略(批量告警间隔)")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:update')")
    public CommonResult<Boolean> updateWarningStrategyIntervalBatch(@RequestBody WarningStrategyBatchUpdIntervalReqVO updateReqVO) {
        warningStrategyService.updateWarningStrategyIntervalBatch(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/deleteBatch")
    @Operation(summary = "删除告警策略(批量)")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:delete')")
    public CommonResult<Boolean> deleteWarningStrategyBatch(@RequestBody List<Long> ids) {
        warningStrategyService.deleteWarningStrategyBatch(ids);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除告警策略")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:delete')")
    public CommonResult<Boolean> deleteWarningStrategy(@RequestParam("id") Long id) {
        warningStrategyService.deleteWarningStrategy(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得告警策略")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:query')")
    public CommonResult<WarningStrategyRespVO> getWarningStrategy(@RequestParam("id") Long id) {
        return success(warningStrategyService.getWarningStrategy(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得告警策略分页")
    @PreAuthorize("@ss.hasPermission('power:warning-strategy:query')")
    public CommonResult<PageResult<WarningStrategyPageRespVO>> getWarningStrategyPage(@Valid WarningStrategyPageReqVO pageReqVO) {
        PageResult<WarningStrategyPageRespVO> pageResult = warningStrategyService.getWarningStrategyPage(pageReqVO);
        return success(pageResult);
    }


}