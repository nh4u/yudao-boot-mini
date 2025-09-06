package cn.bitlinks.ems.module.power.service.cophouraggdata;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.*;
import cn.bitlinks.ems.module.power.dal.mysql.copsettings.CopHourAggDataMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import com.alibaba.excel.util.ListUtils;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getSamePeriodLastYear;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.SYSTEM_TYPE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_TYPE_NOT_EXISTS;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.END_TIME_MUST_AFTER_START_TIME;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.COP;

/**
 * @author liumingqiang
 */
@DS("starrocks")
@Slf4j
@Service
@Validated
public class CopHourAggDataServiceImpl implements CopHourAggDataService {

    @Resource
    private CopHourAggDataMapper copHourAggDataMapper;

    @Override
    public CopTableResultVO copTable(ReportParamVO paramVO) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND);

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = LocalDateTimeUtils.beginOfMonth(range[0]);
        LocalDateTime endTime = LocalDateTimeUtils.endOfMonth(range[1]);

        List<String> copyTypeList = paramVO.getCopType();

        // 结果list
        CopTableResultVO resultVO = new CopTableResultVO();
        resultVO.setDataTime(LocalDateTime.now());
        List<Map<String, Object>> result = new ArrayList<>();

        // 3.字段拼接
        List<CopHourAggData> copHourAggDataList = copHourAggDataMapper.getCopHourAggDataList(range[0], range[1], copyTypeList);
        Map<String, List<CopHourAggData>> copHourAggDataMap = copHourAggDataList.stream().collect(Collectors.groupingBy(CopHourAggData::getTime));

        List<String> copTypes = dealSystemType(copyTypeList);

        // 月份最多31天
        for (int i = 1; i <= 31; i++) {
            for (int j = 0; j <= 23; j++) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", i + "日" + j + ":00:00");

                // 月份处理
                LocalDateTime tempStartTime = startTime;
                while (tempStartTime.isBefore(endTime) || tempStartTime.equals(endTime)) {

                    // 拼接时间
                    int monthValue = tempStartTime.getMonthValue();
                    int year = tempStartTime.getYear();
                    try {
                        LocalDateTime currentTime = LocalDateTime.of(year, monthValue, i, j, 0, 0);
                        List<CopHourAggData> copHourAggDatas = copHourAggDataMap.get(currentTime.format(formatter));

                        if (CollUtil.isNotEmpty(copHourAggDatas)) {

                            if (copHourAggDatas.size() == copTypes.size()) {
                                // 相等
                                List<CopHourAggData> sortedCopHourAggDatas = copHourAggDatas.stream()
                                        // 升序
                                        .sorted(Comparator.comparing(CopHourAggData::getCopType))
                                        .collect(Collectors.toList());

                                sortedCopHourAggDatas.forEach(c -> {
                                    BigDecimal copValue = c.getCopValue();
                                    String copType = c.getCopType();
                                    String key = copType + "_" + year + "-" + monthValue;
                                    map.put(key, copValue);
                                });
                            } else {
                                // 不等的情况

                                Map<String, CopHourAggData> copHourAggDatasMap = copHourAggDatas.stream()
                                        .collect(Collectors.toMap(CopHourAggData::getCopType, Function.identity()));

                                copTypes.forEach(c -> {
                                    String key = c + "_" + year + "-" + monthValue;
                                    CopHourAggData copHourAggData = copHourAggDatasMap.get(c);

                                    if (Objects.isNull(copHourAggData)) {
                                        map.put(key, null);
                                    } else {
                                        map.put(key, copHourAggData.getCopValue());
                                    }
                                });
                            }

                        } else {
                            copTypes.forEach(c -> {
                                String key = c + "_" + year + "-" + monthValue;
                                map.put(key, null);
                            });
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                    }


                    tempStartTime = tempStartTime.plusMonths(1);
                }

                result.add(map);
            }


        }

        LocalDateTime lastTime = copHourAggDataMapper.getLastTime(
                range[0],
                range[1],
                copyTypeList);

        resultVO.setCopMapList(result);
        resultVO.setDataTime(lastTime);
        return resultVO;
    }

    @Override
    public List<List<Object>> getExcelData(ReportParamVO paramVO) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND);

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = LocalDateTimeUtils.beginOfMonth(range[0]);
        LocalDateTime endTime = LocalDateTimeUtils.endOfMonth(range[1]);

        List<String> copyTypeList = paramVO.getCopType();

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();

        // 3.字段拼接
        List<CopHourAggData> copHourAggDataList = copHourAggDataMapper.getCopHourAggDataList(range[0], range[1], copyTypeList);
        Map<String, List<CopHourAggData>> copHourAggDataMap = copHourAggDataList.stream().collect(Collectors.groupingBy(CopHourAggData::getTime));

        List<String> copTypes = dealSystemType(copyTypeList);

        // 月份最多31天
        for (int i = 1; i <= 31; i++) {
            for (int j = 0; j <= 23; j++) {
                List<Object> data = ListUtils.newArrayList();
                data.add(i + "日" + j + ":00:00");

                // 月份处理
                LocalDateTime tempStartTime = startTime;
                while (tempStartTime.isBefore(endTime) || tempStartTime.equals(endTime)) {

                    // 拼接时间
                    int monthValue = tempStartTime.getMonthValue();
                    int year = tempStartTime.getYear();
                    try {
                        LocalDateTime currentTime = LocalDateTime.of(year, monthValue, i, j, 0, 0);
                        List<CopHourAggData> copHourAggDatas = copHourAggDataMap.get(currentTime.format(formatter));

                        if (CollUtil.isNotEmpty(copHourAggDatas)) {

                            if (copHourAggDatas.size() == copTypes.size()) {
                                // 相等
                                List<CopHourAggData> sortedCopHourAggDatas = copHourAggDatas.stream()
                                        // 升序
                                        .sorted(Comparator.comparing(CopHourAggData::getCopType))
                                        .collect(Collectors.toList());

                                sortedCopHourAggDatas.forEach(c -> {
                                    BigDecimal copValue = c.getCopValue();
                                    data.add(copValue);
                                });
                            } else {
                                // 不等的情况
                                Map<String, CopHourAggData> copHourAggDatasMap = copHourAggDatas.stream()
                                        .collect(Collectors.toMap(CopHourAggData::getCopType, Function.identity()));

                                copTypes.forEach(c -> {
                                    String key = c + "_" + year + "-" + monthValue;
                                    CopHourAggData copHourAggData = copHourAggDatasMap.get(c);

                                    if (Objects.isNull(copHourAggData)) {
                                        data.add("/");
                                    } else {
                                        data.add(copHourAggData.getCopValue());
                                    }
                                });
                            }

                        } else {
                            copTypes.forEach(c -> {
                                data.add("/");
                            });
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        copTypes.forEach(c -> {
                            data.add("/");
                        });
                    }

                    tempStartTime = tempStartTime.plusMonths(1);
                }
                result.add(data);
            }
        }

        return result;
    }

    @Override
    public List<List<String>> getExcelHeader(ReportParamVO paramVO) {
        Map<String, String> map = new HashMap<>();
        map.put("LTC", "低温冷机");
        map.put("LTS", "低温系统");
        map.put("MTC", "中温冷机");
        map.put("MTS", "中温系统");

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = LocalDateTimeUtils.beginOfMonth(range[0]);
        LocalDateTime endTime = LocalDateTimeUtils.endOfMonth(range[1]);

        List<String> copyTypeList = paramVO.getCopType();
        List<String> copTypes = dealSystemType(copyTypeList);

        List<List<String>> list = ListUtils.newArrayList();

        // 表单名称
        String sheetName = COP;
        // 系统
        String systemStr = copTypes.stream().map(map::get).collect(Collectors.joining("、"));
        // 统计周期
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);
        // 第一格处理
        list.add(Arrays.asList("表单名称", "系统", "统计周期", "时间/系统", "时间/系统"));

        // 月份处理
        DataTypeEnum dataTypeEnum = validateDateType(1);
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> {
            copTypes.forEach(c -> {
                list.add(Arrays.asList(sheetName, systemStr, strTime, x, map.get(c)));
            });
        });

        return list;
    }

    @Override
    public CopChartResultVO copChart(ReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());

        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];
        // 去年同期
        LocalDateTime startTimePre = startTime.minusYears(1);
        LocalDateTime endTimePre = endTime.minusYears(1);

        // 2. 检验查询时间类型
        Integer dateType = paramVO.getDateType();
        DataTypeEnum dataTypeEnum = validateDateType(dateType);

        List<String> copyTypeList = paramVO.getCopType();

        CopChartResultVO resultVO = new CopChartResultVO();

        // 处理x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);
        resultVO.setXdata(xdata);

        // 3.字段拼接
        // 当下cop数据
        List<CopHourAggData> nowCopAggDataList;
        if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
            nowCopAggDataList = copHourAggDataMapper.getCopDayAggDataList(startTime, endTime, copyTypeList);
        } else {
            nowCopAggDataList = copHourAggDataMapper.getCopHourAggDataList(startTime, endTime, copyTypeList);
        }
        Map<String, List<CopHourAggData>> nowCopAggDataMap = nowCopAggDataList.stream().collect(Collectors.groupingBy(CopHourAggData::getTime));


        // 去年同期cop数据
        List<CopHourAggData> preCopAggDataList;
        if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
            preCopAggDataList = copHourAggDataMapper.getCopDayAggDataList(startTimePre, endTimePre, copyTypeList);
        } else {
            preCopAggDataList = copHourAggDataMapper.getCopHourAggDataList(startTimePre, endTimePre, copyTypeList);
        }
        Map<String, List<CopHourAggData>> preCopAggDataMap = preCopAggDataList.stream().collect(Collectors.groupingBy(CopHourAggData::getTime));

        List<String> copTypes = dealSystemType(copyTypeList);

        List<BigDecimal> ltcNow = new ArrayList<>();
        List<BigDecimal> ltsNow = new ArrayList<>();
        List<BigDecimal> mtcNow = new ArrayList<>();
        List<BigDecimal> mtsNow = new ArrayList<>();
        List<BigDecimal> ltcPre = new ArrayList<>();
        List<BigDecimal> ltsPre = new ArrayList<>();
        List<BigDecimal> mtcPre = new ArrayList<>();
        List<BigDecimal> mtsPre = new ArrayList<>();


        xdata.forEach(x -> {
            List<CopHourAggData> nowCopAggDatas = nowCopAggDataMap.get(x);

            // 时间处理 x是当前年份，所以年份要减去1 即： 2025-06-09 01:00:00 => 2024-06-09 01:00:00
            String samePeriodLastYear = getSamePeriodLastYear(x, DatePattern.NORM_DATETIME_PATTERN);
            List<CopHourAggData> preCopAggDatas = preCopAggDataMap.get(samePeriodLastYear);
            // 当前
            dealYList(nowCopAggDatas, copTypes, ltcNow, ltsNow, mtcNow, mtsNow);
            // 去年同期
            dealYList(preCopAggDatas, copTypes, ltcPre, ltsPre, mtcPre, mtsPre);
        });


        CopChartYData copChartYData = new CopChartYData();

        // LTC
        if (CollUtil.isNotEmpty(ltcNow)) {
            copChartYData.setLtcNow(ltcNow);
        }
        if (CollUtil.isNotEmpty(ltcPre)) {
            copChartYData.setLtcPre(ltcPre);
        }

        // LTS
        if (CollUtil.isNotEmpty(ltsNow)) {
            copChartYData.setLtsNow(ltsNow);
        }
        if (CollUtil.isNotEmpty(ltsPre)) {
            copChartYData.setLtsPre(ltsPre);
        }

        // MTC
        if (CollUtil.isNotEmpty(mtcNow)) {
            copChartYData.setMtcNow(mtcNow);
        }
        if (CollUtil.isNotEmpty(mtcPre)) {
            copChartYData.setMtcPre(mtcPre);
        }

        // MTS
        if (CollUtil.isNotEmpty(mtsNow)) {
            copChartYData.setMtsNow(mtsNow);
        }
        if (CollUtil.isNotEmpty(mtsPre)) {
            copChartYData.setMtsPre(mtsPre);
        }

        resultVO.setYdata(copChartYData);
        return resultVO;
    }

    @Override
    public CopChartResultVO copChartForBigScreen(ReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());

        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];
        // 2. 检验查询时间类型
        Integer dateType = paramVO.getDateType();
        DataTypeEnum dataTypeEnum = validateDateType(dateType);

        List<String> copyTypeList = paramVO.getCopType();

        CopChartResultVO resultVO = new CopChartResultVO();

        // 处理x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);
        resultVO.setXdata(xdata);

        // 3.字段拼接
        // 当下cop数据
        List<CopHourAggData> nowCopAggDataList;
        if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
            nowCopAggDataList = copHourAggDataMapper.getCopDayAggDataList(startTime, endTime, copyTypeList);
        } else {
            nowCopAggDataList = copHourAggDataMapper.getCopHourAggDataList(startTime, endTime, copyTypeList);
        }
        Map<String, List<CopHourAggData>> nowCopAggDataMap = nowCopAggDataList.stream().collect(Collectors.groupingBy(CopHourAggData::getTime));

        List<String> copTypes = dealSystemType(copyTypeList);

        List<BigDecimal> ltcNow = new ArrayList<>();
        List<BigDecimal> ltsNow = new ArrayList<>();
        List<BigDecimal> mtcNow = new ArrayList<>();
        List<BigDecimal> mtsNow = new ArrayList<>();

        xdata.forEach(x -> {
            List<CopHourAggData> nowCopAggDatas = nowCopAggDataMap.get(x);
            // 当前
            dealYList(nowCopAggDatas, copTypes, ltcNow, ltsNow, mtcNow, mtsNow);
        });

        CopChartYData copChartYData = new CopChartYData();

        // LTC
        if (CollUtil.isNotEmpty(ltcNow)) {
            copChartYData.setLtcNow(ltcNow);
        }
        // LTS
        if (CollUtil.isNotEmpty(ltsNow)) {
            copChartYData.setLtsNow(ltsNow);
        }
        // MTC
        if (CollUtil.isNotEmpty(mtcNow)) {
            copChartYData.setMtcNow(mtcNow);
        }
        // MTS
        if (CollUtil.isNotEmpty(mtsNow)) {
            copChartYData.setMtsNow(mtsNow);
        }

        resultVO.setYdata(copChartYData);
        return resultVO;
    }

    /**
     * 处理Y轴数据
     *
     * @param nowCopAggDatas
     * @param copTypes
     * @param ltcNow
     * @param ltsNow
     * @param mtcNow
     * @param mtsNow
     */
    private void dealYList(List<CopHourAggData> nowCopAggDatas,
                           List<String> copTypes,
                           List<BigDecimal> ltcNow,
                           List<BigDecimal> ltsNow,
                           List<BigDecimal> mtcNow,
                           List<BigDecimal> mtsNow) {

        if (CollUtil.isNotEmpty(nowCopAggDatas)) {
            Map<String, CopHourAggData> map = nowCopAggDatas.stream()
                    .collect(Collectors.toMap(CopHourAggData::getCopType, Function.identity()));
            for (String copType : copTypes) {
                BigDecimal value = dealCopValue(map.get(copType));
                switch (copType) {
                    case "LTC":
                        ltcNow.add(value);
                        break;
                    case "LTS":
                        ltsNow.add(value);
                        break;
                    case "MTC":
                        mtcNow.add(value);
                        break;
                    case "MTS":
                        mtsNow.add(value);
                        break;
                    default:
                }
            }
        } else {
            for (String copType : copTypes) {
                BigDecimal value = BigDecimal.ZERO;
                switch (copType) {
                    case "LTC":
                        ltcNow.add(value);
                        break;
                    case "LTS":
                        ltsNow.add(value);
                        break;
                    case "MTC":
                        mtcNow.add(value);
                        break;
                    case "MTS":
                        mtsNow.add(value);
                        break;
                    default:
                }
            }
        }
    }

    private BigDecimal dealCopValue(CopHourAggData copHourAggData) {
        if (!Objects.isNull(copHourAggData)) {
            return copHourAggData.getCopValue();
        } else {
            return BigDecimal.ZERO;
        }
    }


    /**
     * 校验时间范围
     *
     * @param rangeOrigin
     * @return
     */
    private LocalDateTime[] validateRange(LocalDateTime[] rangeOrigin) {
        // 1.校验时间范围
        // 1.1.校验结束时间必须大于开始时间
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
//        // 时间不能相差1年
//        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
//            throw exception(DATE_RANGE_EXCEED_LIMIT);
//        }

        return rangeOrigin;
    }

    /**
     * 校验系统类型
     *
     * @param systemType
     */
    private List<String> dealSystemType(List<String> systemType) {

        // 时间类型不存在
        if (CollUtil.isEmpty(systemType)) {

            // 获取全系统类型
            systemType = DictFrameworkUtils.getDictDataLabelList(SYSTEM_TYPE);
        }

        return systemType.stream().sorted().collect(Collectors.toList());
    }

    /**
     * 校验时间类型
     *
     * @param dateType
     */
    private DataTypeEnum validateDateType(Integer dateType) {
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(dateType);
        // 时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        return dataTypeEnum;
    }
}
