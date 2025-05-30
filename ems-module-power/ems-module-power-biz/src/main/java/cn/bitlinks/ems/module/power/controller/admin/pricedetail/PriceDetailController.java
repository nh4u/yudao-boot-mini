//package cn.bitlinks.ems.module.power.controller.admin.pricedetail;
//
//import org.springframework.web.bind.annotation.*;
//import javax.annotation.Resource;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.security.access.prepost.PreAuthorize;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.Operation;
//
//import javax.validation.constraints.*;
//import javax.validation.*;
//import javax.servlet.http.*;
//import java.util.*;
//import java.io.IOException;
//
//import cn.bitlinks.ems.framework.common.pojo.PageParam;
//import cn.bitlinks.ems.framework.common.pojo.PageResult;
//import cn.bitlinks.ems.framework.common.pojo.CommonResult;
//import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
//import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
//
//import cn.bitlinks.ems.framework.excel.core.util.ExcelUtils;
//
//import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
//import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.*;
//
//import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.*;
//import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
//import cn.bitlinks.ems.module.power.service.pricedetail.PriceDetailService;
//
//@Tag(name = "管理后台 - 单价详细")
//@RestController
//@RequestMapping("/power/price-detail")
//@Validated
//public class PriceDetailController {
//
//    @Resource
//    private PriceDetailService priceDetailService;
//
//    @DeleteMapping("/delete")
//    @Operation(summary = "删除单价详细")
//    @Parameter(name = "id", description = "编号", required = true)
//    @PreAuthorize("@ss.hasPermission('power:price-detail:delete')")
//    public CommonResult<Boolean> deletePriceDetail(@RequestParam("id") Long id) {
//        priceDetailService.deletePriceDetail(id);
//        return success(true);
//    }
//
//    @GetMapping("/get")
//    @Operation(summary = "获得单价详细")
//    @Parameter(name = "id", description = "编号", required = true, example = "1024")
//    @PreAuthorize("@ss.hasPermission('power:price-detail:query')")
//    public CommonResult<PriceDetailRespVO> getPriceDetail(@RequestParam("id") Long id) {
//        PriceDetailDO priceDetail = priceDetailService.getPriceDetail(id);
//        return success(BeanUtils.toBean(priceDetail, PriceDetailRespVO.class));
//    }
//
//    @GetMapping("/page")
//    @Operation(summary = "获得单价详细分页")
//    @PreAuthorize("@ss.hasPermission('power:price-detail:query')")
//    public CommonResult<PageResult<PriceDetailRespVO>> getPriceDetailPage(@Valid PriceDetailPageReqVO pageReqVO) {
//        PageResult<PriceDetailDO> pageResult = priceDetailService.getPriceDetailPage(pageReqVO);
//        return success(BeanUtils.toBean(pageResult, PriceDetailRespVO.class));
//    }
//
//    @GetMapping("/export-excel")
//    @Operation(summary = "导出单价详细 Excel")
//    @PreAuthorize("@ss.hasPermission('power:price-detail:export')")
//    @ApiAccessLog(operateType = EXPORT)
//    public void exportPriceDetailExcel(@Valid PriceDetailPageReqVO pageReqVO,
//              HttpServletResponse response) throws IOException {
//        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
//        List<PriceDetailDO> list = priceDetailService.getPriceDetailPage(pageReqVO).getList();
//        // 导出 Excel
//        ExcelUtils.write(response, "单价详细.xls", "数据", PriceDetailRespVO.class,
//                        BeanUtils.toBean(list, PriceDetailRespVO.class));
//    }
//
//}