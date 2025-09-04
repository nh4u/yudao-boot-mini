package cn.bitlinks.ems.module.power.controller.admin.additionalrecording;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.additionalrecording.AdditionalRecordingDO;
import cn.bitlinks.ems.module.power.service.additionalrecording.AdditionalRecordingService;
import cn.bitlinks.ems.module.power.service.additionalrecording.ExcelMeterDataProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.error;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.IMPORT_EXCEL_ERROR;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.ADDITIONAL_RECORD;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;

@Tag(name = "管理后台 - 补录")
@RestController
@RequestMapping("/power/additional-recording")
@Validated
@Slf4j
public class AdditionalRecordingController {

    @Resource
    private AdditionalRecordingService additionalRecordingService;
    @Resource
    private ExcelMeterDataProcessor excelMeterDataProcessor;

    @PostMapping("/create")
    @Operation(summary = "手动补录")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:create')")
    public CommonResult<Boolean> createAdditionalRecording(@Valid @RequestBody AdditionalRecordingManualSaveReqVO createReqVO) {
        additionalRecordingService.createAdditionalRecording(createReqVO);
        return success(true);
    }

    @PostMapping("/createByVoucherId")
    @Operation(summary = "凭证导入")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:createByVoucherId')")
    public CommonResult<List<Long>> createAdditionalRecording(@RequestBody CreateAdditionalRecordingDTO request) {
        Long standingbookId = request.getStandingbookId();
        List<Long> voucherIds = request.getVoucherIds();
        return success(additionalRecordingService.createAdditionalRecordingByVoucherId(voucherIds, standingbookId));
    }

    @GetMapping("/getVoucherIdBystandingbookId")
    @Operation(summary = "回显凭证id")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:getVoucherIdBystandingbookId')")
    public CommonResult<List<Long>> getVoucherIdsByStandingbookId(@RequestParam("standingbookId") Long standingbookId) {
        return success(additionalRecordingService.getVoucherIdsByStandingbookId(standingbookId));
    }

    @GetMapping("/getExistDataRange")
    @Operation(summary = "全量-选择补录时间后查询原有数据")
    public CommonResult<AdditionalRecordingExistAcqDataRespVO> getExistDataRange(
            @RequestParam("standingbookId") Long standingbookId,
            @RequestParam("currentCollectTime")
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
            LocalDateTime currentCollectTime) {
        return CommonResult.success(additionalRecordingService.getExistDataRange(standingbookId, currentCollectTime));
    }

    @PutMapping("/update")
    @Operation(summary = "更新补录")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:update')")
    public CommonResult<Boolean> updateAdditionalRecording(@Valid @RequestBody AdditionalRecordingSaveReqVO updateReqVO) {
        additionalRecordingService.updateAdditionalRecording(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/deleteIds")
    @Operation(summary = "批量删除补录")
    @Parameter(name = "ids", description = "编号", required = true)
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:delete')")
    public CommonResult<Boolean> deleteAdditionalRecordings(@RequestBody List<Long> ids) {
        additionalRecordingService.deleteAdditionalRecordings(ids);
        ;
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除补录")
    @Parameter(name = "id", description = "编号", required = true)
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:delete')")
    public CommonResult<Boolean> deleteAdditionalRecording(@RequestParam("id") Long id) {
        additionalRecordingService.deleteAdditionalRecording(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得补录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:query')")
    public CommonResult<AdditionalRecordingRespVO> getAdditionalRecording(@RequestParam("id") Long id) {
        AdditionalRecordingDO additionalRecording = additionalRecordingService.getAdditionalRecording(id);
        return success(BeanUtils.toBean(additionalRecording, AdditionalRecordingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得补录分页")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:query')")
    public CommonResult<PageResult<AdditionalRecordingRespVO>> getAdditionalRecordingPage(@Valid AdditionalRecordingPageReqVO pageReqVO) {
        PageResult<AdditionalRecordingDO> pageResult = additionalRecordingService.getAdditionalRecordingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AdditionalRecordingRespVO.class));
    }

    @PostMapping("/exportAdditionalRecord")
    @Operation(summary = "导出数据补录 Excel")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAdditionalRecord(@Valid @RequestBody Map<String, String> pageReqVO,
                                       HttpServletResponse response) throws IOException {
        String filename = ADDITIONAL_RECORD + XLSX;

        List<AdditionalRecordingExportRespVO> list = additionalRecordingService.getAdditionalRecordingList(pageReqVO);
        // 导出 Excel
        ExcelUtils.write(response, filename, "数据", AdditionalRecordingExportRespVO.class, list);
    }

    @GetMapping("/query")
    @Operation(summary = "查询补录数据")
    //@PreAuthorize("@ss.hasPermission('power:additional-recording:query')")
    public CommonResult<List<AdditionalRecordingDO>> getAdditionalRecordingPage(
            BigDecimal minThisValue, BigDecimal maxThisValue,
            String recordPerson,
            Integer recordMethod,
            LocalDateTime startThisCollectTime, LocalDateTime endThisCollectTime,
            LocalDateTime startEnterTime, LocalDateTime endEnterTime) {
        return success(additionalRecordingService.selectByCondition(minThisValue, maxThisValue, recordPerson, recordMethod, startThisCollectTime, endThisCollectTime, startEnterTime, endEnterTime));
    }


    /**
     * 批量导入
     *
     * @param file
     * @param acqNameStart
     * @param acqNameEnd
     * @param acqTimeStart
     * @param acqTimeEnd
     * @return
     */
    @Operation(summary = "批量导入")
    @PostMapping("/importExcelData")
    public CommonResult<AcqDataExcelListResultVO> importExcelData(@RequestParam(value = "file") MultipartFile file,
                                                                  @RequestParam String acqNameStart, @RequestParam String acqNameEnd,
                                                                  @RequestParam String acqTimeStart, @RequestParam String acqTimeEnd) {
        try {
            return success(excelMeterDataProcessor.process(file.getInputStream(), acqTimeStart, acqTimeEnd, acqNameStart, acqNameEnd));
        } catch (IOException e) {
            log.error("Excel解析失败{}", e.getMessage(), e);
            return error(IMPORT_EXCEL_ERROR);
        }

    }


}