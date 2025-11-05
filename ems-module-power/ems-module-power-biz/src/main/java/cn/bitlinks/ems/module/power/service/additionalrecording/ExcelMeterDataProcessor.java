package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.enums.FullIncrementEnum;
import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.calc.AggSplitUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataFiveMinuteApi;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelCoordinate;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelListResultVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelResultVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingBookHeaderDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookServiceImpl;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.starrocks.StarRocksBatchImportService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
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
import java.util.stream.IntStream;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

@Slf4j
@Service
@Validated
public class ExcelMeterDataProcessor {


    @Resource
    private MinuteAggregateDataApi minuteAggregateDataApi;

    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    @Resource
    private AdditionalRecordingService additionalRecordingService;
    @Resource
    private SplitTaskDispatcher splitTaskDispatcher;
    @Autowired
    private StandingbookServiceImpl standingbookService;
    @Resource
    private MinuteAggregateDataFiveMinuteApi minuteAggregateDataFiveMinuteApi;

    @Resource
    private StarRocksBatchImportService starRocksBatchImportService;
    public AcqDataExcelListResultVO process(InputStream file, String timeStartCell, String timeEndCell,
                                            String meterStartCell, String meterEndCell,Boolean acqFlag) throws IOException {

        // 单元格处理
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
            if (CollUtil.isEmpty(times)) {
                throw exception(IMPORT_NO_TIMES);
            }
            boolean timeError;
            if (timeVertical && meterHorizontal) {
                timeError = timeStart[0] + times.size() - 1 != timeEnd[0];
            } else {
                timeError = timeStart[1] + times.size() - 1 != timeEnd[1];
            }
            if (timeError) {
                throw exception(IMPORT_TIMES_ERROR);
            }
            if (CollUtil.isEmpty(meterValuesMap)) {
                throw exception(IMPORT_NO_METER);
            }
            return calculateMinuteDataParallel(meterValuesMap, times, meterNames, acqFlag);
        } catch (ServiceException e) {
            ErrorCode errorCode = new ErrorCode(1_001_000_000, e.getMessage());
            throw exception(errorCode);
        }
    }


    public AcqDataExcelCoordinate getExcelImportCoordinate(InputStream file) throws IOException {

        AcqDataExcelCoordinate acqDataExcelCoordinate = new AcqDataExcelCoordinate();

        try (Workbook workbook = WorkbookFactory.create(file)) {
            //只判断一个sheet页的数据
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw exception(IMPORT_EXCEL_SHEET_NOT_EXIST);
            }

            Row row = sheet.getRow(0);
            if (Objects.isNull(row)) {
                throw exception(IMPORT_EXCEL_FORMAT_ERROR);
            }
            Cell cell = row.getCell(0);
            if (Objects.isNull(cell)) {
                throw exception(IMPORT_EXCEL_FORMAT_ERROR);
            }
            String stringCellValue = cell.getStringCellValue();
            if (!"时间\\数据".equals(stringCellValue)) {
                throw exception(IMPORT_EXCEL_FORMAT_ERROR);
            }

            // ===== 1. 获取第一行（row 0）最后一个非空单元格的列索引 =====
            int lastColIndex = getLastNonEmptyCellColumnIndex(row);
            // ===== 2. 获取第一列（column 0）最后一个非空单元格的行索引 =====
            int lastRowIndex = getLastNonEmptyCellRowIndex(sheet, 0);

            // 如果你想返回 Excel 风格的坐标，比如 "K26"，可以进一步转换：
            String acqNameStart = getExcelCoordinate(0, 1);
            acqDataExcelCoordinate.setAcqNameStart(acqNameStart);

            lastColIndex = lastColIndex > 0 ? lastColIndex : 1;
            String acqNameEnd = getExcelCoordinate(0, lastColIndex);
            acqDataExcelCoordinate.setAcqNameEnd(acqNameEnd);

            String acqTimeStart = getExcelCoordinate(1, 0);
            acqDataExcelCoordinate.setAcqTimeStart(acqTimeStart);

            lastRowIndex = lastRowIndex > 0 ? lastRowIndex : 1;
            String acqTimeEnd = getExcelCoordinate(lastRowIndex, 0);
            acqDataExcelCoordinate.setAcqTimeEnd(acqTimeEnd);

        } catch (ServiceException e) {
            ErrorCode errorCode = new ErrorCode(1_001_000_000, e.getMessage());
            throw exception(errorCode);
        }
        return acqDataExcelCoordinate;
    }

    /**
     * 获取某一行（Row）中，最后一个非空单元格的 列索引（从0开始）
     */
    private int getLastNonEmptyCellColumnIndex(Row row) {
        if (row == null) {
            return -1;
        }

        int lastColIndex = -1;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (isCellNonEmpty(cell)) {
                lastColIndex = i;
            }
        }
        return lastColIndex;
    }

    /**
     * 获取某一列（Column）中，最后一个非空单元格的 行索引（从0开始）
     */
    private int getLastNonEmptyCellRowIndex(Sheet sheet, int columnIndex) {
        if (sheet == null) {
            return -1;
        }

        int lastRowIndex = -1;
        for (int rowNum = 0; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Cell cell = row.getCell(columnIndex);
                if (isCellNonEmpty(cell)) {
                    lastRowIndex = rowNum;
                }
            }
        }
        return lastRowIndex;
    }

    /**
     * 判断单元格是否非空：非null，且内容有值（非空字符串/数字等）
     */
    private boolean isCellNonEmpty(Cell cell) {
        if (cell == null) {
            return false;
        }
        switch (cell.getCellType()) {
            case BLANK:
                return false;
            case STRING:
                return !cell.getStringCellValue().trim().isEmpty();
            case NUMERIC:
            case BOOLEAN:
            case FORMULA:
                return true; // 公式、数字、布尔值都算非空
            default:
                return false;
        }
    }

    /**
     * 根据行号和列号（从 0 开始的数字索引）生成 Excel 风格的坐标，如 A1, B2, AA26
     *
     * @param row    行号，从 0 开始（比如 0 表示第 1 行）
     * @param column 列号，从 0 开始（比如 0 表示第 1 列/A）
     * @return Excel 格式的坐标，如 "A1", "K26"
     */
    private String getExcelCoordinate(int row, int column) {
        String colLetter = indexToExcelColumn(column); // 如 0 -> A
        int excelRowNumber = row + 1; // 如 0 -> 1
        return colLetter + excelRowNumber;
    }

    /**
     * 可选：将列的数字索引转换为 Excel 列字母，如 0 -> A, 1 -> B, ..., 10 -> K
     */
    private String indexToExcelColumn(int columnIndex) {
        StringBuilder sb = new StringBuilder();
        while (columnIndex >= 0) {
            int rem = columnIndex % 26;
            sb.append((char) ('A' + rem));
            columnIndex = (columnIndex / 26) - 1;
        }
        return sb.reverse().toString();
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
     * 计算分钟级别数据
     *
     * @param meterValuesMap
     * @param times
     * @return
     */
    private AcqDataExcelListResultVO calculateMinuteDataParallel(Map<String, List<BigDecimal>> meterValuesMap, List<LocalDateTime> times, List<String> meterNames, Boolean acqFlag) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(availableProcessors * 2, meterValuesMap.size()));
        AcqDataExcelListResultVO resultVO = new AcqDataExcelListResultVO();
        List<AcqDataExcelResultVO> failMsgList = new ArrayList<>();
        AtomicInteger acqFailCount = new AtomicInteger();
        List<Future<Pair<List<MinuteAggregateDataDTO>, List<MinuteAggDataSplitDTO>>>> futures = new ArrayList<>();

        Map<String, StandingBookHeaderDTO> standingbookInfo = getStandingbookInfo(meterNames);
        if (CollUtil.isEmpty(standingbookInfo)) {
            meterNames.forEach(meter -> {
                failMsgList.add(AcqDataExcelResultVO.builder().acqCode(meter).mistake(IMPORT_ACQ_MISTAKE.getMsg()).mistakeDetail(IMPORT_ACQ_MISTAKE_DETAIL.getMsg()).build());
            });
            resultVO.setFailList(failMsgList);
            resultVO.setFailAcqTotal(meterNames.size());
            return resultVO;
        }

        LocalDateTime startTime = times.get(0);
        LocalDateTime endTime = times.get(times.size() - 1);
        List<Long> sbIds = standingbookInfo.values().stream()
                .filter(Objects::nonNull)
                .map(StandingBookHeaderDTO::getStandingbookId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        MinuteRangeDataParamDTO paramDTO = new MinuteRangeDataParamDTO();
        paramDTO.setStarTime(startTime);
        paramDTO.setEndTime(endTime);
        paramDTO.setSbIds(sbIds);
        Map<Long, MinuteAggDataSplitDTO> standingboookUsageRangeTimePreNextAggDataMap = minuteAggregateDataApi.getPreAndNextData(paramDTO).getData();

        for (Map.Entry<String, List<BigDecimal>> entry : meterValuesMap.entrySet()) {
            String meter = entry.getKey();
            List<BigDecimal> values = entry.getValue();


            if (MapUtil.isEmpty(standingbookInfo) || !standingbookInfo.containsKey(meter) || Objects.isNull(standingbookInfo.get(meter).getStandingbookId())) {
                failMsgList.add(AcqDataExcelResultVO.builder().acqCode(meter).mistake(IMPORT_ACQ_MISTAKE.getMsg()).mistakeDetail(IMPORT_ACQ_MISTAKE_DETAIL.getMsg()).build());
                acqFailCount.addAndGet(values.size());
                continue;
            }

            StandingBookHeaderDTO standingBookHeaderDTO = standingbookInfo.get(meter);
            StandingbookTmplDaqAttrDO daqAttrDO = standingbookTmplDaqAttrService.getUsageAttrBySbId(standingBookHeaderDTO.getStandingbookId());
            if (Objects.isNull(daqAttrDO)) {
                failMsgList.add(AcqDataExcelResultVO.builder().acqCode(meter).mistake(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS.getMsg()).mistakeDetail(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS.getMsg()).build());
                log.info("无对应能源用量，不可进行补录, 表头：{}", meter);
                acqFailCount.addAndGet(values.size());
                continue;
            }

            MinuteAggregateDataDTO baseDTO = new MinuteAggregateDataDTO();
            baseDTO.setStandingbookId(standingBookHeaderDTO.getStandingbookId());
            baseDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
            baseDTO.setParamCode(daqAttrDO.getCode());
            baseDTO.setUsage(daqAttrDO.getUsage());
            baseDTO.setDataType(daqAttrDO.getDataType());
            baseDTO.setFullIncrement(FullIncrementEnum.FULL.getCode());
            baseDTO.setDataFeature(daqAttrDO.getDataType());
            baseDTO.setAcqFlag(AcqFlagEnum.ACQ.getCode());

            futures.add(executor.submit(() -> {
                // standingboookUsageRangeTimePreNextAggDataMap可能是空，因此需要判空
                MinuteAggDataSplitDTO minuteAggDataSplitDTO = CollUtil.isNotEmpty(standingboookUsageRangeTimePreNextAggDataMap) ? standingboookUsageRangeTimePreNextAggDataMap.get(standingBookHeaderDTO.getStandingbookId()) : null;
                List<MinuteAggregateDataDTO> toAddAcqDataList = new ArrayList<>();
                List<MinuteAggDataSplitDTO> toAddNotAcqSplitDataList = new ArrayList<>();
                // 1. 收集非 null 下标
                List<Integer> idx = IntStream.range(0, values.size())
                        .filter(i -> values.get(i) != null)
                        .boxed()
                        .sorted(Comparator.comparing(times::get)) // 按时间升序
                        .collect(Collectors.toList());

                // 2. 生成两个新的、已排序且 value 非 null 的列表
                List<BigDecimal> newValues = idx.stream().map(values::get).collect(Collectors.toList());
                List<LocalDateTime> newTimes = idx.stream().map(times::get).collect(Collectors.toList());
                for (int i = 0; i < newTimes.size(); i++) {
                    LocalDateTime curTime = newTimes.get(i);
                    MinuteAggregateDataDTO curDTO = BeanUtils.toBean(baseDTO, MinuteAggregateDataDTO.class);
                    curDTO.setAggregateTime(curTime);
                    curDTO.setFullValue(newValues.get(i));

                    if (i == 0) {
                        if (minuteAggDataSplitDTO != null && minuteAggDataSplitDTO.getStartDataDO() != null) {
                            MinuteAggregateDataDTO preDTO = minuteAggDataSplitDTO.getStartDataDO();
                            BigDecimal incr = AggSplitUtils.calculatePerMinuteIncrement(preDTO.getAggregateTime(), curDTO.getAggregateTime(), preDTO.getFullValue(), curDTO.getFullValue());
                            curDTO.setIncrementalValue(incr.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : incr);
                            toAddAcqDataList.add(curDTO);
                            toAddNotAcqSplitDataList.add(new MinuteAggDataSplitDTO(preDTO, curDTO));
                        } else {
                            curDTO.setIncrementalValue(BigDecimal.ZERO);
                            toAddAcqDataList.add(curDTO);
                        }
                    } else if (i == newTimes.size() - 1) {
                        MinuteAggregateDataDTO preDTO = toAddAcqDataList.get(i - 1);
                        BigDecimal incr = AggSplitUtils.calculatePerMinuteIncrement(newTimes.get(i - 1), curTime, newValues.get(i - 1), newValues.get(i));
                        curDTO.setIncrementalValue(incr.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : incr);
                        toAddAcqDataList.add(curDTO);
                        toAddNotAcqSplitDataList.add(new MinuteAggDataSplitDTO(preDTO, curDTO));

                        if (minuteAggDataSplitDTO != null && minuteAggDataSplitDTO.getEndDataDO() != null) {
                            MinuteAggregateDataDTO lastDTO = minuteAggDataSplitDTO.getEndDataDO();
                            BigDecimal incr2 = AggSplitUtils.calculatePerMinuteIncrement(curDTO.getAggregateTime(), lastDTO.getAggregateTime(), curDTO.getFullValue(), lastDTO.getFullValue());
                            lastDTO.setIncrementalValue(incr2.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : incr);
                            toAddAcqDataList.add(lastDTO);
                            toAddNotAcqSplitDataList.add(new MinuteAggDataSplitDTO(curDTO, lastDTO));
                        }
                    } else {
                        MinuteAggregateDataDTO preDTO = toAddAcqDataList.get(i - 1);
                        BigDecimal incr = AggSplitUtils.calculatePerMinuteIncrement(newTimes.get(i - 1), curTime, newValues.get(i - 1), newValues.get(i));
                        curDTO.setIncrementalValue(incr.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : incr);
                        toAddAcqDataList.add(curDTO);
                        toAddNotAcqSplitDataList.add(new MinuteAggDataSplitDTO(preDTO, curDTO));
                    }
                }

                return Pair.of(toAddAcqDataList, toAddNotAcqSplitDataList);
            }));
        }

        List<MinuteAggregateDataDTO> toAddAllAcqList = new ArrayList<>();
        List<MinuteAggDataSplitDTO> toAddAllNotAcqSplitList = new ArrayList<>();

        for (Future<Pair<List<MinuteAggregateDataDTO>, List<MinuteAggDataSplitDTO>>> future : futures) {
            try {
                Pair<List<MinuteAggregateDataDTO>, List<MinuteAggDataSplitDTO>> pair = future.get();
                toAddAllAcqList.addAll(pair.getFirst());
                toAddAllNotAcqSplitList.addAll(pair.getSecond());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("线程中断异常", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof FeignException) {
                    FeignException fe = (FeignException) cause;
                    String body = fe.contentUTF8();
                    log.error("远程调用失败：{}", body, fe);
                } else {
                    log.error("线程中执行任务时发生未知异常", cause);
                }
            }
        }

        executor.shutdown();

        // 添加业务点手动写入
        if (acqFlag) {
            starRocksBatchImportService.addDataToQueue(toAddAllAcqList);
            additionalRecordingService.saveAdditionalRecordingBatch(toAddAllAcqList);
            resultVO.setFailList(failMsgList);
            resultVO.setFailAcqTotal(acqFailCount.get());
            return resultVO;
        }
        starRocksBatchImportService.addDataToQueue(toAddAllAcqList);
        additionalRecordingService.saveAdditionalRecordingBatch(toAddAllAcqList);
        splitTaskDispatcher.dispatchSplitTaskBatch(toAddAllNotAcqSplitList);

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


    private void handleApiResult(CommonResult<?> result, List<AcqDataExcelResultVO> subResult, AtomicInteger acqFailCount, String meter, String time, BigDecimal value) {
        if (result.isError()) {
            subResult.add(AcqDataExcelResultVO.builder()
                    .acqCode(meter)
                    .acqTime(time)
                    .mistake(result.getMsg())
                    .mistakeDetail(result.getMsg())
                    .build());
            acqFailCount.incrementAndGet();
            log.error("采集点【{}】,采集时间【{}】,采集数值【{}】远程调用失败：{}", meter, time, value, result.getMsg());
        }
    }

    /**
     * 转为数值 不能转为数值的按0处理
     *
     * @param cell
     * @return
     */
    private BigDecimal getNumericValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC)
            return BigDecimal.valueOf(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                log.error("{} not a number", cell.getStringCellValue());
                return null;
            }
        }
        return null;
    }

    private Map<String, StandingBookHeaderDTO> getStandingbookInfo(List<String> headList) {
        List<StandingBookHeaderDTO> standingBookHeaderDTOS = standingbookService.getStandingBookHeadersByHeaders(headList);
        if (CollUtil.isEmpty(standingBookHeaderDTOS)) {
            return null;
        }
        return standingBookHeaderDTOS.stream().collect(Collectors.toMap(StandingBookHeaderDTO::getHeader, Function.identity()));
    }
}

