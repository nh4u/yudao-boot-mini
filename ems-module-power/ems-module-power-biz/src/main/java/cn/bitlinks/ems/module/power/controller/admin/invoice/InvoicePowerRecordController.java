package cn.bitlinks.ems.module.power.controller.admin.invoice;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordRespVO;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordSaveReqVO;
import cn.bitlinks.ems.module.power.service.invoice.InvoicePowerRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 发票电量记录")
@RestController
@RequestMapping("/power/invoice-power-record")
@Validated
public class InvoicePowerRecordController {

    @Resource
    private InvoicePowerRecordService invoicePowerRecordService;

    @PostMapping("/save")
    @Operation(summary = "新增/修改发票电量记录（数据补录弹窗提交）")
    @PreAuthorize("@ss.hasPermission('power:invoice-power-record:save')")
    public CommonResult<Long> save(@Valid @RequestBody InvoicePowerRecordSaveReqVO reqVO) {
        Long id = invoicePowerRecordService.saveInvoicePowerRecord(reqVO);
        return success(id);
    }

    @GetMapping("/get")
    @Operation(summary = "获取发票电量记录详情（编辑回显）")
    @PreAuthorize("@ss.hasPermission('power:invoice-power-record:query')")
    public CommonResult<InvoicePowerRecordRespVO> get(@RequestParam("id") Long id) {
        return success(invoicePowerRecordService.getInvoicePowerRecord(id));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询发票电量记录（列表展示）")
    @PreAuthorize("@ss.hasPermission('power:invoice-power-record:query')")
    public CommonResult<PageResult<InvoicePowerRecordRespVO>> getPage(@Valid InvoicePowerRecordPageReqVO pageReqVO) {
        return success(invoicePowerRecordService.getInvoicePowerRecordPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出发票电量记录 Excel（与列表一致）")
    @PreAuthorize("@ss.hasPermission('power:invoice-power-record:export')")
    public void exportExcel(@Valid InvoicePowerRecordPageReqVO exportReqVO,
                            HttpServletResponse response) throws IOException {
        invoicePowerRecordService.exportInvoicePowerRecordExcel(response, exportReqVO);
    }


}
