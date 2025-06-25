package cn.bitlinks.ems.module.power.service.cophouraggdata;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartYData;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopHourAggData;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.dal.mysql.copsettings.CopHourAggDataMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.hutool.core.collection.CollectionUtil;
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
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.SYSTEM_TYPE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

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
    public List<Map<String, Object>> copTable(ReportParamVO paramVO) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND);

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = LocalDateTimeUtils.beginOfMonth(range[0]);
        LocalDateTime endTime = LocalDateTimeUtils.endOfMonth(range[1]);

        List<String> copyTypeList = paramVO.getCopType();

        // 结果list
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
                    try {
                        LocalDateTime currentTime = LocalDateTime.of(tempStartTime.getYear(), monthValue, i, j, 0, 0);
                        List<CopHourAggData> copHourAggDatas = copHourAggDataMap.get(currentTime.format(formatter));

                        if (CollectionUtil.isNotEmpty(copHourAggDatas)) {
                            copHourAggDatas.forEach(c -> {
                                BigDecimal copValue = c.getCopValue();
                                String copType = c.getCopType();
                                String key = copType + "-" + monthValue;
                                map.put(key, copValue);
                            });
                        } else {
                            copTypes.forEach(c -> {
                                String key = c + "-" + monthValue;
                                map.put(key, BigDecimal.ZERO);
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


        return result;
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
            List<CopHourAggData> preCopAggDatas = preCopAggDataMap.get(x);
            // 当前
            dealYList(nowCopAggDatas, copTypes, ltcNow, ltsNow, mtcNow, mtsNow);
            // 去年同期
            dealYList(preCopAggDatas, copTypes, ltcPre, ltsPre, mtcPre, mtsPre);
        });


        CopChartYData copChartYData = new CopChartYData();

        // LTC
        if (CollectionUtil.isNotEmpty(ltcNow)) {
            copChartYData.setLtcNow(ltcNow);
        }
        if (CollectionUtil.isNotEmpty(ltcPre)) {
            copChartYData.setLtcPre(ltcPre);
        }

        // LTS
        if (CollectionUtil.isNotEmpty(ltsNow)) {
            copChartYData.setLtsNow(ltsNow);
        }
        if (CollectionUtil.isNotEmpty(ltcPre)) {
            copChartYData.setLtsPre(ltcPre);
        }

        // MTC
        if (CollectionUtil.isNotEmpty(mtcNow)) {
            copChartYData.setMtcNow(mtcNow);
        }
        if (CollectionUtil.isNotEmpty(mtcPre)) {
            copChartYData.setMtcPre(mtcPre);
        }

        // MTS
        if (CollectionUtil.isNotEmpty(mtsNow)) {
            copChartYData.setMtsNow(mtsNow);
        }
        if (CollectionUtil.isNotEmpty(mtsPre)) {
            copChartYData.setMtsPre(mtsPre);
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

        if (CollectionUtil.isNotEmpty(nowCopAggDatas)) {
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
        // 时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        return rangeOrigin;
    }

    /**
     * 校验时间类型
     *
     * @param systemType
     */
    private List<String> dealSystemType(List<String> systemType) {

        // 时间类型不存在
        if (CollectionUtil.isEmpty(systemType)) {

            // 获取全系统类型
            systemType = DictFrameworkUtils.getDictDataLabelList(SYSTEM_TYPE);
        }

        return systemType;
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
