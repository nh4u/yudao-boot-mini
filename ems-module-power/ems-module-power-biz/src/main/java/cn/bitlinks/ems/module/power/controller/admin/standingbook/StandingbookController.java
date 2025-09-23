package cn.bitlinks.ems.module.power.controller.admin.standingbook;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.excelstyle.PaddedColumnWidthStrategy;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账")
@RestController
@RequestMapping("/power/standingbook")
@Validated
public class StandingbookController {
    @Resource
    private StandingbookService standingbookService;


    @PostMapping("/create")
    @Operation(summary = "创建台账")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:create')")
    public CommonResult<Long> createStandingbook(@Valid @RequestBody Map<String, String> createReqVO) {
        return success(standingbookService.createStandingbook(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<Boolean> updateStandingbook(@Valid @RequestBody Map<String, String> updateReqVO) {
        standingbookService.updateStandingbook(updateReqVO);
        return success(true);
    }


    @DeleteMapping("/delete")
    @Operation(summary = "删除台账")
    @Parameter(name = "id", description = "编号", required = true)
    //@PreAuthorize("@ss.hasPermission('power:standingbook:delete')")
    public CommonResult<Boolean> deleteStandingbook(@RequestBody List<Long> ids) {
        standingbookService.deleteStandingbookBatch(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得台账")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<StandingbookRespVO> getStandingbook(@RequestParam("id") Long id) {
        StandingbookDO standingbook = standingbookService.getStandingbook(id);
        return success(BeanUtils.toBean(standingbook, StandingbookRespVO.class));
    }

    @PostMapping("/list")
    @Operation(summary = "获得台账列表")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> getStandingbookPage(@Valid @RequestBody Map<String, String> pageReqVO) {
        List<StandingbookDO> list = standingbookService.getStandingbookList(pageReqVO);
        List<StandingbookRespVO> respVOS = BeanUtils.toBean(list, StandingbookRespVO.class);
        //补充能源信息
        standingbookService.sbOtherField(respVOS);
        return success(respVOS);
    }

    @PostMapping("/listSbAllWithAssociations")
    @Operation(summary = "关联计量器具：关联下级计量器具/关联设备接口（topType=2）或者重点设备（topType=1）")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> listSbAllWithAssociations(@RequestBody StandingbookAssociationReqVO reqVO) {
        return success(standingbookService.listSbAllWithAssociations(reqVO));
    }

    @PostMapping("/listSbAllWithAssociationsVirtual")
    @Operation(summary = "虚拟表：关联下级计量器具（topType=2）")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> listSbAllWithAssociationsVirtual(@RequestBody StandingbookAssociationReqVO reqVO) {
        return success(standingbookService.listSbAllWithAssociationsVirtual(reqVO));
    }

    @PostMapping("/measurementInstrument")
    @Operation(summary = "虚拟表：关联下级计量")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<Boolean> updAssociationMeasurementInstrument(@Valid @RequestBody MeasurementVirtualAssociationSaveReqVO createReqVO) {
        standingbookService.updAssociationMeasurementInstrument(createReqVO);
        return success(true);
    }

    @PostMapping("/listWithAssociations")
    @Operation(summary = "关联计量器具：根据条件获得台账列表和计量器具联系")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookWithAssociations>> getStandingbookListWithAssociations(@RequestBody Map<String, String> pageReqVO) {
        List<StandingbookWithAssociations> list = standingbookService.getStandingbookListWithAssociations(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookWithAssociations.class));
    }


    @PostMapping("/treeWithEnergyParam")
    @Operation(summary = "根据能源参数名称查询所有的实体计量器具，分类->计量器具名称（编号）")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingBookTypeTreeRespVO>> treeWithEnergyParam(@RequestBody StandingbookEnergyParamReqVO standingbookEnergyParamReqVO) {
        return success(standingbookService.treeWithEnergyParam(standingbookEnergyParamReqVO));
    }

    @PostMapping("/treeDeviceWithParam")
    @Operation(summary = "根据能源参数名称查询所有的重点设备，分类->台账名称（编号）")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingBookTypeTreeRespVO>> treeDeviceWithParam(@RequestBody StandingbookParamReqVO standingbookParamReqVO) {
        return success(standingbookService.treeDeviceWithParam(standingbookParamReqVO));
    }

    @PostMapping("/treeWithEnergyCode")
    @Operation(summary = "根据能源编码查询所有的实体计量器具，分类->计量器具名称（编号）")
    //@PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingBookTypeTreeRespVO>> treeWithEnergyCode(@RequestBody StandingbookEnergyReqVO standingbookEnergyReqVO) {
        return success(standingbookService.treeWithEnergyCode(standingbookEnergyReqVO));
    }
    @GetMapping("/export-meter-template")
    @Operation(summary = "下载计量器具导入模板")
    // @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    public void exportMeterTemplate(HttpServletResponse response) {
        standingbookService.exportMeterTemplate(response);
    }

    @GetMapping("/export-ledger-template")
    @Operation(summary = "下载台账模板")
    // @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    public void exportLedgerTemplate(HttpServletResponse response) throws UnsupportedEncodingException {
        standingbookService.exportLedgerTemplate(response);
    }

    @PostMapping("/exportStandingBook")
    @Operation(summary = "导出台账")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandingBook(@Valid @RequestBody Map<String, String> paramVO,
                                   HttpServletResponse response) throws IOException {
        Integer mergeIndex = 0;
        // 获取导出数据
        StandingbookExportVO vo = standingbookService.getExcelData(paramVO);
        // 文件名字处理
        String filename = vo.getFilename();
        // 表头
        List<List<String>> header = vo.getHeaderList();
        // 行数据
        List<List<Object>> dataList = vo.getDataList();

        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("File-Name", URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");

        WriteCellStyle headerStyle = new WriteCellStyle();
        // 设置水平居中对齐
        headerStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        // 设置垂直居中对齐
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 设置背景色
//        headerStyle.setFillBackgroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        // 设置字体
//        WriteFont headerFont = new WriteFont();
//        headerFont.setFontHeightInPoints((short) 8);
//        headerFont.setColor(IndexedColors.WHITE.getIndex());
//        headerStyle.setWriteFont(headerFont);


        // 创建一个新的 WriteCellStyle 对象
        WriteCellStyle contentStyle = new WriteCellStyle();

        // 设置水平居中对齐
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

        // 设置垂直居中对齐
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 设置边框
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);

        try (OutputStream outputStream = response.getOutputStream()) {
            EasyExcelFactory.write(outputStream)
                    .head(header)
                    .registerWriteHandler(new PaddedColumnWidthStrategy())
                    .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                    // 设置表头行高 30，内容行高 20
                    .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 15, (short) 15))
                    // 自适应表头宽度
//                .registerWriteHandler(new MatchTitleWidthStyleStrategy())
                    // 由于column索引从0开始 返回来的labelDeep是从1开始，又由于有个能源列，所以合并索引 正好相抵，直接使用labelDeep即可
//                    .registerWriteHandler(new FullCellMergeStrategy(0, null, 0, mergeIndex))
                    .sheet("数据").doWrite(dataList);
        } catch (Exception e) {
            e.printStackTrace(); // 或者没打印
        }// try-with-resources 会自动关闭 outputStream
    }

    @PostMapping("/exportSbImportTemplate")
    @Operation(summary = "导出数据补录导入模版")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSbImportTemplate(@Valid @RequestBody Map<String, String> paramVO,
                                       HttpServletResponse response) throws IOException {
        Integer mergeIndex = 0;
        // 获取导出数据
        StandingbookExportVO vo = standingbookService.getImportTemplateExcelData(paramVO);
        // 文件名字处理
        String filename = vo.getFilename();
        // 表头
        List<List<String>> header = vo.getHeaderList();
        // 行数据
        List<List<Object>> dataList = vo.getDataList();

        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("File-Name", URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");

        WriteCellStyle headerStyle = new WriteCellStyle();
        // 设置水平居中对齐
        headerStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        // 设置垂直居中对齐
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 设置背景色
//        headerStyle.setFillBackgroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        // 设置字体
//        WriteFont headerFont = new WriteFont();
//        headerFont.setFontHeightInPoints((short) 8);
//        headerFont.setColor(IndexedColors.WHITE.getIndex());
//        headerStyle.setWriteFont(headerFont);


        // 创建一个新的 WriteCellStyle 对象
        WriteCellStyle contentStyle = new WriteCellStyle();

        // 设置水平居中对齐
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

        // 设置垂直居中对齐
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 设置边框
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);

        try (OutputStream outputStream = response.getOutputStream()) {
            EasyExcelFactory.write(outputStream)
                    .head(header)
                    .registerWriteHandler(new SimpleColumnWidthStyleStrategy(25))
                    .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                    // 设置表头行高 30，内容行高 20
                    .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 20, (short) 17))
                    // 自适应表头宽度
//                .registerWriteHandler(new MatchTitleWidthStyleStrategy())
                    // 由于column索引从0开始 返回来的labelDeep是从1开始，又由于有个能源列，所以合并索引 正好相抵，直接使用labelDeep即可
//                    .registerWriteHandler(new FullCellMergeStrategy(0, null, 0, mergeIndex))
                    .sheet("数据").doWrite(dataList);
        } catch (Exception e) {
            e.printStackTrace(); // 或者没打印
        }// try-with-resources 会自动关闭 outputStream
    }
}
