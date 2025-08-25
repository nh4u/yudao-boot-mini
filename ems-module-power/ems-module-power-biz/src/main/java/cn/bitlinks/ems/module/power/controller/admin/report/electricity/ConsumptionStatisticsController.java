package cn.bitlinks.ems.module.power.controller.admin.report.electricity;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsInfo;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.ConsumptionStatisticsResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.FullCellMergeStrategy;
import cn.bitlinks.ems.module.power.service.report.electricity.ConsumptionStatisticsService;
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


@Tag(name = "管理后台 - 个性化报表[电]-用电量统计")
@RestController
@RequestMapping("/power/report/electricity/consumptionStatistics")
@Validated
public class ConsumptionStatisticsController {
    @Resource
    private ConsumptionStatisticsService consumptionStatisticsService;


    @PostMapping("/consumptionStatisticsTable")
    @Operation(summary = "用电量统计（表）")
    public CommonResult<ConsumptionStatisticsResultVO<ConsumptionStatisticsInfo>> consumptionStatisticsTable(@Valid @RequestBody ConsumptionStatisticsParamVO paramVO) {
        return success(consumptionStatisticsService.consumptionStatisticsTable(paramVO));
    }


    @PostMapping("/consumptionStatisticsChart")
    @Operation(summary = "用电量统计（图）")
    public CommonResult<ConsumptionStatisticsChartResultVO> consumptionStatisticsChart(@Valid @RequestBody ConsumptionStatisticsParamVO paramVO) {
        return success(consumptionStatisticsService.consumptionStatisticsChart(paramVO));
    }

    @PostMapping("/exportConsumptionStatisticsTable")
    @Operation(summary = "导出用电量统计表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportConsumptionStatisticsTable(@Valid @RequestBody ConsumptionStatisticsParamVO paramVO,
                                                 HttpServletResponse response) throws IOException {
        paramVO.setQueryType(QueryDimensionEnum.OVERALL_REVIEW.getCode());
        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);
        Integer mergeIndex = labelDeep - 1;
        // 文件名字处理
        String filename = "用电量统计表" + XLSX;

        List<List<String>> header = consumptionStatisticsService.getExcelHeader(paramVO);
        List<List<Object>> dataList = consumptionStatisticsService.getExcelData(paramVO);


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
}
