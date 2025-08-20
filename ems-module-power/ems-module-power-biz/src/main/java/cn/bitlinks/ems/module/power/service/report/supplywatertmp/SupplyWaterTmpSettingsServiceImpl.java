package cn.bitlinks.ems.module.power.service.report.supplywatertmp;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.SupplyWaterTmpMinuteAggData;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.supplywatertmp.SupplyWaterTmpSettingsMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.minuteagg.MinuteAggDataService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import com.alibaba.excel.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.SUPPLY_WATER_TMP;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.divideWithScale;
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
    public void updateBatch(List<SupplyWaterTmpSettingsSaveReqVO> supplyWaterTmpSettingsList) {
        // 校验
        if (CollUtil.isEmpty(supplyWaterTmpSettingsList)) {
            throw exception(SUPPLY_ANALYSIS_SETTINGS_LIST_NOT_EXISTS);
        }

        // 统一保存
        List<SupplyWaterTmpSettingsDO> list = BeanUtils.toBean(supplyWaterTmpSettingsList, SupplyWaterTmpSettingsDO.class);
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
        return supplyWaterTmpSettingsMapper.selectList((new LambdaQueryWrapperX<SupplyWaterTmpSettingsDO>().eqIfPresent(SupplyWaterTmpSettingsDO::getSystem, pageReqVO.getSystem()).orderByAsc(SupplyWaterTmpSettingsDO::getId)));
    }

    @Override
    public List<SupplyWaterTmpSettingsDO> getSystem() {
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
        }else {
            if (teamFlag == 1){
                throw exception(TEAM_NOT_INPUT);
            }
        }


        SupplyWaterTmpTableResultVO resultVO = new SupplyWaterTmpTableResultVO();

        // 校验系统 没值就返空
        List<String> codes1 = paramVO.getCodes();
        if (CollUtil.isEmpty(codes1)) {
            return resultVO;
        }

        // 4.获取所有standingBookids
        List<SupplyWaterTmpSettingsDO> supplyWaterTmpSettingsList = supplyWaterTmpSettingsMapper.selectList(paramVO);
        // 4.4.设置为空直接返回结果
        if (CollUtil.isEmpty(supplyWaterTmpSettingsList)) {
            return resultVO;
        }
        List<Long> standingBookIds = supplyWaterTmpSettingsList.stream().map(SupplyWaterTmpSettingsDO::getStandingbookId).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        // 4.4.台账id为空直接返回结果
        if (CollUtil.isEmpty(standingBookIds)) {
            return resultVO;
        }

        List<String> paramCodes = supplyWaterTmpSettingsList.stream().map(SupplyWaterTmpSettingsDO::getEnergyParamCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        List<String> codes = supplyWaterTmpSettingsList.stream().map(SupplyWaterTmpSettingsDO::getCode).filter(Objects::nonNull).distinct().collect(Collectors.toList());

        Map<String, Long> standingBookCodeMap = new HashMap<>();
        supplyWaterTmpSettingsList.forEach(s -> standingBookCodeMap.put(s.getCode(), s.getStandingbookId()));

        // 5.根据台账ID和参数code查用小时用量数据
        List<SupplyWaterTmpMinuteAggData> minuteAggDataList = minuteAggDataService.getTmpRangeDataSteady(
                standingBookIds,
                paramCodes,
                range[0],
                range[1]);

//        if (CollUtil.isEmpty(minuteAggDataList)) {
//            // TODO: 2025/8/17 处理没数据时的数据
//            return resultVO;
//        }

        List<Map<String, Object>> result = new ArrayList<>();
        // 天
        if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
            result = dealDayData(minuteAggDataList, startTime, endTime, standingBookCodeMap, codes, teamFlag);

        } else if (dataTypeEnum.equals(DataTypeEnum.HOUR)) {
            // 小时数据
            result = dealHourData(minuteAggDataList, startTime, endTime, standingBookCodeMap, codes);
        } else {

        }

        LocalDateTime lastTime = minuteAggDataService.getLastTime(standingBookIds, paramCodes, range[0], range[1]);

        resultVO.setDataTime(lastTime);
        resultVO.setList(result);

        return resultVO;
    }

    /**
     * 处理天数据
     *
     * @param minuteAggDataList
     * @param startTime
     * @param endTime
     * @param standingBookCodeMap
     * @param codes
     * @return
     */
    private List<Map<String, Object>> dealDayData(List<SupplyWaterTmpMinuteAggData> minuteAggDataList, LocalDateTime startTime, LocalDateTime endTime, Map<String, Long> standingBookCodeMap, List<String> codes, Integer teamFlag) {

        List<Map<String, Object>> result = new ArrayList<>();

        //  1.处理小时数据为天数据
        switch (teamFlag) {
            case 0:
                //非班组
                result = dealNonTeamData(minuteAggDataList, startTime, endTime, standingBookCodeMap, codes);
                break;
            case 1:
                // 班组
                result = dealTeamData(minuteAggDataList, startTime, endTime, standingBookCodeMap, codes);
                break;
            default:

        }
        return result;
    }

    /**
     * 处理非班组天数据
     *
     * @param minuteAggDataList
     * @param startTime
     * @param endTime
     * @param standingBookCodeMap
     * @param codes
     * @return
     */
    private List<Map<String, Object>> dealNonTeamData(List<SupplyWaterTmpMinuteAggData> minuteAggDataList, LocalDateTime startTime, LocalDateTime endTime, Map<String, Long> standingBookCodeMap, List<String> codes) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<SupplyWaterTmpMinuteAggData> minuteAggregateDataList = hourToDayNonTeam(minuteAggDataList);
        // 按时间分map
        Map<LocalDateTime, List<SupplyWaterTmpMinuteAggData>> minuteAggDataMap = minuteAggregateDataList.stream().collect(Collectors.groupingBy(SupplyWaterTmpMinuteAggData::getAggregateTime));

        for (int i = 1; i <= 31; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", i + DAY);

            // 月份处理
            LocalDateTime tempStartTime = startTime;
            while (tempStartTime.isBefore(endTime) || tempStartTime.equals(endTime)) {

                // 拼接时间
                int monthValue = tempStartTime.getMonthValue();
                int year = tempStartTime.getYear();
                try {
                    LocalDateTime currentTime = LocalDateTime.of(year, monthValue, i, 0, 0, 0);
                    List<SupplyWaterTmpMinuteAggData> minuteAggDatas = minuteAggDataMap.get(currentTime);

                    if (CollUtil.isNotEmpty(minuteAggDatas)) {
                        Map<Long, SupplyWaterTmpMinuteAggData> minuteAggregateDataMap = minuteAggDatas.stream().collect(Collectors.toMap(SupplyWaterTmpMinuteAggData::getStandingbookId, Function.identity()));

                        codes.forEach(c -> {
                            String key = c + "_" + year + "-" + monthValue;
                            Long standingBookId = standingBookCodeMap.get(c);
                            SupplyWaterTmpMinuteAggData minuteAggregateData = minuteAggregateDataMap.get(standingBookId);
                            if (Objects.isNull(minuteAggregateData)) {
                                map.put(key, null);
                            } else {
                                map.put(key, minuteAggregateData.getFullValue());
                            }
                        });

                    } else {
                        codes.forEach(c -> {
                            String key = c + "_" + year + "-" + monthValue;
                            map.put(key, null);
                        });
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                    codes.forEach(c -> {
                        String key = c + "_" + year + "-" + monthValue;
                        map.put(key, null);
                    });
                }

                tempStartTime = tempStartTime.plusMonths(1);
            }

            result.add(map);
        }
        return result;


    }

    /**
     * 处理班组天数据
     *
     * @param minuteAggDataList
     * @param startTime
     * @param endTime
     * @param standingBookCodeMap
     * @param codes
     * @return
     */
    private List<Map<String, Object>> dealTeamData(List<SupplyWaterTmpMinuteAggData> minuteAggDataList, LocalDateTime startTime, LocalDateTime endTime, Map<String, Long> standingBookCodeMap, List<String> codes) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<SupplyWaterTmpMinuteAggData> minuteAggregateDataList = hourToDayTeam(minuteAggDataList);
        // 按时间分map
        Map<LocalDateTime, List<SupplyWaterTmpMinuteAggData>> minuteAggDataMap = minuteAggregateDataList.stream().collect(Collectors.groupingBy(SupplyWaterTmpMinuteAggData::getAggregateTime));

        for (int i = 1; i <= 31; i++) {
            Map<String, Object> map1 = new HashMap<>();
            map1.put("date", i + DAY);
            Map<String, Object> map2 = new HashMap<>();
            map2.put("date", i + DAY);

            // 月份处理
            LocalDateTime tempStartTime = startTime;
            while (tempStartTime.isBefore(endTime) || tempStartTime.equals(endTime)) {

                // 拼接时间
                int monthValue = tempStartTime.getMonthValue();
                int year = tempStartTime.getYear();
                try {
                    LocalDateTime currentTime = LocalDateTime.of(year, monthValue, i, 0, 0, 0);
                    List<SupplyWaterTmpMinuteAggData> minuteAggDatas = minuteAggDataMap.get(currentTime);

                    if (CollUtil.isNotEmpty(minuteAggDatas)) {
                        Map<Long, List<SupplyWaterTmpMinuteAggData>> minuteAggregateDataMap = minuteAggDatas
                                .stream()
                                .collect(Collectors.groupingBy(SupplyWaterTmpMinuteAggData::getStandingbookId));

                        // 处理一下 点1  点2
                        codes.forEach(c -> {
                            String key1 = POINT_ONE + "_" + c + "_" + year + "-" + monthValue;
                            String key2 = POINT_TWO + "_" + c + "_" + year + "-" + monthValue;
                            Long standingBookId = standingBookCodeMap.get(c);
                            List<SupplyWaterTmpMinuteAggData> list = minuteAggregateDataMap.get(standingBookId);
                            if (CollUtil.isEmpty(list)) {
                                map1.put(key1, null);
                                map2.put(key2, null);
                            } else {
                                list.forEach(l -> {
                                    if (POINT_ONE.equals(l.getPoint())) {
                                        map1.put(key1, l.getFullValue());
                                        Object o = map2.getOrDefault(key2, 0);
                                        if (o.equals(0)) {
                                            map2.put(key2, null);
                                        }
                                    } else {
                                        Object o = map1.getOrDefault(key1, 0);
                                        // 如果不存在才赋值0 如果存在则不赋值0，保留原来的值
                                        if (o.equals(0)) {
                                            map2.put(key2, null);
                                        }
                                        map2.put(key2, l.getFullValue());
                                    }
                                });
                            }
                        });

                    } else {
                        codes.forEach(c -> {
                            String key1 = POINT_ONE + "_" + c + "_" + year + "-" + monthValue;
                            String key2 = POINT_TWO + "_" + c + "_" + year + "-" + monthValue;
                            map1.put(key1, null);
                            map2.put(key2, null);
                        });
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                    codes.forEach(c -> {
                        String key1 = POINT_ONE + "_" + c + "_" + year + "-" + monthValue;
                        String key2 = POINT_TWO + "_" + c + "_" + year + "-" + monthValue;
                        map1.put(key1, null);
                        map2.put(key2, null);
                    });
                }

                tempStartTime = tempStartTime.plusMonths(1);
            }

            result.add(map1);
            result.add(map2);
        }
        return result;
    }

    /**
     * 小时数据转成天数据
     *
     * @param minuteAggDataList
     */
    private List<SupplyWaterTmpMinuteAggData> hourToDayNonTeam(List<SupplyWaterTmpMinuteAggData> minuteAggDataList) {

        List<SupplyWaterTmpMinuteAggData> result = new ArrayList<>();
        // 非班组
        Map<Long, Map<String, Map<LocalDateTime, SupplyWaterTmpMinuteAggData>>> collect = minuteAggDataList.stream().map(m -> m.setAggregateTime(m.getAggregateTime().truncatedTo(ChronoUnit.DAYS))).collect(Collectors.groupingBy(
                // 第一个分组条件：按 台账
                SupplyWaterTmpMinuteAggData::getStandingbookId,
                // 第二个分组条件：按参数code
                Collectors.groupingBy(SupplyWaterTmpMinuteAggData::getParamCode, Collectors.groupingBy(SupplyWaterTmpMinuteAggData::getAggregateTime, Collectors.collectingAndThen(Collectors.toList(), list -> {
                    BigDecimal sum = list.stream().map(SupplyWaterTmpMinuteAggData::getFullValue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    // 取平均数:00:00~23:00小时温度值之和除以24. （2025-08-18 14:33变更） 当中间采集时间数据缺失时，分母保持24、14、10
                    BigDecimal avg = sum.divide(new BigDecimal(24), 10, RoundingMode.HALF_UP);
                    SupplyWaterTmpMinuteAggData minuteAggregateDataDO = list.get(0);
                    minuteAggregateDataDO.setFullValue(avg);
                    result.add(minuteAggregateDataDO);
                    return minuteAggregateDataDO;
                })))));
        return result;
    }

    /**
     * 小时数据转成天数据
     * 一天记录两个点位值：
     * 点位1：0点到7点，18点到23点，共14个点，用14个小时点总值求平均，得到点位1的值。
     * 点位2：8点到17点，共10个点，用10个小时点总值求平均，得到点位2的值。
     *
     * @param minuteAggDataList
     */
    private List<SupplyWaterTmpMinuteAggData> hourToDayTeam(List<SupplyWaterTmpMinuteAggData> minuteAggDataList) {
        List<SupplyWaterTmpMinuteAggData> result = new ArrayList<>();
        // 非班组
        minuteAggDataList.stream().collect(Collectors.groupingBy(
                // 第一个分组条件：按 台账
                SupplyWaterTmpMinuteAggData::getStandingbookId,
                // 第二个分组条件：按参数code
                Collectors.groupingBy(SupplyWaterTmpMinuteAggData::getParamCode, Collectors.groupingBy(data -> data.getAggregateTime().toLocalDate(), Collectors.collectingAndThen(Collectors.toList(), list -> {

                    //  点位1：  0~7 18~23
                    List<SupplyWaterTmpMinuteAggData> one = new ArrayList<>();
                    //  点位2：  8~17
                    List<SupplyWaterTmpMinuteAggData> two = new ArrayList<>();

                    list.forEach(l -> {
                        LocalDateTime aggregateTime = l.getAggregateTime();
                        int hour = aggregateTime.getHour();
                        if (8 <= hour && hour <= 17) {
                            two.add(l);
                        } else {
                            one.add(l);
                        }
                    });

                    // 点位1 计算
                    BigDecimal oneSum = one.stream()
                            .map(SupplyWaterTmpMinuteAggData::getFullValue)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 点位2 计算
                    BigDecimal twoSum = two.stream()
                            .map(SupplyWaterTmpMinuteAggData::getFullValue)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    // 点位1：0点到7点，18点到23点，共14个点，用14个小时点总值求平均，得到点位1的值。
                    //点位2：8点到17点，共10个点，用10个小时点总值求平均，得到点位2的值。
                    //注：当中间采集时间数据缺失时，分母保持24、14、10。
                    BigDecimal onwAvg = divideWithScale(new BigDecimal(14), oneSum, 10);
                    BigDecimal twoAvg = divideWithScale(new BigDecimal(10), twoSum, 10);

                    // 点位1 构建
                    SupplyWaterTmpMinuteAggData old = list.get(0);
                    SupplyWaterTmpMinuteAggData minuteAggDataOne = new SupplyWaterTmpMinuteAggData();
                    minuteAggDataOne.setParamCode(old.getParamCode());
                    minuteAggDataOne.setStandingbookId(old.getStandingbookId());
                    minuteAggDataOne.setAggregateTime(old.getAggregateTime().truncatedTo(ChronoUnit.DAYS));
                    minuteAggDataOne.setFullValue(onwAvg);
                    minuteAggDataOne.setPoint(1);
                    result.add(minuteAggDataOne);

                    // 点位2 构建
                    SupplyWaterTmpMinuteAggData minuteAggDataTwo = new SupplyWaterTmpMinuteAggData();
                    minuteAggDataTwo.setParamCode(old.getParamCode());
                    minuteAggDataTwo.setStandingbookId(old.getStandingbookId());
                    minuteAggDataTwo.setAggregateTime(old.getAggregateTime().truncatedTo(ChronoUnit.DAYS));
                    minuteAggDataTwo.setFullValue(twoAvg);
                    minuteAggDataTwo.setPoint(2);
                    result.add(minuteAggDataTwo);

                    return minuteAggDataTwo;
                })))));

        return result;
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
    private List<Map<String, Object>> dealHourData(List<SupplyWaterTmpMinuteAggData> minuteAggDataList, LocalDateTime startTime, LocalDateTime endTime, Map<String, Long> standingBookCodeMap, List<String> codes) {
        List<Map<String, Object>> result = new ArrayList<>();
        // 按时间分map
        Map<LocalDateTime, List<SupplyWaterTmpMinuteAggData>> minuteAggDataMap = minuteAggDataList.stream().collect(Collectors.groupingBy(SupplyWaterTmpMinuteAggData::getAggregateTime));

        // 月份最多31天  小时数据
        for (int i = 1; i <= 31; i++) {
            for (int j = 0; j <= 23; j++) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", i + DAY + j + ":00:00");

                // 月份处理
                LocalDateTime tempStartTime = startTime;
                while (tempStartTime.isBefore(endTime) || tempStartTime.equals(endTime)) {

                    // 拼接时间
                    int monthValue = tempStartTime.getMonthValue();
                    int year = tempStartTime.getYear();
                    try {
                        LocalDateTime currentTime = LocalDateTime.of(year, monthValue, i, j, 0, 0);
                        List<SupplyWaterTmpMinuteAggData> minuteAggDatas = minuteAggDataMap.get(currentTime);

                        if (CollUtil.isNotEmpty(minuteAggDatas)) {
                            // 不等的情况
                            Map<Long, SupplyWaterTmpMinuteAggData> minuteAggregateDataMap = minuteAggDatas.stream().collect(Collectors.toMap(SupplyWaterTmpMinuteAggData::getStandingbookId, Function.identity()));

                            codes.forEach(c -> {
                                String key = c + "_" + year + "-" + monthValue;
                                Long standingBookId = standingBookCodeMap.get(c);
                                SupplyWaterTmpMinuteAggData minuteAggregateData = minuteAggregateDataMap.get(standingBookId);
                                if (Objects.isNull(minuteAggregateData)) {
                                    map.put(key, null);
                                } else {
                                    map.put(key, minuteAggregateData.getFullValue());
                                }
                            });

                        } else {
                            codes.forEach(c -> {
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

        return result;
    }

    @Override
    public SupplyWaterTmpChartResultVO supplyWaterTmpChart(SupplyWaterTmpReportParamVO paramVO) {

        SupplyWaterTmpChartResultVO resultVO = new SupplyWaterTmpChartResultVO();

        SupplyWaterTmpTableResultVO result = supplyWaterTmpTable(paramVO);
        resultVO.setDataTime(result.getDataTime());

        // 2.校验时间类型
        Integer dateType = paramVO.getDateType();
        DataTypeEnum dataTypeEnum = validateDateType(dateType);

        LocalDateTime[] range = paramVO.getRange();
        List<Map<String, Object>> list = result.getList();

        if (CollUtil.isEmpty(list)) {
            return resultVO;
        }

        Integer teamFlag = paramVO.getTeamFlag();

        List<SupplyWaterTmpSettingsDO> supplyWaterTmpSettingsList = supplyWaterTmpSettingsMapper.selectList(paramVO);
        if (CollUtil.isEmpty(supplyWaterTmpSettingsList)) {
            return resultVO;
        }

        for (SupplyWaterTmpSettingsDO one : supplyWaterTmpSettingsList) {
            String code = one.getCode();

            switch (code) {
                case LTWT:
                    // 低温水供水温度
                    LtwtChartVO ltwt = dealSingleSystem(list, one, teamFlag, range, dataTypeEnum);
                    resultVO.setLtwt(ltwt);
                    break;
                case MTWT:
                    // 中温水供水温度
                    LtwtChartVO mtwt = dealSingleSystem(list, one, teamFlag, range, dataTypeEnum);
                    resultVO.setMtwt(mtwt);
                    break;
                case HRWT:
                    // 热回收水供水温度
                    LtwtChartVO hrwt = dealSingleSystem(list, one, teamFlag, range, dataTypeEnum);
                    resultVO.setHrwt(hrwt);
                    break;
                case BHWT:
                    // 热水供水温度（锅炉出水）  热水供水温度（市政出水）
                    SupplyWaterTmpSettingsDO two = supplyWaterTmpSettingsMapper.selectOneByCode(MHWT);
                    resultVO.setBhwt(dealDoubleSystem(list, one, two, teamFlag, range, dataTypeEnum));
                    break;
                case MHWT:
                    // 热水供水温度（锅炉出水）  热水供水温度（市政出水）
                    SupplyWaterTmpSettingsDO bhwt = supplyWaterTmpSettingsMapper.selectOneByCode(BHWT);
                    resultVO.setBhwt(dealDoubleSystem(list, bhwt, one, teamFlag, range, dataTypeEnum));
                    break;
                case PCWP:
                    // PCW供水压力温度（供水压力） PCW供水压力温度（供水温度）
                    SupplyWaterTmpSettingsDO pcwt = supplyWaterTmpSettingsMapper.selectOneByCode(PCWT);
                    resultVO.setPcwp(dealDoubleSystem(list, one, pcwt, teamFlag, range, dataTypeEnum));
                    break;
                case PCWT:
                    // PCW供水压力温度（供水压力） PCW供水压力温度（供水温度）
                    SupplyWaterTmpSettingsDO pcwp = supplyWaterTmpSettingsMapper.selectOneByCode(PCWP);
                    resultVO.setPcwp(dealDoubleSystem(list, pcwp, one, teamFlag, range, dataTypeEnum));
                    break;
                default:
            }
        }


        return resultVO;
    }

    /**
     * 热水供水温度（锅炉出水）  热水供水温度（市政出水） PCW供水压力温度（供水压力） PCW供水压力温度（供水温度）
     *
     * @param list
     * @param one
     * @param two
     * @param teamFlag
     * @param range
     * @param dataTypeEnum
     * @return
     */

    private PcwChartVO dealDoubleSystem(List<Map<String, Object>> list,
                                        SupplyWaterTmpSettingsDO one,
                                        SupplyWaterTmpSettingsDO two,
                                        Integer teamFlag,
                                        LocalDateTime[] range,
                                        DataTypeEnum dataTypeEnum) {
        PcwChartVO pcwp = new PcwChartVO();
        // 锅炉上限/压力上限
        pcwp.setMax1(one.getMax());
        // 锅炉下限/压力下限
        pcwp.setMin1(one.getMin());
        // 市政上限/温度上限
        pcwp.setMax2(two.getMax());
        // 市政下限/温度下限
        pcwp.setMin2(two.getMin());
        // 锅炉供水温度 供水压力
        String code1 = one.getCode();
        // 市政供水温度 供水温度
        String code2 = two.getCode();

        Set<String> xdata = new HashSet<>();
        // 锅炉供水温度/供水压力
        Map<String, BigDecimal> ydata11 = new HashMap<>();
        // 市政供水温度/供水温度
        Map<String, BigDecimal> ydata12 = new HashMap<>();

        if (teamFlag.equals(0)) {
            //非班组
            list.forEach(l -> {

                List<String> key1 = getKey(l, code1);
                List<String> key2 = getKey(l, code2);

                if (CollUtil.isNotEmpty(key1)) {
                    key1.forEach(key -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, key, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value = l.get(key);
                            xdata.add(date);
                            ydata11.put(date, (BigDecimal) value);
                        }
                    });
                }

                if (CollUtil.isNotEmpty(key2)) {
                    key2.forEach(key -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, key, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value = l.get(key);
                            xdata.add(date);
                            ydata12.put(date, (BigDecimal) value);
                        }
                    });
                }
            });


            List<String> dateList = xdata.stream().sorted().collect(Collectors.toList());
            pcwp.setXdata(dateList);

            //  点位1锅炉供水温度/供水压力
            List<BigDecimal> y11 = dateList
                    .stream()
                    .map(d -> ydata11.getOrDefault(d, BigDecimal.ZERO))
                    .collect(Collectors.toList());
            pcwp.setYdata11(y11);

            //  点位1 市政供水温度/供水温度
            if (CollUtil.isNotEmpty(ydata12)) {
                List<BigDecimal> y12 = dateList
                        .stream()
                        .map(d -> ydata12.getOrDefault(d, BigDecimal.ZERO))
                        .collect(Collectors.toList());
                pcwp.setYdata12(y12);
            }

        } else {
            // 班组  点位2
            Map<String, BigDecimal> ydata21 = new HashMap<>();
            Map<String, BigDecimal> ydata22 = new HashMap<>();
            list.forEach(l -> {

                List<String> oneKey1 = getKey(l, "1_" + code1);
                List<String> oneKey2 = getKey(l, "2_" + code1);
                List<String> twoKey1 = getKey(l, "1_" + code2);
                List<String> twoKey2 = getKey(l, "2_" + code2);

                if (CollUtil.isNotEmpty(oneKey1)) {
                    oneKey1.forEach(key -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, key, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value = l.get(key);
                            xdata.add(date);
                            ydata11.put(date, (BigDecimal) value);
                        }
                    });
                }

                if (CollUtil.isNotEmpty(oneKey2)) {
                    oneKey2.forEach(key -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, key, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value = l.get(key);
                            xdata.add(date);
                            ydata12.put(date, (BigDecimal) value);
                        }
                    });
                }
                if (CollUtil.isNotEmpty(twoKey1)) {
                    twoKey1.forEach(key -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, key, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value = l.get(key);
                            xdata.add(date);
                            ydata21.put(date, (BigDecimal) value);
                        }
                    });
                }

                if (CollUtil.isNotEmpty(twoKey2)) {
                    twoKey2.forEach(key -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, key, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value = l.get(key);
                            xdata.add(date);
                            ydata22.put(date, (BigDecimal) value);
                        }
                    });
                }
            });

            List<String> dateList = xdata.stream().sorted().collect(Collectors.toList());
            pcwp.setXdata(dateList);

            // 点位1
            if (CollUtil.isNotEmpty(ydata11)) {
                List<BigDecimal> y = dateList
                        .stream()
                        .map(d -> ydata11.getOrDefault(d, BigDecimal.ZERO))
                        .collect(Collectors.toList());
                pcwp.setYdata11(y);
            }
            if (CollUtil.isNotEmpty(ydata12)) {
                List<BigDecimal> y = dateList
                        .stream()
                        .map(d -> ydata12.getOrDefault(d, BigDecimal.ZERO))
                        .collect(Collectors.toList());
                pcwp.setYdata12(y);
            }

            // 点位2
            if (CollUtil.isNotEmpty(ydata21)) {
                List<BigDecimal> y = dateList
                        .stream()
                        .map(d -> ydata21.getOrDefault(d, BigDecimal.ZERO))
                        .collect(Collectors.toList());
                pcwp.setYdata21(y);
            }

            if (CollUtil.isNotEmpty(ydata22)) {
                List<BigDecimal> y = dateList
                        .stream()
                        .map(d -> ydata22.getOrDefault(d, BigDecimal.ZERO))
                        .collect(Collectors.toList());
                pcwp.setYdata22(y);
            }
        }

        return pcwp;
    }


    /**
     * 低温水供水温度 中温水供水温度 热回收水供水温度
     *
     * @param list
     * @param one
     * @param teamFlag
     * @return
     */
    private LtwtChartVO dealSingleSystem(List<Map<String, Object>> list,
                                         SupplyWaterTmpSettingsDO one,
                                         Integer teamFlag,
                                         LocalDateTime[] range,
                                         DataTypeEnum dataTypeEnum) {
        LtwtChartVO ltwt = new LtwtChartVO();
        ltwt.setMax(one.getMax());
        ltwt.setMin(one.getMin());
        String code = one.getCode();

        Set<String> xdata = new HashSet<>();
        // 点位1
        Map<String, BigDecimal> ydata1 = new HashMap<>();
        // 点位2  班组时才会用到此点位
        Map<String, BigDecimal> ydata2 = new HashMap<>();
        if (teamFlag.equals(0)) {
            //非班组
            list.forEach(l -> {

                List<String> key = getKey(l, code);

                if (CollUtil.isNotEmpty(key)) {
                    key.forEach(k -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, k, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value = l.get(k);
                            xdata.add(date);
                            ydata1.put(date, (BigDecimal) value);
                        }
                    });

                }
            });
        } else {
            // 班组
            list.forEach(l -> {

                List<String> key1 = getKey(l, "1_" + code);
                List<String> key2 = getKey(l, "2_" + code);

                if (CollUtil.isNotEmpty(key1)) {
                    key1.forEach(k -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, k, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value1 = l.get(k);
                            xdata.add(date);
                            ydata1.put(date, (BigDecimal) value1);
                        }
                    });
                }

                if (CollUtil.isNotEmpty(key2)) {
                    key2.forEach(k -> {
                        //处理时间
                        String date = (String) l.get("date");
                        date = dealDate(date, k, dataTypeEnum);
                        if (Boolean.TRUE.equals(checkDate(date, range, dataTypeEnum))) {
                            // 处理数据
                            Object value2 = l.get(k);
                            xdata.add(date);
                            ydata2.put(date, (BigDecimal) value2);
                        }
                    });
                }
            });
        }
        List<String> dateList = xdata.stream().sorted().collect(Collectors.toList());
        ltwt.setXdata(dateList);

        // 点位1
        List<BigDecimal> y1 = dateList
                .stream()
                .map(d -> ydata1.getOrDefault(d, BigDecimal.ZERO))
                .collect(Collectors.toList());
        ltwt.setYdata1(y1);

        // 点位2
        if (CollUtil.isNotEmpty(ydata2)) {
            List<BigDecimal> y2 = dateList
                    .stream()
                    .map(d -> ydata2.getOrDefault(d, BigDecimal.ZERO))
                    .collect(Collectors.toList());
            ltwt.setYdata2(y2);
        }
        return ltwt;
    }

    private List<String> getKey(Map<String, Object> l, String key) {

        List<String> keyList = new ArrayList<>();
        for (String k : l.keySet()) {
            if (k.contains(key)) {
                keyList.add(k);
            }
        }
        return keyList.stream().sorted().collect(Collectors.toList());
    }

    private String dealDate(String date, String key, DataTypeEnum dataTypeEnum) {

        // 天 时 处理
        String[] l1 = date.split(DAY);
        int day = Integer.parseInt(l1[0].trim());
        int hour = 0;
        if (l1.length > 1) {
            String[] l2 = l1[1].split(StrPool.COLON);
            hour = Integer.parseInt(l2[0].trim());
        }

        // 年 月 处理
        String[] l3 = key.split(StrPool.UNDERLINE);
        String[] l4 = l3[l3.length - 1].split(StrPool.DASHED);
        int year = Integer.parseInt(l4[0].trim());
        int month = Integer.parseInt(l4[1].trim());

        try {
            if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
                LocalDate currentTime = LocalDate.of(year, month, day);
                return LocalDateTimeUtils.getFormatTime(currentTime);
            } else {
                LocalDateTime currentTime = LocalDateTime.of(year, month, day, hour, 0, 0);
                return LocalDateTimeUtils.getFormatTime(currentTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public List<List<String>> getExcelHeader(SupplyWaterTmpReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = LocalDateTimeUtils.beginOfMonth(range[0]);
        LocalDateTime endTime = LocalDateTimeUtils.endOfMonth(range[1]);

        // 表头数据
        List<List<String>> list = ListUtils.newArrayList();
        // 表单名称
        String sheetName = SUPPLY_WATER_TMP;
        // 统计周期
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);
        // 统计系统
        List<SupplyWaterTmpSettingsDO> supplyWaterTmpSettingsList = supplyWaterTmpSettingsMapper.selectList(paramVO);
        List<String> systemList = supplyWaterTmpSettingsList
                .stream()
                .map(SupplyWaterTmpSettingsDO::getSystem).collect(Collectors.toList());
        String system = systemList
                .stream()
                .collect(Collectors.joining("、"));
        String systemStr = CharSequenceUtil.isNotEmpty(system) ? system : "全";

        // 第一列处理
        list.add(Arrays.asList("表单名称", "统计系统", "统计周期", "时间/系统", "时间/系统"));

        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(1);
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> {
            systemList.forEach(s -> {
                list.add(Arrays.asList(sheetName, systemStr, strTime, x, s));
            });
        });
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(SupplyWaterTmpReportParamVO paramVO) {

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        SupplyWaterTmpTableResultVO resultVO = supplyWaterTmpTable(paramVO);

        List<Map<String, Object>> list = resultVO.getList();

        if (CollUtil.isEmpty(list)) {
            return result;
        }

        Integer teamFlag = paramVO.getTeamFlag();

        List<SupplyWaterTmpSettingsDO> supplyWaterTmpSettingsList = supplyWaterTmpSettingsMapper.selectList(paramVO);
        if (CollUtil.isEmpty(supplyWaterTmpSettingsList)) {
            return result;
        }

        List<String> codeList = supplyWaterTmpSettingsList
                .stream()
                .map(SupplyWaterTmpSettingsDO::getCode).collect(Collectors.toList());

        // 找出所有月份 做循环
        Map<String, Object> map = list.get(0);
        List<String> monthList = getAllMonth(map);

        // 处理表数据
        if (teamFlag.equals(0)) {
            //非班组
            list.forEach(l -> {
                List<Object> data = ListUtils.newArrayList();
                data.add(l.get("date"));

                monthList.forEach(m -> {
                    codeList.forEach(c -> {
                        String key = c + StrPool.UNDERLINE + m;
                        // 处理数据
                        Object value = l.get(key);
                        data.add(getConvertData((BigDecimal) value));
                    });
                });

                result.add(data);
            });
        } else {
            // 班组
            list.forEach(l -> {
                List<Object> data = ListUtils.newArrayList();
                data.add(l.get("date"));
                monthList.forEach(m -> {
                    codeList.forEach(c -> {
                        String key1 = "1_" + c + StrPool.UNDERLINE + m;
                        String key2 = "2_" + c + StrPool.UNDERLINE + m;
                        Object value = l.get(key1);
                        if (Objects.isNull(value)) {
                            // 处理数据
                            value = l.get(key2);
                        }
                        data.add(getConvertData((BigDecimal) value));
                    });
                });

                result.add(data);
            });
        }

        return result;
    }

    private List<String> getAllMonth(Map<String, Object> map) {
        return map.keySet().stream().filter(key -> !"date".equals(key)).map(key -> {
            String[] list = key.split(StrPool.UNDERLINE);
            return list[list.length - 1];
        }).distinct().sorted().collect(Collectors.toList());
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
            throw exception(TEAM_NOT_EMPTY);
        }

        if (teamFlag != 0 && teamFlag != 1) {
            throw exception(TEAM_NOT_MATCH);
        }
    }

    private Boolean checkDate(String date, LocalDateTime[] range, DataTypeEnum dataTypeEnum) {
        if (CharSequenceUtil.isNotBlank(date)) {
            if (dataTypeEnum.equals(DataTypeEnum.DAY)) {
                date = date + " 00:00:00";
            }
            return LocalDateTimeUtils.isBetween(range[0], range[1], date);
        } else {
            return false;
        }


    }
}
