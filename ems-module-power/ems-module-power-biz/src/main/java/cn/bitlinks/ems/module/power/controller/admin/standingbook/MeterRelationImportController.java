package cn.bitlinks.ems.module.power.controller.admin.standingbook;


import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.MeterRelationExcelDTO;
import cn.bitlinks.ems.module.power.service.standingbook.MeterRelationExcelListener;
import cn.bitlinks.ems.module.power.service.standingbook.MeterRelationService;
import com.alibaba.excel.EasyExcel;
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
import java.io.IOException;
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
    private MeterRelationService meterRelationService;

    /**
     * 批量导入关联关系（接收Excel文件）
     *
     * @param file 前端上传的Excel文件
     * @return 导入结果（成功/失败信息 + 错误行号）
     */
    @PostMapping("/import")
    @Operation(summary = "计量器具导入")
    public CommonResult<String> importMeterRelation(@RequestParam("file") MultipartFile file) {
        return success(actualImport(file));
    }

    public String actualImport(MultipartFile file) {
        // 1. 校验文件合法性
        if (file.isEmpty()) {
            return "文件不能为空";
        }
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            return "请上传Excel格式文件（.xlsx/.xls）";
        }
        // 2. 初始化解析监听器（注入业务服务）
        MeterRelationExcelListener listener = new MeterRelationExcelListener(meterRelationService);

        // return "";
        try {
            // 3. 调用EasyExcel解析文件（指定监听、模型、是否忽略标题行）
            EasyExcel.read(file.getInputStream(), MeterRelationExcelDTO.class, listener)
                    .sheet() // 读取第一个sheet
                    .headRowNumber(2) // 第1行为标题行（忽略不解析）
                    .autoTrim(true) // 自动去除单元格前后空格（4.0.x 新增）
                    .doRead(); // 开始解析

            // 4. 处理校验结果
            List<Integer> errorRowNums = listener.getErrorRowNums();
            if (!errorRowNums.isEmpty()) {
                // 4.1 错误行号处理：超过50行用“...”代替
                String errorMsg = buildErrorMsg(errorRowNums);
                return errorMsg;
            }

            // 4.2 无错误：执行最终入库
            int importCount = meterRelationService.batchSaveToDb();
            return "导入成功，共导入" + importCount + "条数据";

        } catch (IOException e) {
            e.printStackTrace();
            return "文件解析失败，请检查文件完整性";
        } catch (Exception e) {
            log.error("导入过程异常", e);
            return "导入异常：" + e.getMessage();
        }
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