package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.GasStatisticsInfoData;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerTankSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerGasMeasurementDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.gas.PowerTankSettingsMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ.StandingbookTmplDaqAttrMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.minuteagg.MinuteAggDataService;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.Comparator;
import java.time.format.DateTimeFormatter;

import static cn.bitlinks.ems.framework.common.enums.DataTypeEnum.DAY;
import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_RANGE_EXCEED_LIMIT;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.END_TIME_MUST_AFTER_START_TIME;
import static cn.bitlinks.ems.module.power.enums.GasStatisticsCacheConstants.GAS_STATISTICS_ENERGY_ITEMS_;
import static cn.bitlinks.ems.module.power.enums.GasStatisticsCacheConstants.GAS_STATISTICS_TABLE;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;

import cn.hutool.json.JSONUtil;

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
    private PowerGasMeasurementService powerGasMeasurementService;

    @Resource
    private MinuteAggregateDataService minuteAggregateDataService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;

    @Resource
    private StandingbookMapper standingbookMapper;

    @Resource
    private StandingbookTmplDaqAttrMapper standingbookTmplDaqAttrMapper;

    @Resource
    private MinuteAggDataService minuteAggDataService;

    // 后续可能根据三目运算符来取动态的有效数字位scale
    private Integer scale = DEFAULT_SCALE;

	/**
	 * 日期字符串格式：yyyy-MM-dd
	 */
	private static final java.time.format.DateTimeFormatter DAY_FORMATTER =
			java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");


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
        // 添加缓存，避免重复查询
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(GAS_STATISTICS_ENERGY_ITEMS_);
        if (compressed != null) {
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isNotEmpty(cacheRes)) {
                log.debug("从缓存获取能源统计项列表");
                return JSON.parseObject(cacheRes, new TypeReference<List<EnergyStatisticsItemInfoRespVO>>() {});
            }
        }

        // 改为从固定43条数据获取
        List<GasMeasurementInfo> gasMeasurementInfos = powerGasMeasurementService.getGasMeasurementInfos();
        List<EnergyStatisticsItemInfoRespVO> result = gasMeasurementInfos.stream()
                .map(info -> {
                    EnergyStatisticsItemInfoRespVO vo = new EnergyStatisticsItemInfoRespVO();
                    vo.setMeasurementCode(info.getMeasurementCode());
                    vo.setMeasurementName(info.getMeasurementName());
                    return vo;
                })
                .collect(Collectors.toList());

        // 缓存结果，有效期30分钟
        String jsonStr = JSONUtil.toJsonStr(result);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(GAS_STATISTICS_ENERGY_ITEMS_, bytes, 30, TimeUnit.MINUTES);
        
        log.debug("能源统计项列表查询完成，共{}条，已缓存", result.size());
        return result;
    }

    @Override
    public GasStatisticsResultVO<GasStatisticsInfo> gasStatisticsTable(GasStatisticsParamVO paramVO) {
        // 参数校验
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 生成缓存键
        String cacheKey = GAS_STATISTICS_TABLE + ":" + 
                startTime.format(DAY_FORMATTER) + ":" + 
                endTime.format(DAY_FORMATTER) + ":" + 
                (paramVO.getEnergyStatisticsItemCodes() != null ? 
                        String.join(",", paramVO.getEnergyStatisticsItemCodes()) : "all");

        // 尝试从缓存获取
        byte[] cachedBytes = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        if (cachedBytes != null) {
            try {
                String jsonStr = StrUtils.decompressGzip(cachedBytes);
                GasStatisticsResultVO<GasStatisticsInfo> cachedResult = JSON.parseObject(jsonStr, 
                        new TypeReference<GasStatisticsResultVO<GasStatisticsInfo>>() {});
                log.info("从缓存获取气化科报表数据，返回{}条记录", 
                        cachedResult.getStatisticsInfoList() != null ? cachedResult.getStatisticsInfoList().size() : 0);
                return cachedResult;
            } catch (Exception e) {
                log.warn("缓存数据解析失败，重新查询: {}", e.getMessage());
            }
        }

        log.info("开始查询气化科报表数据，时间范围: {} ~ {}", startTime, endTime);

        // 构建结果对象
        GasStatisticsResultVO<GasStatisticsInfo> resultVO = new GasStatisticsResultVO<>();
        List<String> timeRangeList = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DAY);
        resultVO.setHeader(timeRangeList);

        // 获取计量器具信息
        List<GasMeasurementInfo> gasMeasurementInfos;
        if (CollUtil.isNotEmpty(paramVO.getEnergyStatisticsItemCodes())) {
            // 直接查询指定的计量器具，避免查询所有43条记录
            List<PowerGasMeasurementDO> measurements = powerGasMeasurementService.getMeasurementsByCodes(paramVO.getEnergyStatisticsItemCodes());
            gasMeasurementInfos = batchConvertToGasMeasurementInfo(measurements);
            log.info("根据指定编码查询到{}条计量器具记录", gasMeasurementInfos.size());
        } else {
            // 查询所有有效的计量器具
            List<PowerGasMeasurementDO> measurements = powerGasMeasurementService.getAllValidMeasurements();
            gasMeasurementInfos = batchConvertToGasMeasurementInfo(measurements);
            log.info("查询到{}条有效计量器具记录", gasMeasurementInfos.size());
        }

        if (CollUtil.isEmpty(gasMeasurementInfos)) {
            log.warn("未找到有效的计量器具信息，返回空结果");
            return resultVO;
        }

        // 批量查询所有相关数据，避免N+1查询
        Map<String, GasMeasurementInfo> measurementMap = new HashMap<>();
        List<Long> allStandingbookIds = new ArrayList<>();
        List<String> allParamCodes = new ArrayList<>();
        Set<String> measurementCodes = new HashSet<>();
        
        for (GasMeasurementInfo info : gasMeasurementInfos) {
            if (info.getStandingbookId() != null && info.getParamCode() != null) {
                measurementMap.put(info.getMeasurementCode(), info);
                allStandingbookIds.add(info.getStandingbookId());
                allParamCodes.add(info.getParamCode());
                measurementCodes.add(info.getMeasurementCode());
            }
        }

        // 去重
        allStandingbookIds = allStandingbookIds.stream().distinct().collect(Collectors.toList());
        allParamCodes = allParamCodes.stream().distinct().collect(Collectors.toList());

        if (CollUtil.isEmpty(allStandingbookIds) || CollUtil.isEmpty(allParamCodes)) {
            log.warn("台账ID或参数编码为空，返回空结果");
            return resultVO;
        }

        // 批量查询储罐设置，避免循环中重复查询
        Map<String, PowerTankSettingsDO> tankSettingsMap = new HashMap<>();
        List<Long> pressureDiffIds = new ArrayList<>();
        
        if (!measurementCodes.isEmpty()) {
            List<PowerTankSettingsDO> tankSettings = powerTankSettingsMapper.selectList(
                    new LambdaQueryWrapperX<PowerTankSettingsDO>()
                            .in(PowerTankSettingsDO::getCode, measurementCodes)
                            .eq(PowerTankSettingsDO::getDeleted, false)
            );
            
            tankSettingsMap = tankSettings.stream()
                    .filter(e -> e.getCode() != null)
                    .collect(Collectors.toMap(
                            PowerTankSettingsDO::getCode,
                            settings -> settings,
                            (v1, v2) -> v1
                    ));
            
            pressureDiffIds = tankSettings.stream()
                    .map(PowerTankSettingsDO::getPressureDiffId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        }

        // 合并所有需要查询的standingbook_id
        allStandingbookIds.addAll(pressureDiffIds);
        allStandingbookIds = allStandingbookIds.stream().distinct().collect(Collectors.toList());

        // 预生成日期列表
        List<LocalDateTime> dateList = timeRangeList.stream()
                .map(dateStr -> LocalDateTime.parse(dateStr + " 00:00:00", 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .collect(Collectors.toList());

        // 批量查询数据并构建缓存
        Map<String, MinuteAggregateDataDO> dataCache = batchQueryData(
                allStandingbookIds, allParamCodes, startTime, endTime);

        // 构建结果数据
        List<GasStatisticsInfo> statisticsInfoList = new ArrayList<>();
        
        for (GasMeasurementInfo info : gasMeasurementInfos) {
            GasStatisticsInfo gasStatisticsInfo = new GasStatisticsInfo();
            gasStatisticsInfo.setMeasurementName(info.getMeasurementName());
            gasStatisticsInfo.setMeasurementCode(info.getMeasurementCode());

            List<GasStatisticsInfoData> statisticsDateDataList = new ArrayList<>();

            for (LocalDateTime date : dateList) {
                GasStatisticsInfoData data = new GasStatisticsInfoData();
                data.setDate(date.format(DAY_FORMATTER));

                BigDecimal value = calculateValueByTypeOptimized(info, date, dataCache, tankSettingsMap);
                data.setValue(dealBigDecimalScale(value));
                statisticsDateDataList.add(data);
            }

            gasStatisticsInfo.setStatisticsDateDataList(statisticsDateDataList);
            statisticsInfoList.add(gasStatisticsInfo);
        }

        resultVO.setStatisticsInfoList(statisticsInfoList);

        // 从数据缓存中获取最新时间
        LocalDateTime lastTime = null;
        if (!dataCache.isEmpty()) {
            lastTime = dataCache.values().stream()
                    .map(MinuteAggregateDataDO::getAggregateTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        }

        if (lastTime != null) {
            resultVO.setDataTime(lastTime);
        }

        // 缓存结果
        try {
            String jsonStr = JSONUtil.toJsonStr(resultVO);
            byte[] bytes = StrUtils.compressGzip(jsonStr);
            byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("缓存结果失败: {}", e.getMessage());
        }

        log.info("气化科报表查询完成，返回{}条统计数据", statisticsInfoList.size());
        return resultVO;
    }
    public static BigDecimal dealBigDecimalScale(BigDecimal num) {
        if (num != null) {
            return num.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);

        }
        return null;
    }
    /**
     * 批量查询数据并构建缓存
     */
    private Map<String, MinuteAggregateDataDO> batchQueryData(
            List<Long> standingbookIds,
            List<String> paramCodes,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        if (CollUtil.isEmpty(standingbookIds) || CollUtil.isEmpty(paramCodes)) {
            log.warn("台账ID或参数编码为空，返回空缓存");
            return new HashMap<>();
        }

        log.info("开始批量查询数据 - standingbookIds: {}, paramCodes: {}, 时间范围: {} ~ {}", 
                standingbookIds.size(), paramCodes.size(), startTime, endTime);

        // 一次性查询所有最后一分钟数据
        List<MinuteAggregateDataDO> lastMinuteData = minuteAggregateDataService
                .selectLastMinuteDataByDateBatch(standingbookIds, paramCodes, startTime, endTime);

        // 一次性查询所有增量数据
        List<MinuteAggregateDataDO> incrementalData = minuteAggregateDataService
                .selectIncrementalSumByDateBatch(standingbookIds, paramCodes, startTime, endTime);

        // 构建缓存Map，key为 "standingbookId:paramCode:date"
        Map<String, MinuteAggregateDataDO> dataCache = new HashMap<>();

        // 处理最后一分钟数据
        for (MinuteAggregateDataDO data : lastMinuteData) {
            String key = String.format("%d:%s:%s",
                    data.getStandingbookId(),
                    data.getParamCode(),
                    data.getAggregateTime().toLocalDate());
            dataCache.put(key, data);
        }

        // 处理增量数据
        for (MinuteAggregateDataDO data : incrementalData) {
            String key = String.format("%d:%s:%s:incremental",
                    data.getStandingbookId(),
                    data.getParamCode(),
                    data.getAggregateTime().toLocalDate());
            dataCache.put(key, data);
        }

        log.info("数据缓存构建完成，总缓存条目数: {} (最后一分钟: {}, 增量: {})", 
                dataCache.size(), lastMinuteData.size(), incrementalData.size());
        
        // 如果没有数据，输出调试信息
        if (dataCache.isEmpty()) {
            log.warn("数据缓存为空，请检查数据库配置和数据");
        }
        
        return dataCache;
    }

    /**
     * 优化后的计算方法，使用缓存数据
     */
    private BigDecimal calculateValueByTypeOptimized(GasMeasurementInfo info,
                                                  LocalDateTime date,
                                                  Map<String, MinuteAggregateDataDO> dataCache,
                                                  Map<String, PowerTankSettingsDO> tankSettingsMap) {

        Integer calculateType = info.getCalculateType();
        Long standingbookId = info.getStandingbookId();
        String paramCode = info.getParamCode();

        // 如果 standingbookId 为 null 或 paramCode 为 null，返回0
        if (standingbookId == null || paramCode == null) {
            return null;
        }

        // 如果calculateType为null，返回0
        if (calculateType == null) {
            return null;
        }

        try {
            BigDecimal result = null;
            switch (calculateType) {
                case 0:
                    // 取得今天有数据的最后一分钟的数值full_value
                    result = getLastMinuteFullValueOptimized(standingbookId, paramCode, date, dataCache);
                    break;

                case 1:
                    // 取得今天所有increment_value值之和
                    result = getIncrementalSumOptimized(standingbookId, paramCode, date, dataCache);
                    break;

                case 2:
                    // 取得今天有数据的最后一分钟的数值full_value，带入到公式H=Δp/(ρg)求出的H值
                    result = calculateHValueOptimized(standingbookId, paramCode, info.getMeasurementCode(), date, dataCache, tankSettingsMap);
                    break;

                default:
                    result = null;
                    break;
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("计算值失败，standingbookId: {}, paramCode: {}, date: {}, calculateType: {}",
                    standingbookId, paramCode, date, calculateType, e);
            return null;
        }
    }

    /**
     * 优化后的获取最后一分钟full_value方法
     */
    private BigDecimal getLastMinuteFullValueOptimized(Long standingbookId, String paramCode, LocalDateTime date, Map<String, MinuteAggregateDataDO> dataCache) {
        if (standingbookId == null || paramCode == null) {
            return null;
        }
        
        String key = String.format("%d:%s:%s", standingbookId, paramCode, date.toLocalDate());
        MinuteAggregateDataDO data = dataCache.get(key);
        if (data != null && data.getFullValue() != null) {
            return data.getFullValue();
        } else {
            return null;
        }
    }

    /**
     * 优化后的获取增量值之和方法
     */
    private BigDecimal getIncrementalSumOptimized(Long standingbookId, String paramCode, LocalDateTime date, Map<String, MinuteAggregateDataDO> dataCache) {
        if (standingbookId == null || paramCode == null) {
            return null;
        }
        
        String key = String.format("%d:%s:%s:incremental", standingbookId, paramCode, date.toLocalDate());
        MinuteAggregateDataDO data = dataCache.get(key);
        if (data != null && data.getIncrementalValue() != null) {
            return data.getIncrementalValue();
        } else {
            return null;
        }
    }

    /**
     * 优化后的计算H值方法
     */
        private BigDecimal calculateHValueOptimized(Long standingbookId, String paramCode, String measurementCode, LocalDateTime date,
                                                 Map<String, MinuteAggregateDataDO> dataCache,
                                                 Map<String, PowerTankSettingsDO> tankSettingsMap) {
        if (standingbookId == null || paramCode == null || measurementCode == null) {
            return null;
        }

        // 获取储罐设置 - 通过计量器具编码查找
        PowerTankSettingsDO tankSettings = tankSettingsMap.get(measurementCode);
        
        if (tankSettings == null || tankSettings.getPressureDiffId() == null) {
            log.warn("储罐设置数据不完整，measurementCode: {}", measurementCode);
            return null;
        }

        // 获取Δp值（最后一分钟的full_value）
        BigDecimal deltaP = getLastMinuteFullValueOptimized(tankSettings.getPressureDiffId(), paramCode, date, dataCache);

        if (deltaP.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        if (tankSettings.getDensity() == null || tankSettings.getGravityAcceleration() == null) {
            log.warn("储罐设置数据不完整，standingbookId: {}", standingbookId);
            return null;
        }

        BigDecimal density = tankSettings.getDensity();
        BigDecimal gravity = tankSettings.getGravityAcceleration();

        // 计算H = Δp/(ρg)
        if (density.compareTo(BigDecimal.ZERO) == 0 || gravity.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("密度或重力加速度为0，无法计算H值，standingbookId: {}", standingbookId);
            return null;
        }

        BigDecimal denominator = density.multiply(gravity);
        return deltaP.divide(denominator, scale, BigDecimal.ROUND_HALF_UP);
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
                if (dateData != null && dateData.getValue() != null && dateData.getValue().compareTo(BigDecimal.ZERO) != 0) {
                    // 保留指定的小数位数
                    BigDecimal value = dateData.getValue().setScale(scale, BigDecimal.ROUND_HALF_UP);
                    dataRow.add(value);
                } else {
                    // 如果没有数据，填充0
                    dataRow.add("/");
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
     * @param info            计量器具信息
     * @param date            日期
     * @param standingbookIds 台账ID列表
     * @param paramCodes      参数编码列表
     * @param tankSettingsMap 储罐设置映射
     * @return 计算后的值
     */
    private BigDecimal calculateValueByType(GasMeasurementInfo info,
                                            LocalDateTime date,
                                            List<Long> standingbookIds,
                                            List<String> paramCodes,
                                            Map<String, PowerTankSettingsDO> tankSettingsMap) {

        Integer calculateType = info.getCalculateType();
        Long standingbookId = info.getStandingbookId();
        String paramCode = info.getParamCode();

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
                    return calculateHValue(standingbookId, paramCode, info.getMeasurementCode(), date, tankSettingsMap);

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
            return dataList.get(0).getFullValue();
        }
        return null;
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
            return dataList.get(0).getIncrementalValue();
        }
        return null;
    }

    /**
     * 计算H值：H=Δp/(ρg)
     */
    private BigDecimal calculateHValue(Long standingbookId, String paramCode, String measurementCode, LocalDateTime date,
                                       Map<String, PowerTankSettingsDO> tankSettingsMap) {
        // 获取储罐设置
        PowerTankSettingsDO tankSettings = tankSettingsMap.get(measurementCode);
        
        if (tankSettings == null || tankSettings.getDensity() == null || tankSettings.getGravityAcceleration() == null) {
            log.warn("储罐设置数据不完整，measurementCode: {}", measurementCode);
            return null;
        }
        
        // 获取Δp值（最后一分钟的full_value）
        BigDecimal deltaP = getLastMinuteFullValue(tankSettings.getPressureDiffId(), paramCode, date);

        if (deltaP == null || deltaP.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal density = tankSettings.getDensity();
        BigDecimal gravity = tankSettings.getGravityAcceleration();

        // 计算H = Δp/(ρg)
        if (density == null || gravity == null || density.compareTo(BigDecimal.ZERO) == 0 || gravity.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("密度或重力加速度为0，无法计算H值，standingbookId: {}", standingbookId);
            return null;
        }

        BigDecimal denominator = density.multiply(gravity);
        return deltaP.divide(denominator, scale, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 将PowerGasMeasurementDO转换为GasMeasurementInfo
     */
    private GasMeasurementInfo convertToGasMeasurementInfo(PowerGasMeasurementDO measurement) {
        GasMeasurementInfo info = new GasMeasurementInfo();
        info.setMeasurementCode(measurement.getMeasurementCode());
        info.setEnergyParam(measurement.getEnergyParam());
        info.setSortNo(measurement.getSortNo());
        info.setMeasurementName(measurement.getMeasurementName());
        
        // 直接查询台账属性信息，获取standingbookId
        List<StandingbookAttributeDO> attrs = standingbookAttributeMapper.selectList(
                new LambdaQueryWrapperX<StandingbookAttributeDO>()
                        .eq(StandingbookAttributeDO::getName, "计量器具编号")
                        .eq(StandingbookAttributeDO::getValue, measurement.getMeasurementCode())
                        .eq(StandingbookAttributeDO::getDeleted, false)
                        .orderByDesc(StandingbookAttributeDO::getCreateTime) // 按创建时间倒序，取最新的
        );
        
        StandingbookAttributeDO attr = null;
        if (!attrs.isEmpty()) {
            // 如果有多条记录，取最新的一条（按创建时间倒序）
            attr = attrs.get(0);
            if (attrs.size() > 1) {
                log.warn("计量器具 {} 存在{}条台账属性记录，使用最新的一条", measurement.getMeasurementCode(), attrs.size());
            }
        }
        
        if (attr != null && attr.getStandingbookId() != null) {
            info.setStandingbookId(attr.getStandingbookId());
            
            // 查询台账信息，获取typeId
            StandingbookDO standingbook = standingbookMapper.selectById(attr.getStandingbookId());
            if (standingbook != null && standingbook.getTypeId() != null) {
                info.setTypeId(standingbook.getTypeId());
                
                // 查询台账模板配置，获取paramCode
                StandingbookTmplDaqAttrDO tmplAttr = standingbookTmplDaqAttrMapper.selectOne(
                        new LambdaQueryWrapperX<StandingbookTmplDaqAttrDO>()
                                .eq(StandingbookTmplDaqAttrDO::getTypeId, standingbook.getTypeId())
                                .eq(StandingbookTmplDaqAttrDO::getParameter, measurement.getEnergyParam())
                                .eq(StandingbookTmplDaqAttrDO::getEnergyFlag, true)
                                .eq(StandingbookTmplDaqAttrDO::getDeleted, false)
                );
                
                if (tmplAttr != null) {
                    info.setParamCode(tmplAttr.getCode());
                } else {
                    log.warn("未找到计量器具 {} 的参数编码配置", measurement.getMeasurementCode());
                }
                
                // 根据data_feature和储罐设置确定计算类型
                Integer dataFeature = tmplAttr != null ? tmplAttr.getDataFeature() : null;
                if (dataFeature != null) {
                    if (dataFeature == 1) {
                        info.setCalculateType(1); // 累计值
                    } else if (dataFeature == 2) {
                        // 检查是否有储罐设置，使用计量器具编码查询
                        // 注意：这里暂时保留查询，因为tankSettingsMap可能还未构建
                        // 在gasStatisticsTable方法中会使用已构建的映射
                        PowerTankSettingsDO tankSetting = powerTankSettingsMapper.selectOne(
                                new LambdaQueryWrapperX<PowerTankSettingsDO>()
                                        .eq(PowerTankSettingsDO::getCode, measurement.getMeasurementCode())
                                        .eq(PowerTankSettingsDO::getDeleted, false)
                        );
                        info.setCalculateType(tankSetting != null ? 2 : 0); // 有储罐设置为液压，否则为稳态
                    } else {
                        info.setCalculateType(0); // 默认稳态值
                    }
                } else {
                    info.setCalculateType(0); // 默认稳态值
                }
                
            } else {
                log.warn("未找到计量器具 {} 对应的台账信息", measurement.getMeasurementCode());
            }
        } else {
            log.warn("未找到计量器具 {} 对应的台账属性", measurement.getMeasurementCode());
        }
        
        return info;
    }

    /**
     * 批量转换PowerGasMeasurementDO为GasMeasurementInfo，减少数据库查询次数
     */
    private List<GasMeasurementInfo> batchConvertToGasMeasurementInfo(List<PowerGasMeasurementDO> measurements) {
        if (CollUtil.isEmpty(measurements)) {
            return new ArrayList<>();
        }

        // 收集所有需要查询的计量器具编码
        List<String> measurementCodes = measurements.stream()
                .map(PowerGasMeasurementDO::getMeasurementCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 批量查询台账属性
        List<StandingbookAttributeDO> allAttrs = standingbookAttributeMapper.selectList(
                new LambdaQueryWrapperX<StandingbookAttributeDO>()
                        .eq(StandingbookAttributeDO::getName, "计量器具编号")
                        .in(StandingbookAttributeDO::getValue, measurementCodes)
                        .eq(StandingbookAttributeDO::getDeleted, false)
        );

        // 构建计量器具编码到台账属性的映射
        Map<String, StandingbookAttributeDO> attrMap = allAttrs.stream()
                .collect(Collectors.groupingBy(StandingbookAttributeDO::getValue))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .max(Comparator.comparing(StandingbookAttributeDO::getCreateTime))
                                .orElse(null)
                ));

        // 收集所有需要查询的台账ID
        List<Long> standingbookIds = attrMap.values().stream()
                .filter(Objects::nonNull)
                .map(StandingbookAttributeDO::getStandingbookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 批量查询台账信息
        Map<Long, StandingbookDO> standingbookMap = new HashMap<>();
        if (!standingbookIds.isEmpty()) {
            List<StandingbookDO> standingbooks = standingbookMapper.selectBatchIds(standingbookIds);
            standingbookMap = standingbooks.stream()
                    .collect(Collectors.toMap(StandingbookDO::getId, standingbook -> standingbook));
        }

        // 收集所有需要查询的typeId
        List<Long> typeIds = standingbookMap.values().stream()
                .map(StandingbookDO::getTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询台账模板配置
        Map<String, StandingbookTmplDaqAttrDO> tmplAttrMap = new HashMap<>();
        if (!typeIds.isEmpty()) {
            List<String> energyParams = measurements.stream()
                    .map(PowerGasMeasurementDO::getEnergyParam)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            List<StandingbookTmplDaqAttrDO> tmplAttrs = standingbookTmplDaqAttrMapper.selectList(
                    new LambdaQueryWrapperX<StandingbookTmplDaqAttrDO>()
                            .in(StandingbookTmplDaqAttrDO::getTypeId, typeIds)
                            .in(StandingbookTmplDaqAttrDO::getParameter, energyParams)
                            .eq(StandingbookTmplDaqAttrDO::getEnergyFlag, true)
                            .eq(StandingbookTmplDaqAttrDO::getDeleted, false)
            );

            tmplAttrMap = tmplAttrs.stream()
                    .collect(Collectors.toMap(
                            attr -> attr.getTypeId() + ":" + attr.getParameter(),
                            attr -> attr
                    ));
        }

        // 批量查询储罐设置
        Map<String, PowerTankSettingsDO> tankSettingsMap = new HashMap<>();
        if (!measurementCodes.isEmpty()) {
            List<PowerTankSettingsDO> tankSettings = powerTankSettingsMapper.selectList(
                    new LambdaQueryWrapperX<PowerTankSettingsDO>()
                            .in(PowerTankSettingsDO::getCode, measurementCodes)
                            .eq(PowerTankSettingsDO::getDeleted, false)
            );

            tankSettingsMap = tankSettings.stream()
                    .collect(Collectors.toMap(
                            PowerTankSettingsDO::getCode,
                            settings -> settings,
                            (v1, v2) -> v1
                    ));
        }

        // 转换结果
        List<GasMeasurementInfo> result = new ArrayList<>();
        for (PowerGasMeasurementDO measurement : measurements) {
            GasMeasurementInfo info = new GasMeasurementInfo();
            info.setMeasurementCode(measurement.getMeasurementCode());
            info.setEnergyParam(measurement.getEnergyParam());
            info.setSortNo(measurement.getSortNo());
            info.setMeasurementName(measurement.getMeasurementName());

            StandingbookAttributeDO attr = attrMap.get(measurement.getMeasurementCode());
            if (attr != null && attr.getStandingbookId() != null) {
                info.setStandingbookId(attr.getStandingbookId());

                StandingbookDO standingbook = standingbookMap.get(attr.getStandingbookId());
                if (standingbook != null && standingbook.getTypeId() != null) {
                    info.setTypeId(standingbook.getTypeId());

                    String tmplKey = standingbook.getTypeId() + ":" + measurement.getEnergyParam();
                    StandingbookTmplDaqAttrDO tmplAttr = tmplAttrMap.get(tmplKey);
                    if (tmplAttr != null) {
                        info.setParamCode(tmplAttr.getCode());

                        Integer dataFeature = tmplAttr.getDataFeature();
                        if (dataFeature != null) {
                            if (dataFeature == 1) {
                                info.setCalculateType(1); // 累计值
                            } else if (dataFeature == 2) {
                                PowerTankSettingsDO tankSetting = tankSettingsMap.get(measurement.getMeasurementCode());
                                info.setCalculateType(tankSetting != null ? 2 : 0);
                            } else {
                                info.setCalculateType(0); // 默认稳态值
                            }
                        } else {
                            info.setCalculateType(0);
                        }
                    } else {
                        log.warn("未找到计量器具 {} 的参数编码配置", measurement.getMeasurementCode());
                    }
                } else {
                    log.warn("未找到计量器具 {} 对应的台账信息", measurement.getMeasurementCode());
                }
            } else {
                log.warn("未找到计量器具 {} 对应的台账属性", measurement.getMeasurementCode());
            }

            result.add(info);
        }

        return result;
    }
}