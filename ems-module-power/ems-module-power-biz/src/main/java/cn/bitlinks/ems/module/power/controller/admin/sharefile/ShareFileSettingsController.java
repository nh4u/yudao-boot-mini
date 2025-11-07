package cn.bitlinks.ems.module.power.controller.admin.sharefile;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.service.sharefile.ShareFileSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

/**
 * @author liumingqiang
 */
@Tag(name = "管理后台 - 共享文件同步设置")
@RestController
@RequestMapping("/power/shareFile")
@Validated
public class ShareFileSettingsController {

    @Resource
    private ShareFileSettingsService shareFileSettingsService;


    @PostMapping("/test")
    @Operation(summary = "测试共享服务连通")
    @PermitAll
    //@PreAuthorize("@ss.hasPermission('power:service-settings:create')")
    public CommonResult<Map<String, List<Map<String, Object>>>> testShareFileSettings() throws IOException {
        return success(shareFileSettingsService.testShareFile());
    }


    @PostMapping("/manual")
    @Operation(summary = "手动执行一次")
    @PermitAll
    //@PreAuthorize("@ss.hasPermission('power:service-settings:create')")
    public CommonResult<Boolean> manualShareFileSettings() throws IOException {
        shareFileSettingsService.dealFile();
        return success(true);
    }

    @GetMapping("/testDirYear")
    @Operation(summary = "手动执行目录YEAR")
    @PermitAll
    public CommonResult<Boolean> testExcelYear(@RequestParam("dir") String dir) {
        shareFileSettingsService.dealFile(dir, true);
        return success(true);
    }

    @GetMapping("/testDir")
    @Operation(summary = "手动执行目录")
    @PermitAll
    public CommonResult<Boolean> testExcel(@RequestParam("dir") String dir) {
        shareFileSettingsService.dealFile(dir, false);
        return success(true);
    }

}