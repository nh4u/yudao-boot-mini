package cn.bitlinks.ems.module.power.controller.admin.starrocks;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.power.service.starrocks.StarrocksService;
import cn.bitlinks.ems.module.power.service.usagecost.CalcUsageCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - starrocks示例")
@RestController
@RequestMapping("/power/starrocks")
@Validated
public class StarrocksController {

    @Resource
    private StarrocksService starrocksService;

    @Resource
    private CalcUsageCostService calcUsageCostService;

    @GetMapping("/get")
    @Operation(summary = "starrocks示例-查询")
    @PreAuthorize("@ss.hasPermission('power:starrocks:query')")
    public CommonResult<List<Map<String, Objects>>> get() {
        List<Map<String, Objects>> map = starrocksService.queryData();

        return success(map);
    }

    @GetMapping("/addData")
    @Operation(summary = "starrocks示例-添加")
    @PreAuthorize("@ss.hasPermission('power:starrocks:add')")
    public CommonResult<Boolean> add(@RequestParam(name = "DATE") String date) {
        starrocksService.addData(date);
        return success(true);
    }

    @GetMapping("/deleteData")
    @Operation(summary = "starrocks示例-删除")
    @PreAuthorize("@ss.hasPermission('power:starrocks:delete')")
    public CommonResult<Boolean> deleteData(@RequestParam(name = "DATE") String date) {
        starrocksService.deleteData(date);
        return success(true);
    }

    @GetMapping("/test")
    @Operation(summary = "starrocks示例-删除")
    public CommonResult<Boolean> test() {
        calcUsageCostService.process(generateMockData(10));
        return success(true);
    }

    public static List<MinuteAggregateDataDTO> generateMockData(int count) {
        List<MinuteAggregateDataDTO> dataList = new ArrayList<>();
        Random random = new Random();
        List<Long> sid = new ArrayList<>();
        sid.add(1901888368999768065L);
        sid.add(1901888619034812418L);
        sid.add(1901888185771597826L);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < count; i++) {
            MinuteAggregateDataDTO data = new MinuteAggregateDataDTO();
            now = now.plusHours(random.nextInt(20)).plusMinutes(random.nextInt(60));
            data.setAggregateTime(now);
            data.setParamCode("usage");
            data.setEnergyFlag(true);
            data.setDataSite("IO:TAG:" + (i + 1));
            data.setStandingbookId(sid.get(random.nextInt(sid.size())));
            data.setFullValue(BigDecimal.valueOf(1000 + random.nextInt(500)));
            data.setIncrementalValue(BigDecimal.valueOf(random.nextDouble() * 10).setScale(2, BigDecimal.ROUND_HALF_UP));

            dataList.add(data);
        }

        return dataList;
    }
}