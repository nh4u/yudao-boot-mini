package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.enums.FullIncrementEnum;
import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.calc.AggSplitUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataParamDTO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelListResultVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.AcqDataExcelResultVO;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.HeaderCodeMappingVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.reportcod.HeaderCodeMappingMapper;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
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

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.STREAM_LOAD_RANGE_FAIL;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

@Slf4j
@Service
@Validated
public class ExcelMeterDataProcessor {

    @Resource
    private HeaderCodeMappingMapper headerCodeMappingMapper;

    @Resource
    private MinuteAggregateDataApi minuteAggregateDataApi;

    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    @Resource
    private AdditionalRecordingService additionalRecordingService;
    @Resource
    private SplitTaskDispatcher splitTaskDispatcher;

    public static void main(String[] args) {
        try (FileInputStream fis = new FileInputStream(new File("D:/工作文件/燕东/51051.xls"))) {
            ExcelMeterDataProcessorV0 processor = new ExcelMeterDataProcessorV0();
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
     * 计算分钟级别数据
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
        List<Future<Pair<List<MinuteAggregateDataDTO>, List<MinuteAggDataSplitDTO>>>> futures = new ArrayList<>();

        Map<String, HeaderCodeMappingVO> standingbookInfo = getStandingbookInfo(meterNames);
        if (CollUtil.isEmpty(standingbookInfo)) {
            log.warn("暂无报表与台账关联信息，不进行计算");
            throw exception(IMPORT_NO_MAPPING);
        }

        LocalDateTime startTime = times.get(0);
        LocalDateTime endTime = times.get(times.size() - 1);
        List<Long> sbIds = standingbookInfo.values().stream()
                .filter(Objects::nonNull)
                .map(HeaderCodeMappingVO::getStandingbookId)
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
                log.info("暂无报表与台账关联信息，不进行计算, 表头：{}", meter);
                acqFailCount.addAndGet(values.size());
                continue;
            }

            HeaderCodeMappingVO headerCodeMappingVO = standingbookInfo.get(meter);
            StandingbookTmplDaqAttrDO daqAttrDO = standingbookTmplDaqAttrService.getUsageAttrBySbId(headerCodeMappingVO.getStandingbookId());
            if (Objects.isNull(daqAttrDO)) {
                failMsgList.add(AcqDataExcelResultVO.builder().acqCode(meter).mistake(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS.getMsg()).mistakeDetail(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS.getMsg()).build());
                log.info("无对应能源用量，不可进行补录, 表头：{}", meter);
                acqFailCount.addAndGet(values.size());
                continue;
            }

            MinuteAggregateDataDTO baseDTO = new MinuteAggregateDataDTO();
            baseDTO.setStandingbookId(headerCodeMappingVO.getStandingbookId());
            baseDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
            baseDTO.setParamCode(daqAttrDO.getCode());
            baseDTO.setUsage(daqAttrDO.getUsage());
            baseDTO.setDataType(daqAttrDO.getDataType());
            baseDTO.setFullIncrement(FullIncrementEnum.FULL.getCode());
            baseDTO.setDataFeature(daqAttrDO.getDataType());
            baseDTO.setAcqFlag(AcqFlagEnum.ACQ.getCode());

            futures.add(executor.submit(() -> {
                MinuteAggDataSplitDTO minuteAggDataSplitDTO = standingboookUsageRangeTimePreNextAggDataMap.get(headerCodeMappingVO.getStandingbookId());
                List<MinuteAggregateDataDTO> toAddAcqDataList = new ArrayList<>();
                List<MinuteAggDataSplitDTO> toAddNotAcqSplitDataList = new ArrayList<>();

                for (int i = 0; i < times.size(); i++) {
                    LocalDateTime curTime = times.get(i);
                    MinuteAggregateDataDTO curDTO = BeanUtils.toBean(baseDTO, MinuteAggregateDataDTO.class);
                    curDTO.setAggregateTime(curTime);
                    curDTO.setFullValue(values.get(i));

                    if (i == 0) {
                        if (minuteAggDataSplitDTO != null && minuteAggDataSplitDTO.getStartDataDO() != null) {
                            MinuteAggregateDataDTO preDTO = minuteAggDataSplitDTO.getStartDataDO();
                            curDTO.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(preDTO.getAggregateTime(), curDTO.getAggregateTime(), preDTO.getFullValue(), curDTO.getFullValue()));
                            toAddAcqDataList.add(curDTO);
                            toAddNotAcqSplitDataList.add(new MinuteAggDataSplitDTO(preDTO, curDTO));
                        } else {
                            curDTO.setIncrementalValue(BigDecimal.ZERO);
                            toAddAcqDataList.add(curDTO);
                        }
                    } else if (i == times.size() - 1) {
                        MinuteAggregateDataDTO preDTO = toAddAcqDataList.get(i - 1);
                        curDTO.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(times.get(i - 1), curTime, values.get(i - 1), values.get(i)));
                        toAddAcqDataList.add(curDTO);
                        toAddNotAcqSplitDataList.add(new MinuteAggDataSplitDTO(preDTO, curDTO));

                        if (minuteAggDataSplitDTO != null && minuteAggDataSplitDTO.getEndDataDO() != null) {
                            MinuteAggregateDataDTO lastDTO = minuteAggDataSplitDTO.getEndDataDO();
                            lastDTO.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(curDTO.getAggregateTime(), lastDTO.getAggregateTime(), curDTO.getFullValue(), lastDTO.getFullValue()));
                            toAddAcqDataList.add(lastDTO);
                            toAddNotAcqSplitDataList.add(new MinuteAggDataSplitDTO(curDTO, lastDTO));
                        }
                    } else {
                        MinuteAggregateDataDTO preDTO = toAddAcqDataList.get(i - 1);
                        curDTO.setIncrementalValue(AggSplitUtils.calculatePerMinuteIncrement(times.get(i - 1), curTime, values.get(i - 1), values.get(i)));
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

        minuteAggregateDataApi.insertDataBatch(toAddAllAcqList);
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
//
//    /**
//     * 计算最后一分钟的增量
//     *
//     * @param start
//     * @param end
//     * @param startFull
//     * @param endFull
//     * @return
//     */
//    private BigDecimal calcEndMinuteIncrementValue(LocalDateTime start, LocalDateTime end, BigDecimal startFull, BigDecimal endFull) {
//        long minutes = Duration.between(start, end).toMinutes();
//        if (minutes <= 0) {
//            return BigDecimal.ZERO;
//        }
//
//        // 平均每分钟的增量
//        BigDecimal totalIncrement = endFull.subtract(startFull);
//        return totalIncrement.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);
//    }

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

