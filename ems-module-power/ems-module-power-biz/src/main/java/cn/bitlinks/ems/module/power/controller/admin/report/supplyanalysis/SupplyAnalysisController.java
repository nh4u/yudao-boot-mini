package cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.FullCellMergeStrategy;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplyanalysis.SupplyAnalysisSettingsDO;
import cn.bitlinks.ems.module.power.service.report.supplyanalysis.SupplyAnalysisSettingsService;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
import com.alibaba.excel.write.style.row.SimpleRowHeightStyleStrategy;
import io.swagger.v3.oas.annotations.Operation;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.SUPPLY_ANALYSIS;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/06/21 19:23
 **/

@Tag(name = "管理后台 - 供应分析报表")
@RestController
@RequestMapping("/power/supplyAnalysis")
@Validated
public class SupplyAnalysisController {


    @Resource
    private SupplyAnalysisSettingsService supplyAnalysisSettingsService;

    @PutMapping("/updateBatch")
    @Operation(summary = "更新供应分析设置批量")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_analysis_settings:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody List<SupplyAnalysisSettingsSaveReqVO> supplyAnalysisSettingsList) {
        supplyAnalysisSettingsService.updateBatch(supplyAnalysisSettingsList);
        return success(true);
    }


    @GetMapping("/list")
    @Operation(summary = "获得供应分析设置List")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_analysis_settings:query')")
    public CommonResult<List<SupplyAnalysisSettingsRespVO>> getSupplyAnalysisSettingsList(@Valid SupplyAnalysisSettingsPageReqVO pageReqVO) {
        List<SupplyAnalysisSettingsDO> list = supplyAnalysisSettingsService.getSupplyAnalysisSettingsList(pageReqVO);
        return success(BeanUtils.toBean(list, SupplyAnalysisSettingsRespVO.class));
    }

    @GetMapping("/getSystem")
    @Operation(summary = "获得供应分析系统")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_analysis_settings:query')")
    public CommonResult<List<String>> getSystem() {
        List<String> list = supplyAnalysisSettingsService.getSystem();
        return success(list);
    }


    @PostMapping("/supplyAnalysisTable")
    @Operation(summary = "供应分析表")
    public CommonResult<StatisticsResultV2VO> supplyAnalysisTable(@Valid @RequestBody SupplyAnalysisReportParamVO paramVO) {
        return success(supplyAnalysisSettingsService.supplyAnalysisTable(paramVO));
    }

    @PostMapping("/supplyAnalysisChart")
    @Operation(summary = "供应分析图")
    public CommonResult<SupplyAnalysisPieResultVO> supplyAnalysisChart(@Valid @RequestBody SupplyAnalysisReportParamVO paramVO) {
        return success(supplyAnalysisSettingsService.supplyAnalysisChart(paramVO));
    }

    @PostMapping("/exportSupplyAnalysisTable")
    @Operation(summary = "导出供应分析表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSupplyAnalysisTable(@Valid @RequestBody SupplyAnalysisReportParamVO paramVO,
                                          HttpServletResponse response) throws IOException {


        Integer mergeIndex = 0;
        // 文件名字处理
        String filename = SUPPLY_ANALYSIS + XLSX;

        List<List<String>> header = supplyAnalysisSettingsService.getExcelHeader(paramVO);
        List<List<Object>> dataList = supplyAnalysisSettingsService.getExcelData(paramVO);

        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
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
//        headerFont.setFontHeightInPoints((short) 10);
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

        EasyExcelFactory.write(response.getOutputStream())
                .head(header)
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(15))
                .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                // 设置表头行高 30，内容行高 20
                .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 15, (short) 15))
                // 自适应表头宽度
//                .registerWriteHandler(new MatchTitleWidthStyleStrategy())
                // 由于column索引从0开始 返回来的labelDeep是从1开始，又由于有个能源列，所以合并索引 正好相抵，直接使用labelDeep即可
                .registerWriteHandler(new FullCellMergeStrategy(0, null, 0, mergeIndex))
                .sheet("数据").doWrite(dataList);
    }
}
