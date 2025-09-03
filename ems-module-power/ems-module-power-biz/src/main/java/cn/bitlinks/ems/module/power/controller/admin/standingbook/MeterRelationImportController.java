package cn.bitlinks.ems.module.power.controller.admin.standingbook;


import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.service.standingbook.MeterRelationImportService;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
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
import java.util.List;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;


/**
 * 计量器具关联关系导入接口
 */
@Tag(name = "管理后台 - 计量器具关联关系导入")
@RestController
@Slf4j
@RequestMapping("/power/standingbook/relations")
@Validated
public class MeterRelationImportController {

    @Resource
    private MeterRelationImportService meterRelationImportService;

    //    /**
//     * 批量导入关联关系（接收Excel文件）
//     *
//     * @param file 前端上传的Excel文件
//     * @return 导入结果（成功/失败信息 + 错误行号）
//     */
//    @PostMapping("/import")
//    @Operation(summary = "计量器具导入")
//    public CommonResult<String> importMeterRelation(@RequestParam("file") MultipartFile file) {
//        return success(actualImport(file));
//    }
    @PostMapping("/import")
    @Operation(summary = "计量器具导入")
    public CommonResult<String> importMeterRelation(@RequestParam("file") MultipartFile file) {
        return success(meterRelationImportService.importExcel(file));
    }


    /**
     * 构建错误提示信息（符合需求：逐行提示，超过50行用“...”）
     */
    private String buildErrorMsg(List<Integer> errorRowNums) {
        int errorCount = errorRowNums.size();
        if (errorCount == 0) {
            return StringPool.EMPTY;
        }
        String rowStr = errorRowNums.stream()
                .limit(50)
                .map(String::valueOf)
                .collect(Collectors.joining(StringPool.COMMA));

        // 超过50行时补充“...”
        if (errorRowNums.size() > 50) {
            rowStr += "...";
        }
        return rowStr;
    }

}