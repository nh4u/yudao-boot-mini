package cn.bitlinks.ems.module.power.controller.admin.report;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.COP;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/06/21 19:23
 **/

@Tag(name = "管理后台 - COP报表")
@RestController
@RequestMapping("/power/report")
@Validated
public class CopReportController {


    @Resource
    private CopHourAggDataService copHourAggDataService;


    @PostMapping("/copTable")
    @Operation(summary = "COP表")
    public CommonResult<List<Map<String, Object>>> copTable(@Valid @RequestBody ReportParamVO paramVO) {
        return success(copHourAggDataService.copTable(paramVO));
    }

    @PostMapping("/copChart")
    @Operation(summary = "COP图")
    public CommonResult<CopChartResultVO> copChart(@Valid @RequestBody ReportParamVO paramVO) {
        return success(copHourAggDataService.copChart(paramVO));
    }

    //    @GetMapping("/export-excel")
    @Operation(summary = "导出COP Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLabelConfigExcel(ReportParamVO pageReqVO,
                                       HttpServletResponse response) throws IOException {

        List<List<String>> list = new ArrayList<List<String>>();

        list.add(Arrays.asList("", ""));

        list.add(Arrays.asList("2025-1月", "低温冷机"));
        list.add(Arrays.asList("2025-1月", "低温系统"));
        list.add(Arrays.asList("2025-1月", "中温冷机"));
        list.add(Arrays.asList("2025-1月", "中温系统"));

        list.add(Arrays.asList("2025-2月", "低温冷机"));
        list.add(Arrays.asList("2025-2月", "低温系统"));
        list.add(Arrays.asList("2025-2月", "中温冷机"));
        list.add(Arrays.asList("2025-2月", "中温系统"));

        list.add(Arrays.asList("2025-3月", "低温冷机"));
        list.add(Arrays.asList("2025-3月", "低温系统"));
        list.add(Arrays.asList("2025-3月", "中温冷机"));
        list.add(Arrays.asList("2025-3月", "中温系统"));

        list.add(Arrays.asList("2025-4月", "低温冷机"));
        list.add(Arrays.asList("2025-4月", "低温系统"));
        list.add(Arrays.asList("2025-4月", "中温冷机"));
        list.add(Arrays.asList("2025-4月", "中温系统"));
        // 表格数据
        List<List<Object>> dataList = ListUtils.newArrayList();
        for (int i = 0; i < 10; i++) {
            List<Object> data = ListUtils.newArrayList();
            data.add("1日" + i + ":00:00");
            for (int j = 0; j < 16; j++) {
                data.add("/");
            }
            dataList.add(data);
        }

        EasyExcel.write("动态表头导出_合并单元格.xlsx")
                // 动态头
                .head(list)
                .sheet("xxx")
                // 表格数据
                .doWrite(dataList);

    }

    @PostMapping("/export-excel")
    @Operation(summary = "导出COP Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCopExcel(@Valid @RequestBody ReportParamVO pageReqVO,
                               HttpServletResponse response) throws IOException {

        String filename = COP + XLSX;
        List<List<String>> header = copHourAggDataService.getExcelHeader(pageReqVO);
        List<List<Object>> dataList = copHourAggDataService.getExcelData(pageReqVO);

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


        EasyExcel.write(response.getOutputStream())
                // 动态头
                .head(header)
                // 自适应  找出数据中最大宽度作为基础宽度（官方不推荐）
//                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                // 固定列宽
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(15))
                .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                // 设置表头行高 30，内容行高 20
                .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 15, (short) 15))
                .sheet("数据")
                // 表格数据
                .doWrite(dataList);
    }
}
