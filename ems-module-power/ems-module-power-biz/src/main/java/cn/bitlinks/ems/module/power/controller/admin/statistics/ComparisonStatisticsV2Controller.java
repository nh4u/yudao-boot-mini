package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.enums.StatisticsQueryType;
import cn.bitlinks.ems.module.power.excelstyle.FullCellMergeStrategy;
import cn.bitlinks.ems.module.power.service.statistics.ComparisonV2Service;
import com.alibaba.excel.EasyExcel;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.getLabelDeep;


@Tag(name = "管理后台 - 用能分析/环比分析")
@RestController
@RequestMapping("/power/statistics/comparison/v2")
@Validated
public class ComparisonStatisticsV2Controller {

    @Resource
    private ComparisonV2Service comparisonV2Service;


    @PostMapping("/discountAnalysisTable")
    @Operation(summary = "用能成本环比分析（表）")
    public CommonResult<StatisticsResultV2VO> discountAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(comparisonV2Service.discountAnalysisTable(paramVO));
    }

    @PostMapping("/discountAnalysisChart")
    @Operation(summary = "用能成本环比分析（图）")
    public CommonResult<ComparisonChartResultVO> discountAnalysisChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(comparisonV2Service.discountAnalysisChart(paramVO));

    }
    @PostMapping("/exportDiscountAnalysisTable")
    @Operation(summary = "导出用能成本环比分析表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDiscountAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO,
                                            HttpServletResponse response) throws IOException {

        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);
        Integer mergeIndex = 0;
        // 文件名字处理
        Integer queryType = paramVO.getQueryType();
        String filename = "";
        switch (queryType) {
            case 0:
                filename = COST_MOM_ALL + XLSX;
                mergeIndex = labelDeep;
                break;
            case 1:
                filename = COST_MOM_ENERGY + XLSX;
                // 能源不需要合并
                mergeIndex = 0;
                break;
            case 2:
                filename = COST_MOM_LABEL + XLSX;
                // 标签没有能源
                mergeIndex = labelDeep - 1;
                break;
            default:
                filename = DEFAULT + XLSX;
        }

        List<List<String>> header = comparisonV2Service.getExcelHeader(paramVO,2);
        List<List<Object>> dataList = comparisonV2Service.getExcelData(paramVO,2);


        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("Access-Control-Expose-Headers","File-Name");
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
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                // 设置表头行高 30，内容行高 20
                .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 15, (short) 15))
                // 自适应表头宽度
//                .registerWriteHandler(new MatchTitleWidthStyleStrategy())
                // 由于column索引从0开始 返回来的labelDeep是从1开始，又由于有个能源列，所以合并索引 正好相抵，直接使用labelDeep即可
                .registerWriteHandler(new FullCellMergeStrategy(0, null, 0, mergeIndex))
                .sheet("数据").doWrite(dataList);
    }

    @PostMapping("/foldCoalAnalysisTable")
    @Operation(summary = "折煤环比分析（表）")
    public CommonResult<StatisticsResultV2VO> foldCoalAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(comparisonV2Service.foldCoalAnalysisTable(paramVO));
    }

    @PostMapping("/foldCoalAnalysisChart")
    @Operation(summary = "折煤环比分析（图）")
    public CommonResult<ComparisonChartResultVO> foldCoalAnalysisChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(comparisonV2Service.foldCoalAnalysisChart(paramVO));

    }
    @PostMapping("/exportFoldCoalAnalysisTable")
    @Operation(summary = "导出折煤环比分析表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFoldCoalAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO,
                                            HttpServletResponse response) throws IOException {

        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);
        Integer mergeIndex = 0;
        // 文件名字处理
        Integer queryType = paramVO.getQueryType();
        String filename = "";
        switch (queryType) {
            case 0:
                filename = STANDARD_COAL_MOM_ALL + XLSX;
                mergeIndex = labelDeep;
                break;
            case 1:
                filename = STANDARD_COAL_MOM_ENERGY + XLSX;
                // 能源不需要合并
                mergeIndex = 0;
                break;
            case 2:
                filename = STANDARD_COAL_MOM_LABEL + XLSX;
                // 标签没有能源
                mergeIndex = labelDeep - 1;
                break;
            default:
                filename = DEFAULT + XLSX;
        }

        List<List<String>> header = comparisonV2Service.getExcelHeader(paramVO,1);
        List<List<Object>> dataList = comparisonV2Service.getExcelData(paramVO,1);


        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("Access-Control-Expose-Headers","File-Name");
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
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                // 设置表头行高 30，内容行高 20
                .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 15, (short) 15))
                // 自适应表头宽度
//                .registerWriteHandler(new MatchTitleWidthStyleStrategy())
                // 由于column索引从0开始 返回来的labelDeep是从1开始，又由于有个能源列，所以合并索引 正好相抵，直接使用labelDeep即可
                .registerWriteHandler(new FullCellMergeStrategy(0, null, 0, mergeIndex))
                .sheet("数据").doWrite(dataList);
    }

    @PostMapping("/utilizationRateTable")
    @Operation(summary = "利用率表")
    public CommonResult<StatisticsResultV2VO<ComparisonItemVO>> getUtilizationRateTable(@Valid @RequestBody BaseTimeDateParamVO paramVO) {
        StatisticsParamV2VO vo = BeanUtils.toBean(paramVO, StatisticsParamV2VO.class);
        vo.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());
        return success(comparisonV2Service.getUtilizationRateTable(vo));
    }

    @PostMapping("/utilizationRateChart")
    @Operation(summary = "利用率图")
    public CommonResult<ComparisonChartResultVO> getUtilizationRateChart(@Valid @RequestBody BaseTimeDateParamVO paramVO) {
        StatisticsParamV2VO vo = BeanUtils.toBean(paramVO, StatisticsParamV2VO.class);
        vo.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());
        return success(comparisonV2Service.getUtilizationRateChart(vo));
    }

    @PostMapping("/exportUtilizationRateTable")
    @Operation(summary = "导出利用率分析表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDiscountAnalysisTable(@Valid @RequestBody BaseTimeDateParamVO paramVO,
                                            HttpServletResponse response) throws IOException {
        StatisticsParamV2VO vo = BeanUtils.toBean(paramVO, StatisticsParamV2VO.class);
        vo.setQueryType(StatisticsQueryType.COMPREHENSIVE_VIEW.getCode());

        // 文件名字处理
        String filename = USAGE_RATE_MOM_ALL + XLSX;

        List<List<String>> header = comparisonV2Service.getExcelHeader(vo);
        List<List<Object>> dataList = comparisonV2Service.getExcelData(vo);

        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("File-Name", URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));


        EasyExcel.write(response.getOutputStream())
                //自适应宽度
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(15))
                // 动态头
                .head(header)
                .sheet("数据")
                // 表格数据
                .doWrite(dataList);
    }
}
