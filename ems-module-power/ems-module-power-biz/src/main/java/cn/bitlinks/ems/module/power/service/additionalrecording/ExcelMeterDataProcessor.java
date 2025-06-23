package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.FullIncrementEnum;
import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelListResultVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelResultVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AdditionalRecordingManualSaveReqVO;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.HeaderCodeMappingVO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.reportcod.HeaderCodeMappingMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import lombok.extern.slf4j.Slf4j;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.hutool.core.date.DatePattern.NORM_DATETIME_MINUTE_FORMATTER;

@Slf4j
@Service
@Validated
public class ExcelMeterDataProcessor {

    @Resource
    private HeaderCodeMappingMapper headerCodeMappingMapper;
    @Resource
    @Lazy
    private AdditionalRecordingService additionalRecordingService;

    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream(new File("D:/工作文件/燕东/51051.xls"))) {
            ExcelMeterDataProcessor processor = new ExcelMeterDataProcessor();
            AcqDataExcelListResultVO result = processor.process(fis, "A4", "A6", "B3", "C3");

//            result.sort(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime));
//            result.stream()
//                    //.filter(s -> s.getStandingbookId().equals("5105F1-a 水泵 正向有功电能"))
//                    .forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AcqDataExcelListResultVO process(InputStream file, String timeStartCell, String timeEndCell,
                                                String meterStartCell, String meterEndCell) throws IOException {

        int[] timeStart = parseCell(timeStartCell);
        int[] timeEnd = parseCell(timeEndCell);
        int[] meterStart = parseCell(meterStartCell);
        int[] meterEnd = parseCell(meterEndCell);

        boolean timeVertical = timeStart[1] == timeEnd[1];
        boolean meterHorizontal = meterStart[0] == meterEnd[0];

        try (Workbook workbook = WorkbookFactory.create(file)) {
            //只判断一个sheet页的数据
            Sheet sheet = workbook.getSheetAt(0);

            List<String> meterNames = parseMeterNames(sheet, meterStart, meterEnd, meterHorizontal);
            List<LocalDateTime> times = parseTimeSeries(sheet, timeStart, timeEnd, timeVertical);
            Map<String, List<BigDecimal>> meterValuesMap = extractMeterValues(sheet, meterNames, timeStart, times, meterStart, timeVertical, meterHorizontal);

            return calculateMinuteDataParallel(meterValuesMap, times, meterNames);
        }
    }

    /**
     * 单元格位置处理
     *
     * @param cellRef
     * @return
     */
    public static int[] parseCell(String cellRef) {
        Matcher matcher = Pattern.compile("([A-Z]+)([0-9]+)").matcher(cellRef.toUpperCase());
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid cell reference: " + cellRef);

        int row = Integer.parseInt(matcher.group(2)) - 1;
        int col = 0;
        for (char ch : matcher.group(1).toCharArray()) {
            col = col * 26 + (ch - 'A' + 1);
        }
        return new int[]{row, col - 1};
    }

    /**
     * 获取所有计量器名称
     *
     * @param sheet
     * @param start
     * @param end
     * @param horizontal
     * @return
     */
    private List<String> parseMeterNames(Sheet sheet, int[] start, int[] end, boolean horizontal) {
        List<String> meterNames = new ArrayList<>();
        if (horizontal) {
            Row row = sheet.getRow(start[0]);
            for (int c = start[1]; c <= end[1]; c++) {
                Cell cell = row.getCell(c);
                meterNames.add(cell == null ? "" : cell.toString());
            }
        } else {
            for (int r = start[0]; r <= end[0]; r++) {
                Row row = sheet.getRow(r);
                Cell cell = row == null ? null : row.getCell(start[1]);
                meterNames.add(cell == null ? "" : cell.toString());
            }
        }
        return meterNames;
    }

    /**
     * 获取表中的时间数据
     *
     * @param sheet
     * @param start
     * @param end
     * @param vertical
     * @return
     */
    private List<LocalDateTime> parseTimeSeries(Sheet sheet, int[] start, int[] end, boolean vertical) {
        List<LocalDateTime> times = new ArrayList<>();
        if (vertical) {
            for (int r = start[0]; r <= end[0]; r++) {
                Row row = sheet.getRow(r);
                Cell cell = row == null ? null : row.getCell(start[1]);
                LocalDateTime dt = parseTime(cell);
                if (dt != null) times.add(dt);
            }
        } else {
            Row row = sheet.getRow(start[0]);
            for (int c = start[1]; c <= end[1]; c++) {
                Cell cell = row == null ? null : row.getCell(c);
                LocalDateTime dt = parseTime(cell);
                if (dt != null) times.add(dt);
            }
        }
        return times;
    }

    /**
     * 计量器具关联的数值
     *
     * @param sheet
     * @param meterNames
     * @param timeStart
     * @param times
     * @param meterStart
     * @param timeVertical
     * @param meterHorizontal
     * @return
     */
    private Map<String, List<BigDecimal>> extractMeterValues(Sheet sheet, List<String> meterNames,
                                                             int[] timeStart, List<LocalDateTime> times,
                                                             int[] meterStart, boolean timeVertical, boolean meterHorizontal) {
        Map<String, List<BigDecimal>> map = new LinkedHashMap<>();
        meterNames.forEach(name -> map.put(name, new ArrayList<>(times.size())));

        if (timeVertical && meterHorizontal) {
            for (int r = timeStart[0]; r < timeStart[0] + times.size(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int i = 0; i < meterNames.size(); i++) {
                    Cell cell = row.getCell(meterStart[1] + i);
                    map.get(meterNames.get(i)).add(getNumericValue(cell));
                }
            }
        } else {
            for (int c = timeStart[1]; c < timeStart[1] + times.size(); c++) {
                for (int i = 0; i < meterNames.size(); i++) {
                    Row row = sheet.getRow(meterStart[0] + i);
                    if (row == null) continue;
                    Cell cell = row.getCell(c);
                    map.get(meterNames.get(i)).add(getNumericValue(cell));
                }
            }
        }
        return map;
    }

    /**
     *
     * @param meterValuesMap
     * @param times
     * @return
     */
    private AcqDataExcelListResultVO calculateMinuteDataParallel(Map<String, List<BigDecimal>> meterValuesMap, List<LocalDateTime> times, List<String> meterNames) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(availableProcessors * 2, meterValuesMap.size()));
        AcqDataExcelListResultVO resultVO = new AcqDataExcelListResultVO();
        List<AcqDataExcelResultVO> failMsgList = new ArrayList<>();
        AtomicInteger acqFailCount = new AtomicInteger();
        List<Future<List<AcqDataExcelResultVO>>> futures = new ArrayList<>();
        //获取表头与台账关系
        Map<String, HeaderCodeMappingVO> standingbookInfo = getStandingbookInfo(meterNames);

        for (Map.Entry<String, List<BigDecimal>> entry : meterValuesMap.entrySet()) {
            String meter = entry.getKey();
            List<BigDecimal> values = entry.getValue();
            if (MapUtil.isEmpty(standingbookInfo) || standingbookInfo.containsKey(meter)) {
                failMsgList.add(AcqDataExcelResultVO.builder().acqCode(meter).mistake(IMPORT_ACQ_MISTAKE.getMsg()).mistakeDetail(IMPORT_ACQ_MISTAKE_DETAIL.getMsg()).build());
                log.info("暂无报表与台账关联信息，不进行计算, 表头：{}", meter);
                acqFailCount.addAndGet(values.size());
                continue;
            }
            HeaderCodeMappingVO headerCodeMappingVO = standingbookInfo.get(meter);
            futures.add(executor.submit(() -> {
                List<AcqDataExcelResultVO> subResult = new ArrayList<>();
                for (int i = 0; i <=  times.size() - 1; i++) {
                    LocalDateTime start = times.get(i);
                    try {
                        // 不是最后一行，需要校验，全量》=上一个数值
                        if(i != times.size()-1){
                            BigDecimal diff = values.get(i + 1).subtract(values.get(i));
                            if (diff.compareTo(BigDecimal.ZERO) < 0) {
                                throw exception(FULL_VALUE_MUST_GT_LEFT);
                            }
                        }

                        AdditionalRecordingManualSaveReqVO additionalRecordingManualSaveReqVO = new AdditionalRecordingManualSaveReqVO();
                        additionalRecordingManualSaveReqVO.setValueType(FullIncrementEnum.FULL.getCode());
                        additionalRecordingManualSaveReqVO.setStandingbookId(headerCodeMappingVO.getStandingbookId());
                        additionalRecordingManualSaveReqVO.setThisCollectTime(start);
                        additionalRecordingManualSaveReqVO.setThisValue(values.get(i));
                        additionalRecordingService.createAdditionalRecording(additionalRecordingManualSaveReqVO);
                    }catch (ServiceException e){
                        subResult.add(AcqDataExcelResultVO.builder().acqCode(meter).acqTime(start.format(NORM_DATETIME_MINUTE_FORMATTER))
                                .mistake(e.getMessage()).mistakeDetail(e.getMessage()).build());
                        acqFailCount.addAndGet(1);
                        log.error("采集点【{}】,采集时间【{}】,采集数值【{}】数据解析失败，数据异常{}",meter,start,values.get(i),e.getMessage(),e);
                    } catch (Exception e){
                        acqFailCount.addAndGet(1);
                        subResult.add(AcqDataExcelResultVO.builder().acqCode(meter).acqTime(start.format(NORM_DATETIME_MINUTE_FORMATTER))
                                .mistake(IMPORT_ACQ_MISTAKE.getMsg()).mistakeDetail(IMPORT_ACQ_MISTAKE_DETAIL.getMsg()).build());
                        log.error("采集点【{}】,采集时间【{}】,采集数值【{}】数据解析失败，数据异常{}",meter,start,values.get(i),e.getMessage(),e);
                    }
                }
                return subResult;
            }));
        }

        for (Future<List<AcqDataExcelResultVO>> future : futures) {
            try {
                failMsgList.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
        executor.shutdown();
        resultVO.setFailList(failMsgList);
        resultVO.setFailAcqTotal(acqFailCount.get());
        return resultVO;
    }

    /**
     * 时间格式化
     *
     * @param cell
     * @return
     */
    private LocalDateTime parseTime(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().withMinute(0).withSecond(0).withNano(0);
        }
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            try {
                if (val.matches("\\d{1,2}:\\d{2}")) {
                    return LocalDateTime.parse(LocalDateTime.now().toLocalDate() + " " + val, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
                return LocalDateTime.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 转为数值 不能转为数值的按0处理
     *
     * @param cell
     * @return
     */
    private BigDecimal getNumericValue(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC)
            return BigDecimal.valueOf(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                log.error("{} not a number", cell.getStringCellValue());
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private Map<String, HeaderCodeMappingVO> getStandingbookInfo(List<String> headList) {
        List<HeaderCodeMappingVO> headerCodeMappingVOS = headerCodeMappingMapper.selectByHeaderCode(headList);
        if (CollUtil.isEmpty(headerCodeMappingVOS)) {
            return null;
        }
        return headerCodeMappingVOS.stream().collect(Collectors.toMap(HeaderCodeMappingVO::getHeader, Function.identity()));
    }

    private MinuteAggregateDataDTO buildMinuteAggregateDataDO(LocalDateTime aggregateTime, BigDecimal fullValue, BigDecimal incrementalValue, Long standingbookId) {
        MinuteAggregateDataDTO dto = new MinuteAggregateDataDTO();
        dto.setAggregateTime(aggregateTime);
        dto.setFullValue(fullValue);
        dto.setIncrementalValue(incrementalValue);
        dto.setParamCode("");
        dto.setStandingbookId(standingbookId);
        dto.setEnergyFlag(true);

        return dto;
    }
}

