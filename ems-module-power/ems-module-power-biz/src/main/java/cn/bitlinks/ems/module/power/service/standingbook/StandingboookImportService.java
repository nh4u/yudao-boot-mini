package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.MeterRelationExcelDTO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookExcelDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.enums.standingbook.ImportTemplateType;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.hutool.core.collection.CollUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class StandingboookImportService {

    @Resource
    private MeterRelationService meterRelationService;

    @Resource
    @Lazy
    private StandingbookService standingbookService;
    @Resource
    @Lazy
    private StandingbookTypeService standingbookTypeService;

   // private static final Pattern PAREN_PATTERN = Pattern.compile(".*\\((.*)\\).*");
    // 单次缓存批量大小
    private static final int BATCH_COUNT = 500;
    private boolean validateHeader(List<String> uploadedHeaders, ImportTemplateType templateType,
                             List<String> configHeaders) {
        return uploadedHeaders.equals(templateType.getBaseHeaders().addAll(configHeaders));

    }


    private String extractBracketValue(String value) {
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
    public String importExcel(MultipartFile file) {
        if (file.isEmpty()) return "文件不能为空";
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) return "请上传Excel格式文件（.xlsx/.xls）";

        List<Integer> errorRowNums = new ArrayList<>();
        List<StandingbookExcelDTO> batchList = new ArrayList<>(BATCH_COUNT);
// 查询所有台账类型
        List<StandingbookTypeDO> standingbookTypeDOS = standingbookTypeService.getStandingbookTypeList();
        if (CollUtil.isEmpty(standingbookTypeDOS)) {
            return "系统不存在台账分类";
        }
        Map<String,StandingbookTypeDO> typeDOCodeMap = standingbookTypeDOS.stream()
                .collect(Collectors.toMap(StandingbookTypeDO::getCode, type -> type));
        List<String> typeCodes = standingbookTypeDOS.stream()
                .map(StandingbookTypeDO::getCode)   // 提取 code 字段
                .filter(Objects::nonNull)           // 过滤掉 null
                .distinct()                         // 去重（可选）
                .collect(Collectors.toList());

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row titleRow = sheet.getRow(2);
            // 解析表头
            List<String> headers = new ArrayList<>();
            for (Cell cell : titleRow) {
                cell.setCellType(CellType.STRING); // 避免数字/日期格式
                headers.add(cell.getStringCellValue().trim());
            }
            // 获取最新标签表头
            List<String> topLabelNames = standingbookService.loadTopLevelLabelNames();
            // 获取最新模板表头
            ImportTemplateType tmplEnum ;
            // 重点设备模板
            if(sheet.getSheetName().equals(ImportTemplateType.EQUIPMENT.name())){
                tmplEnum = ImportTemplateType.EQUIPMENT;
            }else if(sheet.getSheetName().equals(ImportTemplateType.METER.name())){
                tmplEnum = ImportTemplateType.METER;
            }else{
                throw exception("模板数据与最新模板数据不一致，请重新下载最新模板");
            }
            boolean isNew = validateHeader(headers, tmplEnum, topLabelNames);
            if(!isNew){
                throw exception("模板数据与最新模板数据不一致，请重新下载最新模板");
            }

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
                if(tmplEnum.equals(ImportTemplateType.EQUIPMENT)){
                    String sbName = getCellValue(row.getCell(1));
                    String sbCode = getCellValue(row.getCell(2));

                    dto.setSbName(sbName);
                    dto.setSbCode(sbCode);
                    dynamicStartIndex = 3;
                    // 根据动态表头topLabelNames，获取非必填的值和表头组成map存放到dto
                }else{
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


                if (!validateSingleRow(dto)) {
                    errorRowNums.add(rowNum);
                    continue;
                }

                batchList.add(dto);

                // 批量达到阈值，暂存一次
                if (batchList.size() >= BATCH_COUNT) {
                    //meterRelationService.batchSaveTemp(batchList);
                    batchList.clear();
                }
            }

            // 处理剩余未满 BATCH_COUNT 的数据
            if (!batchList.isEmpty()) {
               // meterRelationService.batchSaveTemp(batchList);
            }


            // 如果有错误行，则导入失败，返回错误行号提示
            if (!errorRowNums.isEmpty()) {
                return buildErrorMsg(errorRowNums);
            }
            // 否则最终入库
            int count = meterRelationService.batchSaveToDb();
            return "导入成功，共导入 " + count + " 条数据";

        } catch (Exception e) {
            e.printStackTrace();
            return "文件解析失败：" + e.getMessage();
        }
    }
    /**
     * 校验台账类型编码是否在系统中存在
     *
     * @param typeCode 台账类型编码
     * @return true=存在，false=不存在
     */
    public boolean checkTypeCodeExists(String typeCode,List<String> typeCodes) {
        if (CollUtil.isEmpty(typeCodes)) {
            return false;
        }
        return typeCodes.contains(typeCode);
    }

    // 构建错误提示信息
    private String buildErrorMsg(List<Integer> errorRowNums) {
        String msg;
        int displayCount = Math.min(errorRowNums.size(), 50);
        msg = errorRowNums.subList(0, displayCount).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (errorRowNums.size() > 50) {
            msg += "...";
        }
        return "第" + msg + "行数据有误";
    }
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    private boolean validateSingleRow(StandingbookExcelDTO dto,ImportTemplateType tmplEnum,List<String> typeCodes) {
        String typeCode = dto.getTypeCode();
        String sbName = dto.getSbName();
        String sbCode = dto.getSbCode();
        String tableType = dto.getTableType();

        if (!StringUtils.hasText(typeCode)) return false;
        if (!StringUtils.hasText(sbCode)) return false;
        if(tmplEnum.equals(ImportTemplateType.METER)){
            // 表类型必须存在
            if (!StringUtils.hasText(tableType)) return false;
            // 必须是实体表计/虚拟表计
            if(!Arrays.asList("实体表计","虚拟表计").contains(tableType)){
                return false;
            }
        }
        // 校验台账类型编码必须存在
        if (!checkTypeCodeExists(typeCode,typeCodes)) return false;
        // 校验台账编码必须不存在
        if(checkMeterCodeExists(sbCode)) return false;
        // 校验标签编码必须存在
        Map<String,String> labelMap = dto.getLabelMap();
        if(CollUtil.isNotEmpty(labelMap)){
            labelMap.values();
        }
        if(){

        }


        return true;
    }

    public boolean checkMeterCodeExists(String meterCode) {
        List<String> codeList = standingbookService.getStandingbookCodeMeasurementList();
        if (CollUtil.isEmpty(codeList)) {
            return false;
        }
        return codeList.contains(meterCode);
    }
}