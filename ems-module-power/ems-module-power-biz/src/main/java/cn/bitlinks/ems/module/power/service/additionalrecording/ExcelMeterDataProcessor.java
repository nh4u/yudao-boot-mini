package cn.bitlinks.ems.module.power.service.additionalrecording;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

@Slf4j
@Service
@Validated
public class ExcelMeterDataProcessor {

    @Resource
    private HeaderCodeMappingMapper headerCodeMappingMapper;

    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream(new File("D:/全是文档/燕东/5105.xls"))) {
            ExcelMeterDataProcessor processor = new ExcelMeterDataProcessor();
            List<MinuteAggregateDataDTO> result = processor.process(fis, "A4", "A27", "B3", "S3");

            result.sort(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime));
            result.stream()
                    //.filter(s -> s.getStandingbookId().equals("5105F1-a 水泵 正向有功电能"))
                    .forEach(System.out::println);
            System.out.println("共生成分钟数据条数: " + result.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<MinuteAggregateDataDTO> process(InputStream file, String timeStartCell, String timeEndCell,
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
     * 计算分钟数据 00:00 时间数据不设置增量，整点数据以表中数据为准
     *
     * @param meterValuesMap
     * @param times
     * @return
     */
    private List<MinuteAggregateDataDTO> calculateMinuteDataParallel(Map<String, List<BigDecimal>> meterValuesMap, List<LocalDateTime> times, List<String> meterNames) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(availableProcessors * 2, meterValuesMap.size()));

        List<Future<List<MinuteAggregateDataDTO>>> futures = new ArrayList<>();
        //获取表头与台账关系
        Map<String, HeaderCodeMappingVO> standingbookInfo = getStandingbookInfo(meterNames);

        for (Map.Entry<String, List<BigDecimal>> entry : meterValuesMap.entrySet()) {
            String meter = entry.getKey();
            List<BigDecimal> values = entry.getValue();
            if (MapUtil.isEmpty(standingbookInfo) || standingbookInfo.containsKey(meter)) {
                log.info("暂无报表与台账关联信息，不进行计算, 表头：{}", meter);
                break;
            }
            HeaderCodeMappingVO headerCodeMappingVO = standingbookInfo.get(meter);
            futures.add(executor.submit(() -> {
                List<MinuteAggregateDataDTO> subResult = new ArrayList<>();
                for (int i = 0; i < times.size() - 1; i++) {
                    LocalDateTime start = times.get(i);
                    LocalDateTime end = times.get(i + 1);
                    BigDecimal diff = values.get(i + 1).subtract(values.get(i));
                    long minutes = Duration.between(start, end).toMinutes();
                    if (minutes <= 0) continue;

                    BigDecimal perMinute = BigDecimal.ZERO;
                    if (diff.compareTo(BigDecimal.ZERO) != 0) {
                        perMinute = diff.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);
                    }

                    BigDecimal nowAll = values.get(i);
                    for (int m = 0; m < minutes; m++) {
                        LocalDateTime current = start.plusMinutes(m);

                        if (m == 0 && current.toLocalTime().equals(LocalTime.MIDNIGHT)) {
                            //00:00 时间数据不设置增量
                            MinuteAggregateDataDTO dto = buildMinuteAggregateDataDO(current, values.get(i), BigDecimal.ZERO, headerCodeMappingVO.getStandingbookId());
                            //subResult.add(new MinuteData(meter, current, BigDecimal.ZERO, values.get(i)));
                            subResult.add(dto);
                        } else if (m == 0 && current.getMinute() == 0) {
                            //整点数据以表中数据为准
                            //subResult.add(new MinuteData(meter, current, perMinute, values.get(i)));
                            MinuteAggregateDataDTO dto = buildMinuteAggregateDataDO(current, values.get(i), perMinute, headerCodeMappingVO.getStandingbookId());
                            subResult.add(dto);
                        } else {
                            //其他分钟数据全量相加
                            nowAll = nowAll.add(perMinute);
                            MinuteAggregateDataDTO dto = buildMinuteAggregateDataDO(current, nowAll, perMinute, headerCodeMappingVO.getStandingbookId());
                            //subResult.add(new MinuteData(meter, current, perMinute, nowAll));
                            subResult.add(dto);
                        }
                    }
                }
                return subResult;
            }));
        }

        List<MinuteAggregateDataDTO> result = new ArrayList<>();
        for (Future<List<MinuteAggregateDataDTO>> future : futures) {
            try {
                result.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return result;
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

