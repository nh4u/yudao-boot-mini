package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.MeterRelationExcelDTO;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 计量器具关联关系 Excel 解析监听器（核心校验逻辑）
 */
@Slf4j
public class MeterRelationExcelListener implements ReadListener<MeterRelationExcelDTO> {

    // 英文字符;的正则（校验下级计量器具编号分隔符）
    private static final Pattern SEMICOLON_PATTERN = Pattern.compile("^[\\w\\-]+(;[\\w\\-]+)*$");
    // 单次缓存数据量（避免内存溢出，可根据实际调整）
    private static final int BATCH_COUNT = 500;
    // 错误行号收集（Excel 行号从1开始，标题行忽略）
    private final List<Integer> errorRowNums = new ArrayList<>();
    // 合法数据收集（用于后续入库）
    private List<MeterRelationExcelDTO> validDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    // 系统字典查询服务（注入实际业务服务，用于校验“编号是否存在”）
    private final MeterRelationService meterRelationService;

    // 构造器注入业务服务
    public MeterRelationExcelListener(MeterRelationService meterRelationService) {
        this.meterRelationService = meterRelationService;
    }

    /**
     * 逐行解析 Excel 数据（每行都会触发）
     * @param dto 解析后的单行数据
     * @param context 解析上下文（包含行号）
     */
    @Override
    public void invoke(MeterRelationExcelDTO dto, AnalysisContext context) {
        // 1. 填充行号（context.readRowHolder().getRowIndex() 从0开始，+1转为实际Excel行号）
        int rowNum = context.readRowHolder().getRowIndex() + 1;
        dto.setRowNum(rowNum);
        log.info("开始解析 Excel 第{}行数据：{}", rowNum, dto);

        // 2. 执行核心校验（校验失败则记录行号，不加入合法数据）
        if (!validateSingleRow(dto, rowNum)) {
            errorRowNums.add(rowNum);
            return;
        }

        // 3. 合法数据加入缓存，达到批量阈值时暂存（避免内存溢出）
        validDataList.add(dto);
        if (validDataList.size() >= BATCH_COUNT) {
            meterRelationService.batchSaveTemp(validDataList); // 暂存到临时表/缓存
            validDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT); // 清空缓存
        }
    }

    /**
     * Excel 解析完成后触发（最后一批数据处理 + 错误汇总）
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 处理最后一批未达到阈值的合法数据
        if (!CollectionUtils.isEmpty(validDataList)) {
            meterRelationService.batchSaveTemp(validDataList);
        }
        log.info("Excel 解析完成，共解析{}行，错误{}行", 
                 context.readRowHolder().getRowIndex() + 1, errorRowNums.size());
    }

    /**
     * 单行数据校验（核心规则实现）
     * @param dto 单行数据
     * @param rowNum 行号
     * @return true=校验通过，false=校验失败
     */
    private boolean validateSingleRow(MeterRelationExcelDTO dto, int rowNum) {
        String meterCode = dto.getMeterCode();
        String subMeterCodes = dto.getSubMeterCodes();
        String relatedDevice = dto.getRelatedDevice();
        String stage = dto.getStage();

        // 校验1：计量器具编号为空，其余字段有数据 → 失败
        if (StringUtils.isEmpty(meterCode)) {
            if (StringUtils.hasText(subMeterCodes) || StringUtils.hasText(relatedDevice) || StringUtils.hasText(stage)) {
                log.error("第{}行校验失败：计量器具编号为空，但其余字段有数据", rowNum);
                return false;
            }
            // 若所有字段都为空（空行），也视为失败（可根据需求调整）
            log.error("第{}行校验失败：计量器具编号为空（必填项）", rowNum);
            return false;
        }

        // 校验2：下级计量器具编号存在时，必须用英文;分隔 → 失败
        if (StringUtils.hasText(subMeterCodes)) {
            if (!SEMICOLON_PATTERN.matcher(subMeterCodes).matches()) {
                log.error("第{}行校验失败：下级计量器具编号使用非法分隔符（需用英文;）", rowNum);
                return false;
            }
        }

        // 校验3：关键编号在系统中不存在 → 失败（调用业务服务查询系统字典）
        // 3.1 校验计量器具编号是否存在
        if (!meterRelationService.checkMeterCodeExists(meterCode)) {
            log.error("第{}行校验失败：计量器具编号{}在系统中不存在", rowNum, meterCode);
            return false;
        }
        // 3.2 校验下级计量器具编号是否存在（多个需拆分）
        if (StringUtils.hasText(subMeterCodes)) {
            String[] subCodes = subMeterCodes.split(";");
            Set<String> notExistSubCodes = meterRelationService.checkSubMeterCodesExists(subCodes);
            if (!notExistSubCodes.isEmpty()) {
                log.error("第{}行校验失败：下级计量器具编号{}在系统中不存在", rowNum, notExistSubCodes);
                return false;
            }
        }
        // 3.3 校验关联设备是否存在（非必填，有值才校验）
        if (StringUtils.hasText(relatedDevice) && !meterRelationService.checkDeviceExists(relatedDevice)) {
            log.error("第{}行校验失败：关联设备{}在系统中不存在", rowNum, relatedDevice);
            return false;
        }
        // 3.4 校验环节是否存在（非必填，有值才校验）
        if (StringUtils.hasText(stage) && !meterRelationService.checkLinkExists(stage)) {
            log.error("第{}行校验失败：环节{}在系统中不存在", rowNum, stage);
            return false;
        }

        // 所有校验通过
        return true;
    }

    // 获取错误行号列表（供接口层生成提示信息）
    public List<Integer> getErrorRowNums() {
        return errorRowNums;
    }

    // 获取合法数据总数（供接口层返回结果）
    public int getValidDataCount() {
        return (validDataList.size() + (BATCH_COUNT * (errorRowNums.size() / BATCH_COUNT)));
    }
}