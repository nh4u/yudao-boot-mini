package cn.bitlinks.ems.module.power.controller.admin.standingbook;


import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.service.standingbook.MeterRelationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;


/**
 * 计量器具关联关系导入接口
 */
@Tag(name = "管理后台 - 计量器具关联关系导入")
@RestController
@RequestMapping("/power/standingbook/relations")
@Validated
public class MeterRelationImportController {

    @Resource
    private MeterRelationService meterRelationService;

    /**
     * 批量导入关联关系（接收Excel文件）
     *
     * @param file 前端上传的Excel文件
     * @return 导入结果（成功/失败信息 + 错误行号）
     */
    @PostMapping("/import")
    public CommonResult<String> importMeterRelation(@RequestParam("file") MultipartFile file) {
        return success(meterRelationService.importMeterRelation(file));
    }


}