package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.enums.FullIncrementEnum;
import cn.bitlinks.ems.framework.common.exception.ServiceException;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
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

import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.STREAM_LOAD_RANGE_FAIL;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.hutool.core.date.DatePattern.NORM_DATETIME_MINUTE_FORMATTER;

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

        // 获取每个采集点 在时间段前后的聚合数据
        //获取表头与台账关系
        LocalDateTime startTime = times.get(0);
        LocalDateTime endTime = times.get(times.size() - 1);
        List<Long> sbIds = Optional.ofNullable(standingbookInfo)
                .orElse(Collections.emptyMap())
                .values().stream()
                .filter(Objects::nonNull)
                .map(HeaderCodeMappingVO::getStandingbookId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        MinuteRangeDataParamDTO paramDTO = new MinuteRangeDataParamDTO();paramDTO.setStarTime(startTime);paramDTO.setEndTime(endTime);paramDTO.setSbIds(sbIds);
        Map<Long, MinuteAggDataSplitDTO> standingboookUsageRangeTimePreNextAggDataMap = minuteAggregateDataApi.getPreAndNextData(paramDTO).getData();

        for (Map.Entry<String, List<BigDecimal>> entry : meterValuesMap.entrySet()) {
            String meter = entry.getKey();
            List<BigDecimal> values = entry.getValue();
            if (MapUtil.isEmpty(standingbookInfo) || !standingbookInfo.containsKey(meter)) {
                failMsgList.add(AcqDataExcelResultVO.builder().acqCode(meter).mistake(IMPORT_ACQ_MISTAKE.getMsg()).mistakeDetail(IMPORT_ACQ_MISTAKE_DETAIL.getMsg()).build());
                log.info("暂无报表与台账关联信息，不进行计算, 表头：{}", meter);
                acqFailCount.addAndGet(values.size());
                continue;
            }
            HeaderCodeMappingVO headerCodeMappingVO = standingbookInfo.get(meter);
            StandingbookTmplDaqAttrDO daqAttrDO =
                    standingbookTmplDaqAttrService.getUsageAttrBySbId(headerCodeMappingVO.getStandingbookId());
            if (Objects.isNull(daqAttrDO)) {
                failMsgList.add(AcqDataExcelResultVO.builder().acqCode(meter).mistake(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS.getMsg()).mistakeDetail(ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS.getMsg()).build());
                log.info("无对应能源用量，不可进行补录, 表头：{}", meter);
                acqFailCount.addAndGet(values.size());
                continue;
            }
            MinuteAggregateDataDTO originalDTO = new MinuteAggregateDataDTO();
            originalDTO.setStandingbookId(headerCodeMappingVO.getStandingbookId());
            originalDTO.setEnergyFlag(daqAttrDO.getEnergyFlag());
            originalDTO.setParamCode(daqAttrDO.getCode());
            originalDTO.setUsage(daqAttrDO.getUsage());
            originalDTO.setDataType(daqAttrDO.getDataType());
            originalDTO.setFullIncrement(FullIncrementEnum.FULL.getCode());
            originalDTO.setDataFeature(daqAttrDO.getDataType());

            futures.add(executor.submit(() -> {
                MinuteAggDataSplitDTO minuteAggDataSplitDTO = standingboookUsageRangeTimePreNextAggDataMap.get(headerCodeMappingVO.getStandingbookId());
                List<AcqDataExcelResultVO> subResult = new ArrayList<>();
                for (int i = 0; i <= times.size() - 1; i++) {
                    LocalDateTime cur = times.get(i);
                    try {
                        MinuteAggregateDataDTO startDataDTO = BeanUtils.toBean(originalDTO, MinuteAggregateDataDTO.class);
                        startDataDTO.setAggregateTime(cur);
                        startDataDTO.setFullValue(values.get(i));
                        startDataDTO.setIncrementalValue(BigDecimal.ZERO);
                        startDataDTO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
                        // 如果是第一个采集点的话，特殊处理，需要更改当前时间对应的增量
                        if (i == times.size() - 1) {
                            // 如果是最后一个采集点的话，需要更改最后一个采集点的下一条原有数据的增量
                            if (minuteAggDataSplitDTO != null && minuteAggDataSplitDTO.getEndDataDO() != null) {
                                MinuteAggregateDataDTO lastDTO = minuteAggDataSplitDTO.getEndDataDO();
                                // 重新设置影响的下一条的增量值
                                lastDTO.setIncrementalValue(lastDTO.getFullValue().subtract(values.get(i)));
                                MinuteAggDataSplitDTO rangDTO = new MinuteAggDataSplitDTO();
                                rangDTO.setStartDataDO(startDataDTO);
                                rangDTO.setEndDataDO(lastDTO);
                                CommonResult<String> result = minuteAggregateDataApi.insertRangeDataError(rangDTO);
                                if (result.isError()) {
                                    subResult.add(AcqDataExcelResultVO.builder().acqCode(meter).acqTime(cur.format(NORM_DATETIME_MINUTE_FORMATTER))
                                            .mistake(result.getMsg()).mistakeDetail(result.getMsg()).build());
                                    acqFailCount.addAndGet(1);
                                    log.error("采集点【{}】,采集时间【{}】,采集数值【{}】1数据解析失败，数据异常{}", meter, cur, values.get(i), result.getMsg());
                                }
                            }
                            // 无最后一条的下一条则不处理，
                        } else {
                            if (i == 0) {
                                if (minuteAggDataSplitDTO != null && minuteAggDataSplitDTO.getStartDataDO() != null) {
                                    MinuteAggregateDataDTO preDTO = minuteAggDataSplitDTO.getStartDataDO();
                                    // 计算第一条数据的增量
                                    startDataDTO.setIncrementalValue(values.get(i).subtract(preDTO.getFullValue()));
                                    MinuteAggDataSplitDTO rangDTO = new MinuteAggDataSplitDTO();
                                    rangDTO.setStartDataDO(preDTO);
                                    rangDTO.setEndDataDO(startDataDTO);
                                    CommonResult<String> result = minuteAggregateDataApi.insertRangeDataError(rangDTO);
                                    handleApiResult(result, subResult, acqFailCount, meter, cur.format(NORM_DATETIME_MINUTE_FORMATTER), values.get(i));
                                }else{
                                    // 无上一条数据，则插入单条
                                    CommonResult<String> result = minuteAggregateDataApi.insertSingleDataError(startDataDTO);
                                    handleApiResult(result, subResult, acqFailCount, meter, cur.format(NORM_DATETIME_MINUTE_FORMATTER), values.get(i));
                                }
                            }
                            MinuteAggDataSplitDTO rangDTO = new MinuteAggDataSplitDTO();
                            rangDTO.setStartDataDO(startDataDTO);
                            MinuteAggregateDataDTO endDataDTO = BeanUtils.toBean(originalDTO, MinuteAggregateDataDTO.class);
                            endDataDTO.setAggregateTime(times.get(i + 1));
                            endDataDTO.setFullValue(values.get(i + 1));
                            //需要计算
                            endDataDTO.setIncrementalValue(null);
                            endDataDTO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
                            rangDTO.setEndDataDO(endDataDTO);
                            CommonResult<String> result = minuteAggregateDataApi.insertRangeDataError(rangDTO);
                            handleApiResult(result, subResult, acqFailCount, meter, cur.format(NORM_DATETIME_MINUTE_FORMATTER), values.get(i));

                        }

                    } catch (ServiceException e) {
                        if (e.getCode().equals(STREAM_LOAD_RANGE_FAIL.getCode())) {
                            subResult.add(AcqDataExcelResultVO.builder().acqCode(meter).acqTime(cur.format(NORM_DATETIME_MINUTE_FORMATTER))
                                    .mistake(IMPORT_DATA_STREAM_LOAD_ERROR.getMsg()).mistakeDetail(IMPORT_DATA_STREAM_LOAD_ERROR.getMsg()).build());
                        } else {
                            subResult.add(AcqDataExcelResultVO.builder().acqCode(meter).acqTime(cur.format(NORM_DATETIME_MINUTE_FORMATTER))
                                    .mistake(e.getMessage()).mistakeDetail(e.getMessage()).build());
                        }
                        acqFailCount.addAndGet(1);
                        log.error("采集点【{}】,采集时间【{}】,采集数值【{}】3数据解析失败，数据异常{}", meter, cur, values.get(i), e.getMessage(), e);
                    } catch (Exception e) {
                        acqFailCount.addAndGet(1);
                        subResult.add(AcqDataExcelResultVO.builder().acqCode(meter).acqTime(cur.format(NORM_DATETIME_MINUTE_FORMATTER))
                                .mistake(IMPORT_ACQ_MISTAKE.getMsg()).mistakeDetail(IMPORT_ACQ_MISTAKE.getMsg()).build());
                        log.error("采集点【{}】,采集时间【{}】,采集数值【{}】4数据解析失败，数据异常{}", meter, cur, values.get(i), e.getMessage(), e);
                    }
                }
                return subResult;
            }));
        }

        for (Future<List<AcqDataExcelResultVO>> future : futures) {
            try {
                failMsgList.addAll(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("线程中断异常", e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause(); // 🟢 获取线程中抛出的真实异常
                if (cause instanceof FeignException) {
                    FeignException fe = (FeignException) cause;
                    String body = fe.contentUTF8();
                    log.error("远程调用失败：{}", body, fe);
                    // 如果你有失败列表，也可以补上一个失败记录
                } else {
                    log.error("线程中执行任务时发生未知异常", cause);
                }
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

