package cn.bitlinks.ems.module.power.controller.admin.report.water;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartYInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.excelstyle.FullCellMergeStrategy;
import cn.bitlinks.ems.module.power.excelstyle.HierarchyMergeStrategy;
import cn.bitlinks.ems.module.power.service.report.water.WaterStatisticsService;
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
import static cn.bitlinks.ems.module.power.enums.ExportConstants.WATER_STATISTICS;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.getLabelDeep;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 个性化报表[水]-水科统计")
@RestController
@RequestMapping("/power/report/water")
@Validated
public class WaterStatisticsController {

    @Resource
    private WaterStatisticsService waterStatisticsService;


    @PostMapping("/waterStatisticsTable")
    @Operation(summary = "水科统计表")
    public CommonResult<StatisticsResultV2VO<StatisticsInfoV2>> waterStatisticsTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(waterStatisticsService.waterStatisticsTable(paramVO));
    }

    @PostMapping("/waterStatisticsChart")
    @Operation(summary = "水科统计图")
    public CommonResult<FeeChartResultVO<FeeChartYInfo>> waterStatisticsChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(waterStatisticsService.waterStatisticsChart(paramVO));
    }

    @PostMapping("/exportWaterStatisticsTable")
    @Operation(summary = "导出水科统计表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWaterStatisticsTable(@Valid @RequestBody StatisticsParamV2VO paramVO,
                                           HttpServletResponse response) throws IOException {

        String childLabels = paramVO.getChildLabels();
        Integer queryType = paramVO.getQueryType();

        String filename = WATER_STATISTICS + XLSX;

        // 表头 & 数据
        List<List<String>> header = waterStatisticsService.getExcelHeader(paramVO);
        List<List<Object>> dataList = waterStatisticsService.getExcelData(paramVO);

        // response
        response.addHeader("Content-Disposition", "attachment;filename=" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("File-Name", URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");

        // 样式
        WriteCellStyle headerStyle = new WriteCellStyle();
        headerStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        WriteCellStyle contentStyle = new WriteCellStyle();
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        contentStyle.setBorderLeft(BorderStyle.THIN);
        contentStyle.setBorderTop(BorderStyle.THIN);
        contentStyle.setBorderRight(BorderStyle.THIN);
        contentStyle.setBorderBottom(BorderStyle.THIN);

        // ===== 合并配置 =====
        final int headRowNumber = 4; // 你的 head 是 4 层：表单名称/统计标签/统计周期/末级

        // 用你原来的工具方法算标签深度
        Integer labelDeep = getLabelDeep(childLabels);

        int[] mergeColumns;
        int hierarchyDepth;

        if (QueryDimensionEnum.ENERGY_REVIEW.getCode().equals(queryType)) {
            // 按能源：只合并第 0 列（能源）
            mergeColumns = new int[]{0};
            hierarchyDepth = 0;
        } else if (QueryDimensionEnum.LABEL_REVIEW.getCode().equals(queryType)) {
            // 按标签：合并 0..labelDeep-1 列（标签列）
            hierarchyDepth = labelDeep;
            mergeColumns = java.util.stream.IntStream.range(0, labelDeep).toArray();
        } else {
            // 综合：合并 0..labelDeep-1（标签列）+ labelDeep（能源列）
            hierarchyDepth = labelDeep;
            mergeColumns = java.util.stream.IntStream.range(0, labelDeep + 1).toArray();
        }

        HierarchyMergeStrategy mergeStrategy =
                new HierarchyMergeStrategy(dataList, headRowNumber, mergeColumns, hierarchyDepth);

        EasyExcelFactory.write(response.getOutputStream())
                .head(header)
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 15, (short) 15))
                .registerWriteHandler(mergeStrategy) // 用新的合并策略
                .sheet("数据")
                .doWrite(dataList);
    }



}
