package cn.bitlinks.ems.module.power.controller.admin.voucher;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageParam;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherRespVO;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import cn.bitlinks.ems.module.power.service.voucher.VoucherService;
import cn.hutool.json.JSONObject;
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

@Tag(name = "管理后台 - 凭证管理")
@RestController
@RequestMapping("/power/voucher")
@Validated
public class VoucherController {

    @Resource
    private VoucherService voucherService;

    @PostMapping("/create")
    @Operation(summary = "创建凭证管理")
    @PreAuthorize("@ss.hasPermission('power:voucher:create')")
    public CommonResult<VoucherRespVO> createVoucher(@Valid @RequestBody VoucherSaveReqVO createReqVO) {
        VoucherDO voucher = voucherService.createVoucher(createReqVO);
        return success(BeanUtils.toBean(voucher, VoucherRespVO.class));
    }

//    voucherService.createVoucher(createReqVO)

    @PutMapping("/update")
    @Operation(summary = "更新凭证管理")
    @PreAuthorize("@ss.hasPermission('power:voucher:update')")
    public CommonResult<Boolean> updateVoucher(@Valid @RequestBody VoucherSaveReqVO updateReqVO) {
        voucherService.updateVoucher(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除凭证管理")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:voucher:delete')")
    public CommonResult<Boolean> deleteVoucher(@RequestParam("id") Long id) {
        voucherService.deleteVoucher(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得凭证管理")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:voucher:query')")
    public CommonResult<VoucherRespVO> getVoucher(@RequestParam("id") Long id) {
        VoucherDO voucher = voucherService.getVoucher(id);
        return success(BeanUtils.toBean(voucher, VoucherRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得凭证管理分页")
    @PreAuthorize("@ss.hasPermission('power:voucher:query')")
    public CommonResult<PageResult<VoucherRespVO>> getVoucherPage(@Valid VoucherPageReqVO pageReqVO) {
        PageResult<VoucherDO> pageResult = voucherService.getVoucherPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, VoucherRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出凭证管理 Excel")
    @PreAuthorize("@ss.hasPermission('power:voucher:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportVoucherExcel(@Valid VoucherPageReqVO pageReqVO,
                                   HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<VoucherDO> list = voucherService.getVoucherPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "凭证管理.xls", "数据", VoucherRespVO.class,
                BeanUtils.toBean(list, VoucherRespVO.class));
    }

    @DeleteMapping("/delete-batch")
    @Operation(summary = "批量删除凭证管理")
    @PreAuthorize("@ss.hasPermission('power:voucher:delete')")
    public CommonResult<Boolean> deleteVouchers(@RequestBody List<Long> ids) {
        voucherService.deleteVouchers(ids);
        return success(true);
    }

    @GetMapping("/recognition")
    @Operation(summary = "凭证识别")
    @Parameter(name = "url", description = "文件地址", required = true, example = "xxx.jpg")
    @PreAuthorize("@ss.hasPermission('power:voucher:query')")
    public CommonResult<JSONObject> recognition(@RequestParam("url") String url) {
        JSONObject result = voucherService.recognition(url);
        return success(result);
    }

}