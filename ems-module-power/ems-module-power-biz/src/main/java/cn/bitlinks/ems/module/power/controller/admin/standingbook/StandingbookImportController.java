package cn.bitlinks.ems.module.power.controller.admin.standingbook;


import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;


/**
 * 台账与标签导入接口
 */
@Tag(name = "管理后台 - 台账与标签导入")
@RestController
@Slf4j
@RequestMapping("/power/standingbook/import")
@Validated
public class StandingbookImportController {

    @Resource
    private StandingbookImportService standingbookImportService;


    @PostMapping("/all")
    @Operation(summary = "台账标签导入")
    public CommonResult<String> importAll(@RequestParam("file") MultipartFile file) {
        return success(standingbookImportService.importExcel(file));
    }

}