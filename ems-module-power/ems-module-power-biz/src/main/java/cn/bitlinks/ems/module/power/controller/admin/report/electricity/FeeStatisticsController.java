package cn.bitlinks.ems.module.power.controller.admin.report.electricity;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.FeeChartYInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.excelstyle.FullCellMergeStrategy;
import cn.bitlinks.ems.module.power.service.report.electricityfee.FeeStatisticsService;
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
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.STATISTICS_FEE;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.getLabelDeep;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 个性化报表[电]-电费统计")
@RestController
@RequestMapping("/power/report/electricity/feeStatistics")
@Validated
public class FeeStatisticsController {

    @Resource
    private FeeStatisticsService feeStatisticsService;


    @PostMapping("/feeStatisticsTable")
    @Operation(summary = "电费统计表")
    public CommonResult<StatisticsResultV2VO<StatisticsInfoV2>> feeStatisticsTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(feeStatisticsService.feeStatisticsTable(paramVO));
    }

    @PostMapping("/feeStatisticsChart")
    @Operation(summary = "电费统计图")
    public CommonResult<FeeChartResultVO<FeeChartYInfo>> feeStatisticsChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(feeStatisticsService.feeStatisticsChart(paramVO));
    }

    @PostMapping("/exportFeeStatisticsTable")
    @Operation(summary = "导出电费统计表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportFeeStatisticsTable(@Valid @RequestBody StatisticsParamV2VO paramVO,
                                         HttpServletResponse response) throws IOException {

        String childLabels = paramVO.getChildLabels();
        Integer labelDeep = getLabelDeep(childLabels);
        Integer mergeIndex = labelDeep - 1;
        // 文件名字处理
        String filename = STATISTICS_FEE + XLSX;

        List<List<String>> header = feeStatisticsService.getExcelHeader(paramVO);
        List<List<Object>> dataList = feeStatisticsService.getExcelData(paramVO);

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

        // 3. 使用 EasyExcel 写入到 response.getOutputStream()
        try (OutputStream outputStream = response.getOutputStream()) {
            EasyExcelFactory.write(outputStream)
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
        } catch (Exception e) {
            e.printStackTrace(); // 或者没打印
        }// try-with-resources 会自动关闭 outputStream
    }


}
