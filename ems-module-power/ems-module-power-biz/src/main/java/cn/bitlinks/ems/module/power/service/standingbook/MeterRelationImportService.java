package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.MeterRelationExcelDTO;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

@Slf4j
@Service
public class MeterRelationImportService {

    @Resource
    private MeterRelationService meterRelationService;

    // 下级计量器具分隔符校验
//    private static final Pattern SEMICOLON_PATTERN = Pattern.compile("^[\\w\\-\\u4e00-\\u9fa5]+(;[\\w\\-\\u4e00-\\u9fa5]+)*$");
    // 存在多层嵌套的可选分组与重复量词 (*套 *) 它让 Java 的正则引擎（基于回溯算法）在面对某些输入时，会尝试巨量的匹配路径，导致指数级的时间复杂度，进而卡死。
//    private static final Pattern SEMICOLON_PATTERN = Pattern.compile("^([^;]+(;[^;]+)*)*$");
    //  改用 更简单、无嵌套量词、无重复分组正则
    private static final Pattern SEMICOLON_PATTERN = Pattern.compile("^[^;]+(?:;[^;]+)*$");

    // 单次缓存批量大小
    private static final int BATCH_COUNT = 500;

    public String importExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw exception(STANDINGBOOK_IMPORT_FILE_ERROR);
        }
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw exception(STANDINGBOOK_IMPORT_EXCEL_ERROR);
        }

        List<Integer> errorRowNums = new ArrayList<>();
        List<MeterRelationExcelDTO> batchList = new ArrayList<>(BATCH_COUNT);

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 2; i <= sheet.getLastRowNum(); i++) { // 从第3行开始
                Row row = sheet.getRow(i);
                int rowNum = i + 1;
                if (row == null) continue;

                String meterCode = getCellValue(row.getCell(0));
                String subMeterCodes = getCellValue(row.getCell(1));
                String relatedDevice = getCellValue(row.getCell(2));
                String stage = getCellValue(row.getCell(3));

                MeterRelationExcelDTO dto = new MeterRelationExcelDTO(
                        meterCode, subMeterCodes, relatedDevice, stage, rowNum
                );

                if (!validateSingleRow(dto)) {
                    errorRowNums.add(rowNum);
                    continue;
                }

                batchList.add(dto);

                // 批量达到阈值，暂存一次
                if (batchList.size() >= BATCH_COUNT) {
                    meterRelationService.batchSaveTemp(batchList);
                    batchList.clear();
                }
            }

            // 处理剩余未满 BATCH_COUNT 的数据
            if (!batchList.isEmpty()) {
                meterRelationService.batchSaveTemp(batchList);
            }


            // 如果有错误行，则导入失败，返回错误行号提示
            if (!errorRowNums.isEmpty()) {
                throw exception(new ErrorCode(STANDINGBOOK_IMPORT_ALL_ERROR.getCode(), buildErrorMsg(errorRowNums)));
            }
            // 否则最终入库
            int count = meterRelationService.batchSaveToDb();
            return "导入成功，共导入 " + count + " 条数据";

        } catch (Exception e) {
            log.error("计量器具关系文件解析失败", e);
            throw exception(new ErrorCode(STANDINGBOOK_IMPORT_ALL_ERROR.getCode(), e.getMessage()));
        }
    }

    // 构建错误提示信息
    private String buildErrorMsg(List<Integer> errorRowNums) {
        String errorMsg = "第%s行数据有误";
        String msg;
        int displayCount = Math.min(errorRowNums.size(), 50);

        // 拼接错误信息，每行一个
        msg = errorRowNums.subList(0, displayCount).stream()
                .map(rowNum -> String.format(errorMsg, rowNum))
                .collect(Collectors.joining("\n"));

        // 如果超过 50 条，加上省略号
        if (errorRowNums.size() > 50) {
            msg += "\n...";
        }

        return msg;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) return cell.getDateCellValue().toString();
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    private boolean validateSingleRow(MeterRelationExcelDTO dto) {
        String meterCode = dto.getMeterCode();
        String subMeterCodes = dto.getSubMeterCodes();
        String relatedDevice = dto.getRelatedDevice();
        String stage = dto.getStage();


        if (!StringUtils.hasText(meterCode)) return false;
        if (StringUtils.hasText(subMeterCodes) && !SEMICOLON_PATTERN.matcher(CharSequenceUtil.strip(subMeterCodes, StringPool.SEMICOLON)).matches()) return false;
        if (!meterRelationService.checkMeterCodeExists(meterCode)) return false;
        if (StringUtils.hasText(subMeterCodes) && !meterRelationService.checkSubMeterCodesExists(subMeterCodes.split(StringPool.SEMICOLON)).isEmpty())
            return false;
        if (StringUtils.hasText(relatedDevice) && !meterRelationService.checkDeviceExists(relatedDevice)) return false;
        if (StringUtils.hasText(stage) && !meterRelationService.checkLinkExists(stage)) return false;

        // 下级计量器具中不能出现自己。
        if(StringUtils.hasText(subMeterCodes) && subMeterCodes.contains(meterCode)) {
            return false;
        }

        return true;
    }
}