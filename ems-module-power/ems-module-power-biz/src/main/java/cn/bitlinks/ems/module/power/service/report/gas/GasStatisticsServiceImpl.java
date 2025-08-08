package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.GasStatisticsInfoData;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerTankSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.VPowerMeasurementAttributesDO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.PowerTankSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.VPowerMeasurementAttributesMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;

import static cn.bitlinks.ems.framework.common.enums.DataTypeEnum.DAY;
import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_RANGE_EXCEED_LIMIT;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.END_TIME_MUST_AFTER_START_TIME;
import static cn.bitlinks.ems.module.power.enums.GasStatisticsCacheConstants.GAS_STATISTICS_TABLE;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;

/**
 * 气化科报表 Service 实现类
 *
 * @author bmqi
 */
@Service
@Validated
@Slf4j
public class GasStatisticsServiceImpl implements GasStatisticsService {


    @Resource
    private PowerTankSettingsMapper powerTankSettingsMapper;

    @Resource
    private VPowerMeasurementAttributesMapper vPowerMeasurementMapper;

    @Resource
    private MinuteAggregateDataService minuteAggregateDataService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    // 后续可能根据三目运算符来取动态的有效数字位scale
    private Integer scale = DEFAULT_SCALE;


    @Override
    public List<PowerTankSettingsRespVO> getPowerTankSettings() {
        return BeanUtils.toBean(powerTankSettingsMapper.selectList(), PowerTankSettingsRespVO.class);
    }

    @Override
    public Boolean savePowerTankSettings(SettingsParamVO paramVO) {
        return powerTankSettingsMapper
                .savePowerTankSettings(BeanUtils.toBean(paramVO.getPowerTankSettingsParamVOList(), PowerTankSettingsDO.class));
    }

    @Override
    public List<EnergyStatisticsItemInfoRespVO> getEnergyStatisticsItems() {
        return BeanUtils.toBean(vPowerMeasurementMapper.selectList(), EnergyStatisticsItemInfoRespVO.class);
    }

    @Override
    public GasStatisticsResultVO<GasStatisticsInfo> gasStatisticsTable(GasStatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        //时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }
        String cacheKey = GAS_STATISTICS_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            return JSON.parseObject(cacheRes, new TypeReference<GasStatisticsResultVO<GasStatisticsInfo>>() {
            });
        }

        // 表头处理，只展示到日
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], DAY);

        // 返回结果
        GasStatisticsResultVO<GasStatisticsInfo> resultVO = new GasStatisticsResultVO<>();
        resultVO.setHeader(tableHeader);

        // 获取视图数据
        List<VPowerMeasurementAttributesDO> vPowerMeasurementAttributesDOS = vPowerMeasurementMapper.selectList();

        // 处理ID列表：如果没传就用视图所有ID
        List<Long> idList = paramVO.getEnergyStatisticsItemIds();
        if (CollUtil.isEmpty(idList)) {
            idList = vPowerMeasurementAttributesDOS.stream()
                    .map(VPowerMeasurementAttributesDO::getStandingbookId)
                    .collect(Collectors.toList());
        }
        final List<Long> finalIdList = idList;

        // 过滤视图数据
        List<VPowerMeasurementAttributesDO> filteredAttributes = vPowerMeasurementAttributesDOS.stream()
                .filter(attr -> finalIdList.contains(attr.getStandingbookId()))
                .collect(Collectors.toList());

        // 提取台账ID和参数编码（即使filteredAttributes为空也要处理）
        List<Long> standingbookIds = filteredAttributes.stream()
                .map(VPowerMeasurementAttributesDO::getStandingbookId)
                .distinct()
                .collect(Collectors.toList());

        List<String> paramCodes = filteredAttributes.stream()
                .map(VPowerMeasurementAttributesDO::getParamCode)
                .distinct()
                .collect(Collectors.toList());

        // 获取储罐设置数据
        List<PowerTankSettingsDO> powerTankSettings = powerTankSettingsMapper.selectList();
        Map<Long, PowerTankSettingsDO> tankSettingsMap = powerTankSettings.stream()
                .filter(e -> e.getStandingbookId() != null)
                .collect(Collectors.toMap(
                        PowerTankSettingsDO::getStandingbookId,
                        settings -> settings,
                        (v1, v2) -> v1 // 遇到重复key保留第一条
                ));

        // 生成日期列表
        List<LocalDateTime> dateList = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DAY).stream()
                .map(dateStr -> LocalDateTime.parse(dateStr + " 00:00:00",
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .collect(Collectors.toList());

        // 处理每个计量器具的数据
        List<GasStatisticsInfo> statisticsInfoList = new ArrayList<>();

        // 确保即使没有数据也要返回完整的结构
        if (CollUtil.isNotEmpty(filteredAttributes)) {
            for (VPowerMeasurementAttributesDO attribute : filteredAttributes) {
                GasStatisticsInfo gasStatisticsInfo = new GasStatisticsInfo();
                gasStatisticsInfo.setMeasurementName(attribute.getMeasurementName());
                gasStatisticsInfo.setMeasurementCode(attribute.getMeasurementCode());

                List<GasStatisticsInfoData> statisticsDateDataList = new ArrayList<>();

                for (LocalDateTime date : dateList) {
                    GasStatisticsInfoData data = new GasStatisticsInfoData();
                    data.setDate(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                    BigDecimal value = calculateValueByType(attribute, date, standingbookIds, paramCodes, tankSettingsMap);
                    data.setValue(value);

                    statisticsDateDataList.add(data);
                }

                gasStatisticsInfo.setStatisticsDateDataList(statisticsDateDataList);
                statisticsInfoList.add(gasStatisticsInfo);
            }
        }

        // 填充无数据的日期为0
        statisticsInfoList.forEach(info -> {
            List<GasStatisticsInfoData> oldList = info.getStatisticsDateDataList();
            Map<String, GasStatisticsInfoData> dateMap = oldList.stream()
                    .collect(Collectors.toMap(GasStatisticsInfoData::getDate, d -> d, (a, b) -> a));

            List<GasStatisticsInfoData> newList = new ArrayList<>();
            for (String date : tableHeader) {
                GasStatisticsInfoData data = dateMap.get(date);
                if (data == null) {
                    data = new GasStatisticsInfoData();
                    data.setDate(date);
                    data.setValue(BigDecimal.ZERO);
                }
                newList.add(data);
            }
            info.setStatisticsDateDataList(newList);
        });

        resultVO.setStatisticsInfoList(statisticsInfoList);
        resultVO.setDataTime(LocalDateTime.now());

        // 缓存结果
        String jsonStr = JSON.toJSONString(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 5, TimeUnit.MINUTES);

        return resultVO;
    }

    @Override
    public List<List<String>> getExcelHeader(GasStatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        // 时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 生成Excel表头数据
        // List<List<String>>即 列<行>，如果需要合并单元格，写重复的值即可
        List<List<String>> headerList = new ArrayList<>();

        // 获取时间范围列表作为表头
        List<String> timeRangeList = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DAY);

        String statisticsPeriod = startTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
                "~" + endTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 正确的多级表头构造：外层每个 List<String> 表示一列的多级标题
        // 第1列：表单名称 / 气化科报表 / 能源统计项
        headerList.add(Arrays.asList("表单名称", "统计周期", "能源统计项"));
        // 第2列：统计周期 / <周期值> / 计量器具编号
        headerList.add(Arrays.asList("气化科报表", statisticsPeriod, "计量器具编号"));
        // 后续每一列为一个日期：气化科报表 / <周期值> / <日期>
        for (String date : timeRangeList) {
            headerList.add(Arrays.asList("气化科报表", statisticsPeriod, date));
        }

        return headerList;
    }

    @Override
    public List<List<Object>> getExcelData(GasStatisticsParamVO paramVO) {
        // 获取气化科报表数据
        GasStatisticsResultVO<GasStatisticsInfo> resultVO = gasStatisticsTable(paramVO);
        List<GasStatisticsInfo> statisticsInfoList = resultVO.getStatisticsInfoList();
        List<String> tableHeader = resultVO.getHeader();

        // 存储Excel数据行
        List<List<Object>> excelDataList = new ArrayList<>();

        // 遍历每个计量器具的统计数据
        for (GasStatisticsInfo gasStatisticsInfo : statisticsInfoList) {
            // 获取计量器具基本信息
            String measurementName = gasStatisticsInfo.getMeasurementName(); // 计量器具名称
            String measurementCode = gasStatisticsInfo.getMeasurementCode(); // 计量器具编码
            List<GasStatisticsInfoData> statisticsDateDataList = gasStatisticsInfo.getStatisticsDateDataList();

            // 将日期数据转换为Map，便于快速查找
            Map<String, GasStatisticsInfoData> dateDataMap = statisticsDateDataList.stream()
                    .collect(Collectors.toMap(GasStatisticsInfoData::getDate, data -> data, (existing, replacement) -> existing));

            // 创建一行数据
            List<Object> dataRow = new ArrayList<>();

            // 第一列：能源统计项（计量器具名称）
            dataRow.add(measurementName != null ? measurementName : "");

            // 第二列：计量器具编号
            dataRow.add(measurementCode != null ? measurementCode : "");

            // 后续列：每个时间点的数值
            for (String date : tableHeader) {
                GasStatisticsInfoData dateData = dateDataMap.get(date);
                if (dateData != null && dateData.getValue() != null) {
                    // 保留指定的小数位数
                    BigDecimal value = dateData.getValue().setScale(scale, BigDecimal.ROUND_HALF_UP);
                    dataRow.add(value);
                } else {
                    // 如果没有数据，填充0
                    dataRow.add(BigDecimal.ZERO.setScale(scale, BigDecimal.ROUND_HALF_UP));
                }
            }

            // 将数据行添加到Excel数据列表中
            excelDataList.add(dataRow);
        }

        return excelDataList;
    }

    /**
     * 根据计算类型计算值
     *
     * @param attribute       计量器具属性
     * @param date            日期
     * @param standingbookIds 台账ID列表
     * @param paramCodes      参数编码列表
     * @param tankSettingsMap 储罐设置映射
     * @return 计算后的值
     */
    private BigDecimal calculateValueByType(VPowerMeasurementAttributesDO attribute,
                                            LocalDateTime date,
                                            List<Long> standingbookIds,
                                            List<String> paramCodes,
                                            Map<Long, PowerTankSettingsDO> tankSettingsMap) {

        Integer calculateType = attribute.getCalculateType();
        Long standingbookId = attribute.getStandingbookId();
        String paramCode = attribute.getParamCode();

        // 如果calculateType为null，返回0
        if (calculateType == null) {
            return BigDecimal.ZERO;
        }

        try {
            switch (calculateType) {
                case 0:
                    // 取得今天有数据的最后一分钟的数值full_value
                    return getLastMinuteFullValue(standingbookId, paramCode, date);

                case 1:
                    // 取得今天所有increment_value值之和
                    return getIncrementalSum(standingbookId, paramCode, date);

                case 2:
                    // 取得今天有数据的最后一分钟的数值full_value，带入到公式H=Δp/(ρg)求出的H值
                    return calculateHValue(standingbookId, paramCode, date, tankSettingsMap);

                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            log.error("计算值失败，standingbookId: {}, paramCode: {}, date: {}, calculateType: {}",
                    standingbookId, paramCode, date, calculateType, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 获取最后一分钟的full_value
     */
    private BigDecimal getLastMinuteFullValue(Long standingbookId, String paramCode, LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        log.info("[getLastMinuteFullValue] standingbookId={}, paramCode={}, startOfDay={}, endOfDay={}", standingbookId, paramCode, startOfDay, endOfDay);
        List<MinuteAggregateDataDO> dataList = minuteAggregateDataService.selectLastMinuteDataByDate(
                Collections.singletonList(standingbookId),
                Collections.singletonList(paramCode),
                startOfDay, endOfDay);
        log.info("[getLastMinuteFullValue] result: {}", JSON.toJSONString(dataList));
        if (CollUtil.isNotEmpty(dataList)) {
            return dataList.get(0).getFullValue() != null ? dataList.get(0).getFullValue() : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 获取增量值之和
     */
    private BigDecimal getIncrementalSum(Long standingbookId, String paramCode, LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        log.info("[getIncrementalSum] standingbookId={}, paramCode={}, startOfDay={}, endOfDay={}", standingbookId, paramCode, startOfDay, endOfDay);
        List<MinuteAggregateDataDO> dataList = minuteAggregateDataService.selectIncrementalSumByDate(
                Collections.singletonList(standingbookId),
                Collections.singletonList(paramCode),
                startOfDay, endOfDay);
        log.info("[getIncrementalSum] result: {}", JSON.toJSONString(dataList));
        if (CollUtil.isNotEmpty(dataList)) {
            return dataList.get(0).getIncrementalValue() != null ? dataList.get(0).getIncrementalValue() : BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算H值：H=Δp/(ρg)
     */
    private BigDecimal calculateHValue(Long standingbookId, String paramCode, LocalDateTime date,
                                       Map<Long, PowerTankSettingsDO> tankSettingsMap) {
        // 获取储罐设置
        PowerTankSettingsDO tankSettings = tankSettingsMap.get(standingbookId);
        // 获取Δp值（最后一分钟的full_value）
        BigDecimal deltaP = getLastMinuteFullValue(tankSettings.getPressureDiffId(), paramCode, date);

        if (deltaP.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (tankSettings == null || tankSettings.getDensity() == null || tankSettings.getGravityAcceleration() == null) {
            log.warn("储罐设置数据不完整，standingbookId: {}", standingbookId);
            return BigDecimal.ZERO;
        }

        BigDecimal density = tankSettings.getDensity();
        BigDecimal gravity = tankSettings.getGravityAcceleration();

        // 计算H = Δp/(ρg)
        if (density.compareTo(BigDecimal.ZERO) == 0 || gravity.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("密度或重力加速度为0，无法计算H值，standingbookId: {}", standingbookId);
            return BigDecimal.ZERO;
        }

        BigDecimal denominator = density.multiply(gravity);
        return deltaP.divide(denominator, scale, BigDecimal.ROUND_HALF_UP);
    }
}