package cn.bitlinks.ems.module.power.service.report.supplywatertmp;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisStructureInfo;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpTableResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopHourAggData;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopTableResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.SupplyAnalysisPieResultVO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.supplywatertmp.SupplyWaterTmpSettingsMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.minuteagg.MinuteAggDataService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.SUPPLY_ANALYSIS;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.addBigDecimal;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.getConvertData;

/**
 * @author liumingqiang
 */
@Slf4j
@Service
@Validated
public class SupplyWaterTmpSettingsServiceImpl implements SupplyWaterTmpSettingsService {

    @Resource
    private SupplyWaterTmpSettingsMapper supplyWaterTmpSettingsMapper;

    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;

    @Resource
    private MinuteAggDataService minuteAggDataService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Override
    public void updateBatch(List<SupplyWaterTmpSettingsSaveReqVO> supplyAnalysisSettingsList) {
        // 校验
        if (CollUtil.isEmpty(supplyAnalysisSettingsList)) {
            throw exception(SUPPLY_ANALYSIS_SETTINGS_LIST_NOT_EXISTS);
        }

        // 统一保存
        List<SupplyWaterTmpSettingsDO> list = BeanUtils.toBean(supplyAnalysisSettingsList, SupplyWaterTmpSettingsDO.class);
        list.forEach(l -> {
            Long standingBookId = l.getStandingbookId();
            if (!Objects.isNull(standingBookId)) {
                String paramCode = standingbookTmplDaqAttrService.getParamCode(standingBookId, l.getEnergyParamName());
                l.setEnergyParamCode(paramCode);
            }
        });
        supplyWaterTmpSettingsMapper.updateBatch(list);
    }

    @Override
    public List<SupplyWaterTmpSettingsDO> getSupplyWaterTmpSettingsList(SupplyWaterTmpSettingsPageReqVO pageReqVO) {
        return supplyWaterTmpSettingsMapper.selectList((new LambdaQueryWrapperX<SupplyWaterTmpSettingsDO>()
                .eqIfPresent(SupplyWaterTmpSettingsDO::getSystem, pageReqVO.getSystem())
                .orderByAsc(SupplyWaterTmpSettingsDO::getId)));
    }

    @Override
    public List<String> getSystem() {
        return supplyWaterTmpSettingsMapper.getSystem();
    }

    @Override
    public SupplyWaterTmpTableResultVO supplyWaterTmpTable(SupplyWaterTmpReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = LocalDateTimeUtils.beginOfMonth(range[0]);
        LocalDateTime endTime = LocalDateTimeUtils.endOfMonth(range[1]);

        // 2.校验时间类型
        Integer dateType = paramVO.getDateType();
        DataTypeEnum dataTypeEnum = validateDateType(dateType);

        // 3.如果是天 则 班组标记必须要有
        Integer teamFlag = paramVO.getTeamFlag();
        if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
            validateTeamFlag(teamFlag);
        }


        SupplyWaterTmpTableResultVO resultVO = new SupplyWaterTmpTableResultVO();
        resultVO.setDataTime(LocalDateTime.now());

        // 校验系统 没值就返空
        List<String> system1 = paramVO.getSystem();
        if (CollUtil.isEmpty(system1)) {
            return resultVO;
        }

        // 4.获取所有standingBookids
        List<SupplyWaterTmpSettingsDO> supplyAnalysisSettingsList = supplyWaterTmpSettingsMapper.selectList(paramVO);
        // 4.4.设置为空直接返回结果
        if (CollUtil.isEmpty(supplyAnalysisSettingsList)) {
            return resultVO;
        }
        List<Long> standingBookIds = supplyAnalysisSettingsList
                .stream()
                .map(SupplyWaterTmpSettingsDO::getStandingbookId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 4.4.台账id为空直接返回结果
        if (CollUtil.isEmpty(standingBookIds)) {
            return resultVO;
        }

        List<String> paramCodes = supplyAnalysisSettingsList
                .stream()
                .map(SupplyWaterTmpSettingsDO::getEnergyParamCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        List<String> codes = supplyAnalysisSettingsList
                .stream()
                .map(SupplyWaterTmpSettingsDO::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());


        Map<String, Long> standingBookCodeMap = new HashMap<>();
        supplyAnalysisSettingsList.forEach(s -> standingBookCodeMap.put(s.getCode(), s.getStandingbookId()));

        // 5.根据台账ID和参数code查用小时用量数据
        List<MinuteAggregateDataDO> minuteAggDataList = minuteAggDataService.getTmpRangeDataSteady(
                standingBookIds,
                paramCodes,
                startTime,
                endTime);

        List<Map<String, Object>> result = new ArrayList<>();
        // 天
        if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
            result = dealDayData(
                    minuteAggDataList,
                    startTime,
                    endTime,
                    standingBookCodeMap,
                    codes,
                    teamFlag);

        } else if (dataTypeEnum.equals(DataTypeEnum.HOUR)) {
            // 小时数据
            result = dealHourData(
                    minuteAggDataList,
                    startTime,
                    endTime,
                    standingBookCodeMap,
                    codes);
        } else {

        }


        LocalDateTime lastTime = minuteAggDataService.getLastTime(
                standingBookIds,
                paramCodes,
                range[0],
                range[1]);

        resultVO.setDataTime(lastTime);
        resultVO.setList(result);

        return resultVO;
    }

    /**
     * 处理小时数据
     *
     * @param minuteAggDataList
     * @param startTime
     * @param endTime
     * @param standingBookCodeMap
     * @param codes
     * @return
     */
    private List<Map<String, Object>> dealDayData(List<MinuteAggregateDataDO> minuteAggDataList,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime,
                                                  Map<String, Long> standingBookCodeMap,
                                                  List<String> codes, Integer teamFlag) {

        List<Map<String, Object>> result = new ArrayList<>();

        //  1.处理小时数据为天数据
        hourToDay(minuteAggDataList);

        return result;

    }

    /**
     * 小时数据转成天数据
     *
     * @param minuteAggDataList
     */
    private void hourToDay(List<MinuteAggregateDataDO> minuteAggDataList) {


//        Map<Long, Map<String, List<MinuteAggregateDataDO>>> collect = minuteAggDataList.stream()
//                .collect(Collectors.groupingBy(
//
//                        // 第一个分组条件：按 台账
//                        MinuteAggregateDataDO::getStandingbookId,
//                        // 第二个分组条件：按参数code
//                        Collectors.groupingBy(MinuteAggregateDataDO::getParamCode),
//                        Collectors.collectingAndThen(
//                                Collectors.toList(),
//                                list -> {
//                                    list.stream().map(m -> m.getAggregateTime().truncatedTo(ChronoUnit.DAYS))
//
//
//                                }
//                        )
//                )).values();


    }


    /**
     * 处理小时数据
     *
     * @param minuteAggDataList
     * @param startTime
     * @param endTime
     * @param standingBookCodeMap
     * @param codes
     * @return
     */
    private List<Map<String, Object>> dealHourData(List<MinuteAggregateDataDO> minuteAggDataList,
                                                   LocalDateTime startTime,
                                                   LocalDateTime endTime,
                                                   Map<String, Long> standingBookCodeMap,
                                                   List<String> codes) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 按时间分map
        Map<LocalDateTime, List<MinuteAggregateDataDO>> minuteAggDataMap = minuteAggDataList.stream()
                .collect(Collectors.groupingBy(MinuteAggregateDataDO::getAggregateTime));


        // 月份最多31天  小时数据
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
                        List<MinuteAggregateDataDO> minuteAggDatas = minuteAggDataMap.get(currentTime);

                        if (CollUtil.isNotEmpty(minuteAggDatas)) {
                            // 不等的情况
                            Map<Long, MinuteAggregateDataDO> minuteAggregateDataMap = minuteAggDatas.stream()
                                    .collect(Collectors.toMap(MinuteAggregateDataDO::getStandingbookId, Function.identity()));

                            codes.forEach(c -> {
                                String key = c + "_" + year + "-" + monthValue;
                                Long standingBookId = standingBookCodeMap.get(c);
                                MinuteAggregateDataDO minuteAggregateData = minuteAggregateDataMap.get(standingBookId);
                                if (Objects.isNull(minuteAggregateData)) {
                                    map.put(key, BigDecimal.ZERO);
                                } else {
                                    map.put(key, minuteAggregateData.getFullValue());
                                }
                            });

                        } else {
                            codes.forEach(c -> {
                                String key = c + "_" + year + "-" + monthValue;
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
    public SupplyAnalysisPieResultVO supplyWaterTmpChart(SupplyWaterTmpReportParamVO paramVO) {
        return null;
    }

    @Override
    public List<List<String>> getExcelHeader(SupplyWaterTmpReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];
        // 表头数据
        List<List<String>> list = ListUtils.newArrayList();
        // 表单名称
        String sheetName = SUPPLY_ANALYSIS;
        // 统计周期
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);
        // 统计系统
        List<String> system = paramVO.getSystem();
        String systemStr = CollUtil.isNotEmpty(system) ? String.join("、", system) : "全";

        list.add(Arrays.asList("表单名称", "统计系统", "统计周期", "系统", "系统"));
        list.add(Arrays.asList(sheetName, systemStr, strTime, "分析项", "分析项"));

        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, systemStr, strTime, x, "用量"));
            list.add(Arrays.asList(sheetName, systemStr, strTime, x, "占比(%)"));
        });

        // 周期合计
        list.add(Arrays.asList(sheetName, systemStr, strTime, "周期合计", "用量"));
        list.add(Arrays.asList(sheetName, systemStr, strTime, "周期合计", "占比(%)"));
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(SupplyWaterTmpReportParamVO paramVO) {

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        SupplyWaterTmpTableResultVO resultVO = supplyWaterTmpTable(paramVO);


        List<SupplyAnalysisStructureInfo> statisticsInfoList = resultVO.getList();
        // 底部合计map
        Map<String, BigDecimal> sumStandardCoatMap = new HashMap<>();
        Map<String, BigDecimal> sumProportionMap = new HashMap<>();

        return result;
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

    /**
     * 校验班组
     *
     * @param teamFlag
     */
    private void validateTeamFlag(Integer teamFlag) {
        // 时间类型不存在
        if (Objects.isNull(teamFlag)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        if (teamFlag != 0 && teamFlag != 1) {
            throw exception(DATE_TYPE_NOT_MATCH);
        }
    }
}
