package cn.bitlinks.ems.module.power.controller.admin.starrocks;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.service.starrocks.StarrocksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
}