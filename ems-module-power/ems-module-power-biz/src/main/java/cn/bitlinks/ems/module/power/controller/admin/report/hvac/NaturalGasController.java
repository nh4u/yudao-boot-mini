package cn.bitlinks.ems.module.power.controller.admin.report.hvac;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.*;
import cn.bitlinks.ems.module.power.service.report.hvac.NaturalGasService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;


@Tag(name = "管理后台 - 个性化报表[暖通科报表]-天然气报表")
@RestController
@RequestMapping("/power/report/hvac/naturalGas")
@Validated
public class NaturalGasController {
    @Resource
    private NaturalGasService naturalGasService;

    @PostMapping("/table")
    @Operation(summary = "表")
    public CommonResult<BaseReportResultVO<NaturalGasInfo>> getTable(@Valid @RequestBody BaseTimeDateParamVO paramVO) {
        return success(naturalGasService.getTable(paramVO));
    }

    @PostMapping("/chart")
    @Operation(summary = "图")
    public CommonResult<BaseReportMultiChartResultVO<Map<String,List<BigDecimal>>>> getChart(@Valid @RequestBody BaseTimeDateParamVO paramVO) {
        return success(naturalGasService.getChart(paramVO));
    }

    @PostMapping("/export")
    @Operation(summary = "导出")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCopExcel(@Valid @RequestBody BaseTimeDateParamVO paramVO,
                               HttpServletResponse response) throws IOException {

        String filename = "天然气用量.xlsx";
        List<List<String>> header = naturalGasService.getExcelHeader(paramVO);
        List<List<Object>> dataList = naturalGasService.getExcelData(paramVO);

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.addHeader("Access-Control-Expose-Headers","File-Name");
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.addHeader("File-Name", URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));

        EasyExcel.write(response.getOutputStream())
                //自适应宽度
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                // 动态头
                .head(header)
                .sheet("数据")
                // 表格数据
                .doWrite(dataList);
    }

}
