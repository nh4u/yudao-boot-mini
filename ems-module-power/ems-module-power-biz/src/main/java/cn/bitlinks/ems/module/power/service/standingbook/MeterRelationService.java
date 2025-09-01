package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.MeterRelationExcelDTO;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.STAGE;

/**
 * 计量器具关联关系业务服务（实际业务需对接数据库/缓存）
 */
@Slf4j
@Service("meterRelationService")
public class MeterRelationService {
    @Resource
    @Lazy
    private StandingbookService standingbookService;

    /**
     * 校验计量器具编号是否在系统中存在
     *
     * @param meterCode 计量器具编号
     * @return true=存在，false=不存在
     */
    public boolean checkMeterCodeExists(String meterCode) {
        Set<String> codeList = standingbookService.getStandingbookCodeMeasurementSet();
        if (CollUtil.isEmpty(codeList)) {
            return false;
        }
        return codeList.contains(meterCode);
    }

    /**
     * 校验多个下级计量器具编号是否存在（返回不存在的编号）
     *
     * @param subMeterCodes 下级计量器具编号数组
     * @return 不存在的编号集合（空=全部存在）
     */
    public Set<String> checkSubMeterCodesExists(String[] subMeterCodes) {
        Set<String> codeList = standingbookService.getStandingbookCodeMeasurementSet();
        if (CollUtil.isEmpty(codeList)) {
            return new HashSet<>(Arrays.asList(subMeterCodes));
        }
        return Arrays.asList(subMeterCodes).stream()
                .filter(code -> !codeList.contains(code))
                .collect(Collectors.toSet());
    }

    /**
     * 校验关联设备是否存在
     */
    public boolean checkDeviceExists(String device) {
        Set<String> codeList = standingbookService.getStandingbookCodeDeviceSet();
        if (CollUtil.isEmpty(codeList)) {
            return false;
        }
        return codeList.contains(device);
    }

    /**
     * 校验环节是否存在
     */
    public boolean checkLinkExists(String link) {
        List<String> systemLinks = DictFrameworkUtils.getDictDataLabelList(
                STAGE);
        return systemLinks.contains(link);
    }

    /**
     * 批量暂存合法数据（避免内存溢出，解析完成后统一入库）
     */
    public void batchSaveTemp(List<MeterRelationExcelDTO> validDataList) {
        // 实际逻辑：存入临时表或Redis缓存（示例仅打印日志） todo
        log.info("暂存合法数据{}条：{}", validDataList.size(), validDataList);
    }

    /**
     * 最终入库（Excel解析完成且无错误时调用）
     */
    public int batchSaveToDb() {
        // 实际逻辑：从临时表/缓存读取数据，批量插入正式表 todo

        log.info("合法数据全部入库完成");
        return 100;
    }

    public String importMeterRelation(MultipartFile file) {
        // 1. 校验文件合法性
        if (file.isEmpty()) {
            return "文件不能为空";
        }
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            return "请上传Excel格式文件（.xlsx/.xls）";
        }
        // 2. 初始化解析监听器（注入业务服务）
        MeterRelationExcelListener listener = new MeterRelationExcelListener(this);

        // return "";
        try {
            // 3. 调用EasyExcel解析文件（指定监听、模型、是否忽略标题行）
            EasyExcel.read(file.getInputStream(), MeterRelationExcelDTO.class, listener)
                    .sheet() // 读取第一个sheet
                    .headRowNumber(1) // 第1行为标题行（忽略不解析）
                    .doRead(); // 开始解析

            // 4. 处理校验结果
            List<Integer> errorRowNums = listener.getErrorRowNums();
            if (!errorRowNums.isEmpty()) {
                // 4.1 错误行号处理：超过50行用“...”代替
                String errorMsg = buildErrorMsg(errorRowNums);
                return errorMsg;
            }

            // 4.2 无错误：执行最终入库
            int importCount = batchSaveToDb();
            return "导入成功，共导入" + importCount + "条数据";

        } catch (IOException e) {
            log.error("Excel文件解析失败", e);
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