package cn.bitlinks.ems.module.power.controller.admin.statistics;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.enums.StatisticsQueryType;
import cn.bitlinks.ems.module.power.service.statistics.StandardCoalStructureV2Service;
import cn.bitlinks.ems.module.power.service.statistics.StandardCoalV2Service;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsService;
import com.alibaba.excel.EasyExcel;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

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
    public void exportCopExcel(@Valid @RequestBody StatisticsParamV2VO pageReqVO,
                               HttpServletResponse response) throws IOException {
        // 文件名字处理
        Integer queryType = pageReqVO.getQueryType();
        String queryTypeStr = "";
        switch (queryType) {
            case 0:
                queryTypeStr = StatisticsQueryType.COMPREHENSIVE_VIEW.getDetail();
                break;
            case 1:
                queryTypeStr = StatisticsQueryType.ENERGY_VIEW.getDetail();
                break;
            case 2:
                queryTypeStr = StatisticsQueryType.TAG_VIEW.getDetail();
                break;
            default:

        }

        String filename = "折标煤分析表" + queryTypeStr + ".xlsx";

        List<List<String>> header = standardCoalV2Service.getExcelHeader(pageReqVO);
        List<List<Object>> dataList = standardCoalV2Service.getExcelData(pageReqVO);
        // 需要合并的列
        int[] mergeColumnIndex = {0,1,2,3};
        // 需要从第几行开始合并
        int mergeRowIndex = 3;

        // 放在 write前配置response才会生效，放在后面不生效
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8.name()));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");

        EasyExcel.write(response.getOutputStream())
                // 动态头
                .head(header)
                .sheet("数据")
                // 表格数据
                .doWrite(dataList);
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