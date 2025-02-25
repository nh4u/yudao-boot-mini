package cn.bitlinks.ems.module.power.controller.admin.additionalrecording;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;

import javax.validation.constraints.*;
import javax.validation.*;
import javax.servlet.http.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.*;
import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.module.power.service.additionalrecording.AdditionalRecordingService;

@Tag(name = "管理后台 - 补录")
@RestController
@RequestMapping("/power/additional-recording")
@Validated
public class AdditionalRecordingController {

    @Resource
    private AdditionalRecordingService additionalRecordingService;

    @PostMapping("/create")
    @Operation(summary = "手动补录")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:create')")
    public CommonResult<Long> createAdditionalRecording(@Valid @RequestBody AdditionalRecordingSaveReqVO createReqVO) {
        return success(additionalRecordingService.createAdditionalRecording(createReqVO));
    }

    @PostMapping("/createByVoucherId")
    @Operation(summary = "凭证导入")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:createByVoucherId')")
    public CommonResult<List<Long>> createAdditionalRecording(@RequestBody CreateAdditionalRecordingDTO request) {
        Long standingbookId = request.getStandingbookId();
        List<Long> voucherIds = request.getVoucherIds();
        return success(additionalRecordingService.createAdditionalRecordingByVoucherId(voucherIds,standingbookId));
    }

    @GetMapping("/getVoucherIdBystandingbookId")
    @Operation(summary = "回显凭证id")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:getVoucherIdBystandingbookId')")
    public CommonResult<List<Long>> getVoucherIdsByStandingbookId(@RequestParam("standingbookId")Long standingbookId) {
        return success(additionalRecordingService.getVoucherIdsByStandingbookId(standingbookId));
    }

    @GetMapping("/last-record")
    @Operation(summary = "获取上次记录")
    public CommonResult<AdditionalRecordingLastVO> getLastRecord(
            @RequestParam("standingbookId") Long standingbookId,
            @RequestParam("currentCollectTime")
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime currentCollectTime) {
        return CommonResult.success(additionalRecordingService.getLastRecord(standingbookId, currentCollectTime));
    }

    @PutMapping("/update")
    @Operation(summary = "更新补录")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:update')")
    public CommonResult<Boolean> updateAdditionalRecording(@Valid @RequestBody AdditionalRecordingSaveReqVO updateReqVO) {
        additionalRecordingService.updateAdditionalRecording(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/deleteIds")
    @Operation(summary = "删除补录")
    @Parameter(name = "ids", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:additional-recording:delete')")
    public CommonResult<Boolean> deleteAdditionalRecordings(@RequestBody List<Long> ids) {
        additionalRecordingService.deleteAdditionalRecordings(ids);;
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得补录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:query')")
    public CommonResult<AdditionalRecordingRespVO> getAdditionalRecording(@RequestParam("id") Long id) {
        AdditionalRecordingDO additionalRecording = additionalRecordingService.getAdditionalRecording(id);
        return success(BeanUtils.toBean(additionalRecording, AdditionalRecordingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得补录分页")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:query')")
    public CommonResult<PageResult<AdditionalRecordingRespVO>> getAdditionalRecordingPage(@Valid AdditionalRecordingPageReqVO pageReqVO) {
        PageResult<AdditionalRecordingDO> pageResult = additionalRecordingService.getAdditionalRecordingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AdditionalRecordingRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出补录 Excel")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAdditionalRecordingExcel(@Valid AdditionalRecordingPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AdditionalRecordingDO> list = additionalRecordingService.getAdditionalRecordingPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "补录.xls", "数据", AdditionalRecordingRespVO.class,
                        BeanUtils.toBean(list, AdditionalRecordingRespVO.class));
    }

    @GetMapping("/query")
    @Operation(summary = "查询补录数据")
    @PreAuthorize("@ss.hasPermission('power:additional-recording:query')")
    public CommonResult<List<AdditionalRecordingDO>> getAdditionalRecordingPage(
            BigDecimal minThisValue, BigDecimal maxThisValue,
            String recordPerson,
            Integer recordMethod,
            LocalDateTime startThisCollectTime, LocalDateTime endThisCollectTime,
            LocalDateTime startEnterTime, LocalDateTime endEnterTime) {
        return success(additionalRecordingService.selectByCondition(minThisValue,maxThisValue,recordPerson,recordMethod,startThisCollectTime,endThisCollectTime,startEnterTime,endEnterTime));
    }

}