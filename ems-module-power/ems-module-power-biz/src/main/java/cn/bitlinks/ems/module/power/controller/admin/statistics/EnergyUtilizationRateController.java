package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.EnergyRateInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.service.statistics.EnergyUtilizationRateService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.SimpleColumnWidthStyleStrategy;
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

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 用能分析 (利用率)")
@RestController
@RequestMapping("/power/statistics/utilizationRate")
@Validated
public class EnergyUtilizationRateController {
    @Resource
    private EnergyUtilizationRateService energyUtilizationRateService;

    @PostMapping("/table")
    @Operation(summary = "表")
    public CommonResult<StatisticsResultV2VO<EnergyRateInfo>> getTable(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(energyUtilizationRateService.getTable(paramVO));
    }

    @PostMapping("/chart")
    @Operation(summary = "图")
    public CommonResult<List<BaseReportChartResultVO<BigDecimal>>> getChart(@Valid @RequestBody StatisticsParamV2VO paramVO) {
        return success(energyUtilizationRateService.getChart(paramVO));
    }

    @PostMapping("/export")
    @Operation(summary = "导出")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCopExcel(@Valid @RequestBody StatisticsParamV2VO paramVO,
                               HttpServletResponse response) throws IOException {

        String filename = "能源利用率分析.xlsx";
        List<List<String>> header = energyUtilizationRateService.getExcelHeader(paramVO);
        List<List<Object>> dataList = energyUtilizationRateService.getExcelData(paramVO);

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
