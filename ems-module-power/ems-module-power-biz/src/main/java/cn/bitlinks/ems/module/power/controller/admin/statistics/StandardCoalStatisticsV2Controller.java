package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.service.statistics.StandardCoalStructureV2Service;
import cn.bitlinks.ems.module.power.service.statistics.StandardCoalV2Service;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsService;
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

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 用能分析")
@RestController
@RequestMapping("/power/statistics/standardCoal/v2")
@Validated
public class StandardCoalStatisticsV2Controller {

    @Resource
    private StandardCoalV2Service standardCoalV2Service;

    @Resource
    private StandardCoalStructureV2Service standardCoalStructureV2Service;
    @Resource
    private StatisticsService statisticsService;


    @PostMapping("/standardCoalAnalysisTable")
    @Operation(summary = "折标煤分析（表）V2")
    public CommonResult<StatisticsResultV2VO<StandardCoalInfo>> standardCoalAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(standardCoalV2Service.standardCoalAnalysisTable(paramVO));
    }


    @PostMapping("/standardCoalAnalysisChart")
    @Operation(summary = "折标煤分析（图）V2")
    public CommonResult<StatisticsChartResultV2VO> standardCoalAnalysisChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(standardCoalV2Service.standardCoalAnalysisChart(paramVO));
    }


    @PostMapping("/export-excel")
    @Operation(summary = "导出折标煤分析表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCopExcel(@Valid @RequestBody StatisticsParamV2VO paramVO,
                               HttpServletResponse response) throws IOException {

        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = standardCoalV2Service.getLabelDeep(childLabels);
        Integer mergeIndex = 0;
        // 文件名字处理
        Integer queryType = paramVO.getQueryType();
        String filename = "";
        switch (queryType) {
            case 0:
                filename = STANDARD_COAL_ALL + XLSX;
                mergeIndex = labelDeep;
                break;
            case 1:
                filename = STANDARD_COAL_ENERGY + XLSX;
                // 能源不需要合并
                mergeIndex = 0;
                break;
            case 2:
                filename = STANDARD_COAL_LABEL + XLSX;
                // 标签没有能源
                mergeIndex = labelDeep - 1;
                break;
            default:
                filename = DEFAULT + XLSX;
        }

        List<List<String>> header = standardCoalV2Service.getExcelHeader(paramVO);
        List<List<Object>> dataList = standardCoalV2Service.getExcelData(paramVO);


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


    @PostMapping("/standardCoalStructureAnalysisTable")
    @Operation(summary = "用能结构分析（表）V2")
    public CommonResult<StatisticsResultV2VO<StructureInfo>> standardCoalStructureAnalysisTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(standardCoalStructureV2Service.standardCoalStructureAnalysisTable(paramVO));
    }

    @PostMapping("/standardCoalStructureAnalysisChart")
    @Operation(summary = "用能结构分析（图）V2")
    public CommonResult<StatisticsChartPieResultVO> standardCoalStructureAnalysisChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(standardCoalStructureV2Service.standardCoalStructureAnalysisChart(paramVO));
    }


    @PostMapping("/energyFlowAnalysis")
    @Operation(summary = "能流分析V2")
    public CommonResult<EnergyFlowResultVO> energyFlowAnalysis(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        EnergyFlowResultVO jsonObject = statisticsService.energyFlowAnalysisV2(paramVO);
        return success(jsonObject);
    }
}