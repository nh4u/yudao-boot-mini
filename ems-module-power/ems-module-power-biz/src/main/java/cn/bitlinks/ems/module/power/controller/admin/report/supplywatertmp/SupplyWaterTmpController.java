package cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.FullCellMergeStrategy;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.SupplyAnalysisPieResultVO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;
import cn.bitlinks.ems.module.power.service.report.supplywatertmp.SupplyWaterTmpSettingsService;
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
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.SUPPLY_WATER_TMP;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.XLSX;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/08/05 19:23
 **/

@Tag(name = "管理后台 - 个性化报表[暖通]-供水温度报表")
@RestController
@RequestMapping("/power/supplyWaterTmp")
@Validated
public class SupplyWaterTmpController {

    @Resource
    private SupplyWaterTmpSettingsService supplyWaterTmpSettingsService;

    @PutMapping("/updateBatch")
    @Operation(summary = "更新供水温度设置批量")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_water_tmp_settings:update')")
    public CommonResult<Boolean> updateBatch(@Valid @RequestBody List<SupplyWaterTmpSettingsSaveReqVO> supplyWaterTmpSettingsList) {
        supplyWaterTmpSettingsService.updateBatch(supplyWaterTmpSettingsList);
        return success(true);
    }


    @GetMapping("/list")
    @Operation(summary = "获得供水温度设置List")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_water_tmp_settings:query')")
    public CommonResult<List<SupplyWaterTmpSettingsRespVO>> getSupplyWaterTmpSettingsList(@Valid SupplyWaterTmpSettingsPageReqVO pageReqVO) {
        List<SupplyWaterTmpSettingsDO> list = supplyWaterTmpSettingsService.getSupplyWaterTmpSettingsList(pageReqVO);
        return success(BeanUtils.toBean(list, SupplyWaterTmpSettingsRespVO.class));
    }

    @GetMapping("/getSystem")
    @Operation(summary = "获得供水温度系统")
    //@PreAuthorize("@ss.hasPermission('power:power_supply_water_tmp_settings:query')")
    public CommonResult<List<String>> getSystem() {
        List<String> list = supplyWaterTmpSettingsService.getSystem();
        return success(list);
    }


    @PostMapping("/supplyWaterTmpTable")
    @Operation(summary = "供水温度表")
    public CommonResult<SupplyWaterTmpTableResultVO> supplyWaterTmpTable(@Valid @RequestBody SupplyWaterTmpReportParamVO paramVO) {
        return success(supplyWaterTmpSettingsService.supplyWaterTmpTable(paramVO));
    }

    @PostMapping("/supplyWaterTmpChart")
    @Operation(summary = "供水温度图")
    public CommonResult<SupplyAnalysisPieResultVO> supplyWaterTmpChart(@Valid @RequestBody SupplyWaterTmpReportParamVO paramVO) {
        return success(supplyWaterTmpSettingsService.supplyWaterTmpChart(paramVO));
    }

    @PostMapping("/exportSupplyWaterTmpTable")
    @Operation(summary = "导出供水温度表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportSupplyWaterTmpTable(@Valid @RequestBody SupplyWaterTmpReportParamVO paramVO,
                                          HttpServletResponse response) throws IOException {


        Integer mergeIndex = 0;
        // 文件名字处理
        String filename = SUPPLY_WATER_TMP + XLSX;

        List<List<String>> header = supplyWaterTmpSettingsService.getExcelHeader(paramVO);
        List<List<Object>> dataList = supplyWaterTmpSettingsService.getExcelData(paramVO);

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
