package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookExcelDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.enums.standingbook.ImportTemplateType;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

@Slf4j
@Service
public class StandingbookImportService {


    @Resource
    @Lazy
    private StandingbookService standingbookService;
    @Resource
    @Lazy
    private StandingbookTypeService standingbookTypeService;
    @Resource
    @Lazy
    private LabelConfigService labelConfigService;
    @Resource
    @Lazy
    private StandingbookImportActualService standingbookImportActualService;

    // 单次缓存批量大小
    private static final int BATCH_COUNT = 500;

    private boolean validateHeader(List<String> uploadedHeaders, ImportTemplateType templateType,
                                   List<String> configHeaders) {
        // 1. Defensive copy of baseHeaders (to avoid modifying the original list)
        List<String> combinedHeaders = new ArrayList<>(templateType.getBaseHeaders());

        // 2. Add all configHeaders to the copied list
        combinedHeaders.addAll(configHeaders);

        // 3. Compare uploadedHeaders with the combinedHeaders list
        return uploadedHeaders.equals(combinedHeaders);

    }


    public String importExcel(MultipartFile file) {
        if (file.isEmpty()) {
            throw exception(STANDINGBOOK_IMPORT_FILE_ERROR);
        }
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw exception(STANDINGBOOK_IMPORT_EXCEL_ERROR);
        }


        List<Integer> errorRowNums = new ArrayList<>();
        List<StandingbookExcelDTO> batchList = new ArrayList<>(BATCH_COUNT);
        // 查询所有标签
        List<LabelConfigDO> sysLabelList = labelConfigService.getAllLabelConfig();
        List<String> sysLabelCodes = sysLabelList.stream()
                .map(LabelConfigDO::getCode)   // 提取 code 字段
                .filter(Objects::nonNull)           // 过滤掉 null
                .distinct()                         // 去重（可选）
                .collect(Collectors.toList());

        // 查询所有台账类型
        List<StandingbookTypeDO> standingbookTypeDOS = standingbookTypeService.getStandingbookTypeList();
        if (CollUtil.isEmpty(standingbookTypeDOS)) {
            throw exception(STANDINGBOOK_IMPORT_SYSTEM_DATA_ERROR);
        }

        Map<String, StandingbookTypeDO> typeDOCodeMap = standingbookTypeDOS.stream()
                .filter(type -> type != null && type.getCode() != null && !type.getCode().trim().isEmpty()) // Filter out null and empty codes
                .collect(Collectors.toMap(
                        StandingbookTypeDO::getCode,      // KeyMapper: use the code
                        Function.identity(),              // ValueMapper: use the object itself
                        (existing, replacement) -> existing // MergeFunction: if codes are duplicated, keep the existing one (you might want to change this logic)
                ));
        List<String> typeCodes = standingbookTypeDOS.stream()
                .map(StandingbookTypeDO::getCode)   // 提取 code 字段
                .filter(Objects::nonNull)           // 过滤掉 null
                .distinct()                         // 去重（可选）
                .collect(Collectors.toList());

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row titleRow = sheet.getRow(1);
            // 解析表头
            List<String> headers = new ArrayList<>();
            for (Cell cell : titleRow) {
                cell.setCellType(CellType.STRING); // 避免数字/日期格式
                headers.add(cell.getStringCellValue().trim());
            }
            // 获取最新标签表头
            List<String> topLabelNames = standingbookService.loadTopLevelLabelNames();
            // 获取最新模板表头
            ImportTemplateType tmplEnum;
            // 重点设备模板
            if (!headers.contains("*表类型")) {
                tmplEnum = ImportTemplateType.EQUIPMENT;
            } else {
                tmplEnum = ImportTemplateType.METER;
            }
            boolean isNew = validateHeader(headers, tmplEnum, topLabelNames);
            if (!isNew) {
                throw exception(STANDINGBOOK_IMPORT_TMPL_ERROR);
            }

            Set<String> existCode = new HashSet<>();
            // 解析数据
            for (int i = 2; i <= sheet.getLastRowNum(); i++) { // 从第3行开始
                Row row = sheet.getRow(i);
                int rowNum = i + 1;
                if (row == null) continue;


                String typeCode = extractBracketValue(getCellValue(row.getCell(0)));
                // 固定属性
                StandingbookExcelDTO dto = new StandingbookExcelDTO();
                dto.setTypeCode(typeCode);
                int dynamicStartIndex;
                if (tmplEnum.equals(ImportTemplateType.EQUIPMENT)) {
                    String sbName = getCellValue(row.getCell(1));
                    String sbCode = getCellValue(row.getCell(2));

                    dto.setSbName(sbName);
                    dto.setSbCode(sbCode);
                    dynamicStartIndex = 3;
                    // 根据动态表头topLabelNames，获取非必填的值和表头组成map存放到dto
                } else {
                    String tableType = getCellValue(row.getCell(1));
                    String sbName = getCellValue(row.getCell(2));
                    String sbCode = getCellValue(row.getCell(3));
                    dto.setTableType(tableType);
                    dto.setSbName(sbName);
                    dto.setSbCode(sbCode);
                    dynamicStartIndex = 4;
                }
                // 填充标签数据
                Map<String, String> extAttrs = new HashMap<>();
                for (int j = 0; j < topLabelNames.size(); j++) {
                    int colIndex = dynamicStartIndex + j; // 起始下标，EQUIPMENT=3, METER=4
                    String labelName = topLabelNames.get(j);

                    String rawValue = getCellValue(row.getCell(colIndex));
                    String parsedValue = extractBracketValue(rawValue);
                    if (parsedValue != null && !parsedValue.isEmpty()) {
                        extAttrs.put(labelName, parsedValue);
                    }

                }
                dto.setLabelMap(extAttrs);


                // 校验单行数据+ 校验列表中是否已存在了设备编号，如果存在，则加入errorRowNums
                if (!validateSingleRow(dto, tmplEnum, typeCodes, sysLabelCodes) || existCode.contains(dto.getSbCode())) {
                    errorRowNums.add(rowNum);
                    continue;
                }
                existCode.add(dto.getSbCode());
                batchList.add(dto);

                // 批量达到阈值，暂存一次
                if (batchList.size() >= BATCH_COUNT) {
                    standingbookImportActualService.batchSaveTemp(batchList);
                    batchList.clear();
                }
            }

            // 处理剩余未满 BATCH_COUNT 的数据
            if (!batchList.isEmpty()) {
                standingbookImportActualService.batchSaveTemp(batchList);
            }


            // 如果有错误行，则导入失败，返回错误行号提示
            if (!errorRowNums.isEmpty()) {
                throw exception(new ErrorCode(STANDINGBOOK_IMPORT_ALL_ERROR.getCode(), buildErrorMsg(errorRowNums)));
            }
            // 否则最终入库
            int count = standingbookImportActualService.batchSaveToDb(typeDOCodeMap);
            return "导入成功，共导入 " + count + " 条数据";

        } catch (Exception e) {
            log.error("台账文件解析失败", e);
            throw exception(new ErrorCode(STANDINGBOOK_IMPORT_ALL_ERROR.getCode(), e.getMessage()));
        }
    }


    public String extractBracketValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        int start = value.indexOf('(');
        int end = value.indexOf(')');
        if (start != -1 && end != -1 && end > start) {
            return value.substring(start + 1, end);
        }
        return value;
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
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private boolean validateSingleRow(StandingbookExcelDTO dto, ImportTemplateType tmplEnum, List<String> typeCodes, List<String> sysLabelCodes) {
        String typeCode = dto.getTypeCode();
        String sbName = dto.getSbName();
        String sbCode = dto.getSbCode();
        String tableType = dto.getTableType();

        if (!StringUtils.hasText(typeCode)) return false;
        if (!StringUtils.hasText(sbName)) return false;
        if (!StringUtils.hasText(sbCode)) return false;
        if (tmplEnum.equals(ImportTemplateType.METER)) {
            // 表类型必须存在
            if (!StringUtils.hasText(tableType)) return false;
            // 必须是实体表计/虚拟表计
            if (!Arrays.asList("实体表计", "虚拟表计").contains(tableType)) {
                return false;
            }
        }
        // 校验台账类型编码必须存在
        if (!standingbookImportActualService.checkTypeCodeExists(typeCode, typeCodes)) return false;
        // 校验台账编码必须不存在
        if (standingbookImportActualService.checkMeterCodeExists(sbCode,tmplEnum)) return false;
        // 校验标签编码必须存在
        // 校验标签编码必须存在
        Map<String, String> labelMap = dto.getLabelMap();
        if (CollUtil.isNotEmpty(labelMap)) {
            List<String> labelCodes = new ArrayList<>(labelMap.values());
            if (!standingbookImportActualService.checkLabelCodesExists(labelCodes, sysLabelCodes)) {
                return false;
            }
        }

        return true;
    }

}