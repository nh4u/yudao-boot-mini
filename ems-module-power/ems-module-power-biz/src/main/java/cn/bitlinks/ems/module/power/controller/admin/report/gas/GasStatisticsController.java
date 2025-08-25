package cn.bitlinks.ems.module.power.controller.admin.report.gas;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.*;
import cn.bitlinks.ems.module.power.service.report.gas.GasStatisticsService;
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

@Tag(name = "管理后台 - 个性化报表[气]-气化科报表")
@RestController
@RequestMapping("/power/report/gas/gasStatistics")
@Validated
public class GasStatisticsController {
    @Resource
    private GasStatisticsService gasStatisticsService;

    @GetMapping("/getPowerTankSettings")
    @Operation(summary = "获得储罐液位设置列表")
    public CommonResult<List<PowerTankSettingsRespVO>> getPowerTankSettings() {
        return success(gasStatisticsService.getPowerTankSettings());
    }

    @PostMapping("/savePowerTankSettings")
    @Operation(summary = "保存储罐液位设置")
    public CommonResult<Boolean> savePowerTankSettings(@Valid @RequestBody SettingsParamVO paramVO) {
        return success(gasStatisticsService.savePowerTankSettings(paramVO));
    }

    @GetMapping("/getEnergyStatisticsItems")
    @Operation(summary = "获得能源统计项列表")
    public CommonResult<List<EnergyStatisticsItemInfoRespVO>> getEnergyStatisticsItems() {
        return success(gasStatisticsService.getEnergyStatisticsItems());
    }


    @PostMapping("/gasStatisticsTable")
    @Operation(summary = "气化科报表")
    public CommonResult<GasStatisticsResultVO<GasStatisticsInfo>> gasStatisticsTable(@Valid @RequestBody GasStatisticsParamVO paramVO) {
        return success(gasStatisticsService.gasStatisticsTable(paramVO));
    }

    @PostMapping("/exportGasStatisticsTable")
    @Operation(summary = "导出气化科报表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportGasStatisticsTable(@Valid @RequestBody GasStatisticsParamVO paramVO,
                                         HttpServletResponse response) throws IOException {

        // 设置导出文件名
        String filename = "气化科报表.xlsx";

        // 获取Excel表头数据
        // 表头格式：
        // 第一行：表单名称（气化科报表）- 需要合并所有列
        // 第二行：统计周期（yyyy-MM-dd~yyyy-MM-dd）- 需要合并所有列
        // 第三行：能源统计项、计量器具编号、日期列表
        List<List<String>> header = gasStatisticsService.getExcelHeader(paramVO);

        // 获取Excel数据内容
        // 数据格式：每行包含能源统计项、计量器具编号、各时间点的数值
        List<List<Object>> dataList = gasStatisticsService.getExcelData(paramVO);

        // 配置HTTP响应头，设置文件下载
        // 必须在write之前配置response才会生效，放在后面不生效
        // 设置Content-Disposition头，指定文件名为附件下载
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        // 设置Content-Type为Excel文件类型
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
        // 添加跨域访问头，允许前端获取文件名
        response.addHeader("Access-Control-Expose-Headers", "File-Name");
        response.addHeader("File-Name", URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));

        // 创建表头样式
        WriteCellStyle headerStyle = new WriteCellStyle();
        // 设置水平居中对齐，使表头文字居中显示
        headerStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        // 设置垂直居中对齐，使表头文字垂直居中
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 可以设置背景色，目前注释掉保持默认
        // headerStyle.setFillBackgroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        // 可以设置字体样式，目前注释掉保持默认
        // WriteFont headerFont = new WriteFont();
        // headerFont.setFontHeightInPoints((short) 10);
        // headerFont.setColor(IndexedColors.WHITE.getIndex());
        // headerStyle.setWriteFont(headerFont);

        // 创建内容样式
        WriteCellStyle contentStyle = new WriteCellStyle();
        // 设置水平居中对齐，使数据内容居中显示
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        // 设置垂直居中对齐，使数据内容垂直居中
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 设置边框样式，为所有单元格添加细边框
        contentStyle.setBorderLeft(BorderStyle.THIN);    // 左边框
        contentStyle.setBorderTop(BorderStyle.THIN);     // 上边框
        contentStyle.setBorderRight(BorderStyle.THIN);   // 右边框
        contentStyle.setBorderBottom(BorderStyle.THIN);  // 下边框

        // 使用EasyExcel写入Excel文件
        EasyExcelFactory.write(response.getOutputStream())
                // 设置表头数据
                .head(header)
                // 注册列宽策略，设置每列宽度为15个字符
                .registerWriteHandler(new SimpleColumnWidthStyleStrategy(20))
                // 注册单元格样式策略，应用表头和内容的样式
                .registerWriteHandler(new HorizontalCellStyleStrategy(headerStyle, contentStyle))
                // 注册行高策略，设置表头行高为15，内容行高为15
                .registerWriteHandler(new SimpleRowHeightStyleStrategy((short) 15, (short) 15))
                // 可以注册自适应表头宽度策略，目前注释掉
                // .registerWriteHandler(new MatchTitleWidthStyleStrategy())
                // 设置工作表名称为"数据"
                .sheet("数据")
                // 执行写入操作，将数据写入Excel文件
                .doWrite(dataList);
    }
}
