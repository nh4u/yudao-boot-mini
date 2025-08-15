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
        
        log.info("开始查询气化科报表，时间范围: {} ~ {}, 计量器具编码: {}", 
                startTime, endTime, paramVO.getEnergyStatisticsItemCodes() != null ? 
                String.join(",", paramVO.getEnergyStatisticsItemCodes()) : "全部");
        
        // 生成缓存key，包含计量器具编码信息
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

        // 优化：根据传入的编码直接查询对应的计量器具信息，而不是先查43条再过滤
        List<String> measurementCodes = paramVO.getEnergyStatisticsItemCodes();
        List<GasMeasurementInfo> gasMeasurementInfos;
        
        if (CollUtil.isEmpty(measurementCodes)) {
            // 如果没有传入编码列表，返回所有数据
            gasMeasurementInfos = powerGasMeasurementService.getAllValidMeasurements().stream()
                    .map(this::convertToGasMeasurementInfo)
                    .collect(Collectors.toList());
            log.info("未指定计量器具编码，获取所有{}条计量器具信息", gasMeasurementInfos.size());
        } else {
            // 如果传入了编码列表，直接查询对应的数据
            gasMeasurementInfos = powerGasMeasurementService.getMeasurementsByCodes(measurementCodes).stream()
                    .map(this::convertToGasMeasurementInfo)
                    .collect(Collectors.toList());
            log.info("指定计量器具编码: {}, 查询到{}条计量器具信息", measurementCodes, gasMeasurementInfos.size());
        }

        if (CollUtil.isEmpty(gasMeasurementInfos)) {
            log.warn("未找到有效的计量器具配置");
            resultVO.setStatisticsInfoList(new ArrayList<>());
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }

        // 提取台账ID和参数编码
        List<Long> standingbookIds = gasMeasurementInfos.stream()
                .map(GasMeasurementInfo::getStandingbookId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> paramCodes = gasMeasurementInfos.stream()
                .map(GasMeasurementInfo::getParamCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        log.info("提取的台账ID: {}, 参数编码: {}", standingbookIds, paramCodes);

        // 如果没有有效的台账ID或参数编码，仍然要处理数据，只是数据值会为0
        if (CollUtil.isEmpty(standingbookIds) || CollUtil.isEmpty(paramCodes)) {
            log.warn("未找到有效的台账ID或参数编码，将返回{}条记录但数据值为0", gasMeasurementInfos.size());
        }

        // 对于液压计算类型，需要额外查询power_tank_settings表获取pressure_diff_id
        List<Long> pressureDiffIds = new ArrayList<>();
        Map<String, PowerTankSettingsDO> tankSettingsMap = new HashMap<>();
        
        if (!standingbookIds.isEmpty()) {
            // 获取所有计量器具编码
            List<String> mCodes = gasMeasurementInfos.stream()
                    .map(GasMeasurementInfo::getMeasurementCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            List<PowerTankSettingsDO> tankSettings = powerTankSettingsMapper.selectList(
                    new LambdaQueryWrapperX<PowerTankSettingsDO>()
                            .in(PowerTankSettingsDO::getCode, mCodes)
                            .eq(PowerTankSettingsDO::getDeleted, false)
            );
            
            // 构建储罐设置映射，避免后续重复查询
            tankSettingsMap = tankSettings.stream()
                    .filter(e -> e.getCode() != null)
                    .collect(Collectors.toMap(
                            PowerTankSettingsDO::getCode,
                            settings -> settings,
                            (v1, v2) -> v1 // 遇到重复key保留第一条
                    ));
            
            pressureDiffIds = tankSettings.stream()
                    .map(PowerTankSettingsDO::getPressureDiffId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            
            log.info("查询到{}条储罐设置，其中{}条有压差ID: {}", 
                    tankSettings.size(), pressureDiffIds.size(), pressureDiffIds);
        }

        // 合并所有需要查询的standingbook_id（包括压差ID）
        List<Long> allStandingbookIds = new ArrayList<>(standingbookIds);
        allStandingbookIds.addAll(pressureDiffIds);
        allStandingbookIds = allStandingbookIds.stream().distinct().collect(Collectors.toList());
        
        log.info("最终查询的台账ID列表: {} (原始: {}, 压差: {})", 
                allStandingbookIds, standingbookIds, pressureDiffIds);

        if (CollUtil.isEmpty(allStandingbookIds) || CollUtil.isEmpty(paramCodes)) {
            log.warn("台账ID或参数编码为空，返回空结果");
            return resultVO;
        }

        // 储罐设置数据已在前面查询并构建映射，无需重复查询
        // 如果tankSettingsMap为空，初始化为空Map
        if (tankSettingsMap == null) {
            tankSettingsMap = new HashMap<>();
        }

        // 生成日期列表（仅到日）。此处只构建 LocalDateTime 的零点时间，便于后续组装 key
        List<LocalDateTime> dateList = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DAY).stream()
                .map(dateStr -> LocalDateTime.parse(dateStr + " 00:00:00",
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .collect(Collectors.toList());

        // 预计算日期字符串格式，避免循环中重复格式化
        List<String> dateStrings = dateList.stream()
                .map(date -> date.format(DAY_FORMATTER))
                .collect(Collectors.toList());

        // 性能优化：批量查询所有数据
        // 一次性批量查询并构建数据缓存，避免逐条按天/设备/参数访问数据库
        Map<String, MinuteAggregateDataDO> dataCache = batchQueryData(
                allStandingbookIds, paramCodes, startTime, endTime);
        
        log.info("批量查询数据完成，缓存大小: {}", dataCache.size());
        if (!dataCache.isEmpty()) {
            // 输出前几条缓存数据用于调试
            dataCache.entrySet().stream().limit(3).forEach(entry -> 
                log.info("缓存数据示例 - Key: {}, Value: standingbookId={}, paramCode={}, fullValue={}, incrementalValue={}", 
                    entry.getKey(), 
                    entry.getValue().getStandingbookId(),
                    entry.getValue().getParamCode(),
                    entry.getValue().getFullValue(),
                    entry.getValue().getIncrementalValue()));
        } else {
            log.warn("⚠️ 数据缓存为空！这可能是问题的根源");
            log.warn("请检查以下SQL查询是否返回数据：");
            log.warn("SELECT COUNT(*) FROM minute_aggregate_data WHERE energy_flag=1;");
            log.error("SELECT COUNT(*) as total_count FROM minute_aggregate_data WHERE energy_flag=1;");
            log.error("SELECT COUNT(*) as filtered_count FROM minute_aggregate_data WHERE standingbook_id IN ({}) AND energy_flag=1;", 
                    allStandingbookIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }

        // 处理每个计量器具的数据
        List<GasStatisticsInfo> statisticsInfoList = new ArrayList<>();

        // 确保即使没有数据也要返回完整的结构
        if (CollUtil.isNotEmpty(gasMeasurementInfos)) {
            for (GasMeasurementInfo info : gasMeasurementInfos) {
                GasStatisticsInfo gasStatisticsInfo = new GasStatisticsInfo();
                gasStatisticsInfo.setMeasurementName(info.getMeasurementName());
                gasStatisticsInfo.setMeasurementCode(info.getMeasurementCode());

                List<GasStatisticsInfoData> statisticsDateDataList = new ArrayList<>();

                for (int i = 0; i < dateList.size(); i++) {
                    LocalDateTime date = dateList.get(i);
                    String dateStr = dateStrings.get(i);
                    
                    GasStatisticsInfoData data = new GasStatisticsInfoData();
                    data.setDate(dateStr);

                    // 获取当前计量器具的台账ID和参数编码
                    Long standingbookId = info.getStandingbookId();
                    String paramCode = info.getParamCode();

                    // 根据计算类型计算值
                    BigDecimal value;
                    
                    // 如果是液压计算类型，直接使用压差ID进行计算
                    if (info.getCalculateType() != null && info.getCalculateType() == 2) {
                        // 从已构建的映射中获取储罐设置，避免重复查询
                        PowerTankSettingsDO tankSetting = tankSettingsMap.get(info.getMeasurementCode());
                        
                        if (tankSetting != null && tankSetting.getPressureDiffId() != null) {
                            log.debug("液压计算类型 - 计量器具: {}, 台账ID: {}, 压差ID: {}", 
                                    info.getMeasurementCode(), standingbookId, tankSetting.getPressureDiffId());
                            
                            // 对于液压计算类型，直接使用压差ID进行计算
                            // 重新构建一个临时的GasMeasurementInfo，使用压差ID
                            GasMeasurementInfo tempInfo = new GasMeasurementInfo();
                            tempInfo.setStandingbookId(tankSetting.getPressureDiffId());
                            tempInfo.setParamCode(paramCode);
                            tempInfo.setCalculateType(info.getCalculateType());
                            tempInfo.setMeasurementCode(info.getMeasurementCode());
                            
                            // 直接使用压差ID计算值
                            value = calculateValueByTypeOptimized(tempInfo, date, dataCache, tankSettingsMap);
                            
                            if (value.compareTo(BigDecimal.ZERO) != 0) {
                                log.debug("使用压差ID计算成功 - 压差ID: {}, 值: {}", tankSetting.getPressureDiffId(), value);
                            } else {
                                log.debug("使用压差ID计算完成但结果为0 - 压差ID: {}", tankSetting.getPressureDiffId());
                            }
                        } else {
                            log.warn("液压计算类型但未找到储罐设置或压差ID - 计量器具: {}, 台账ID: {}", 
                                    info.getMeasurementCode(), standingbookId);
                            // 如果没有压差ID，返回0
                            value = BigDecimal.ZERO;
                        }
                    } else {
                        // 非液压计算类型，使用计量器具自身的standingbook_id进行计算
                        value = calculateValueByTypeOptimized(info, date, dataCache, tankSettingsMap);
                    }

                    data.setValue(value.setScale(2, RoundingMode.HALF_UP));

                    statisticsDateDataList.add(data);
                }

                gasStatisticsInfo.setStatisticsDateDataList(statisticsDateDataList);
                statisticsInfoList.add(gasStatisticsInfo);
            }
        }

        resultVO.setStatisticsInfoList(statisticsInfoList);
        resultVO.setDataTime(LocalDateTime.now());

        // 缓存结果
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        // 缓存时间，提升重复查询的复用率
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        log.info("气化科报表查询完成，返回{}条统计数据", statisticsInfoList.size());
        return resultVO;
    }

    /**
     * 批量查询数据并构建缓存
     */
    private Map<String, MinuteAggregateDataDO> batchQueryData(
            List<Long> standingbookIds,
            List<String> paramCodes,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.info("🔍 开始批量查询数据 - standingbookIds: {}, paramCodes: {}, 时间范围: {} ~ {}", 
                standingbookIds, paramCodes, startTime, endTime);

        if (CollUtil.isEmpty(standingbookIds) || CollUtil.isEmpty(paramCodes)) {
            log.warn("❌ 台账ID或参数编码为空，返回空缓存");
            return new HashMap<>();
        }

        // 一次性查询所有最后一分钟数据
        log.info("📊 开始查询最后一分钟数据...");
        List<MinuteAggregateDataDO> lastMinuteData = minuteAggregateDataService
                .selectLastMinuteDataByDateBatch(standingbookIds, paramCodes, startTime, endTime);
        log.info("✅ 查询最后一分钟数据完成，结果数量: {}", lastMinuteData.size());
        
        if (!lastMinuteData.isEmpty()) {
            log.info("📋 最后一分钟数据示例:");
            lastMinuteData.stream().limit(3).forEach(data -> 
                log.info("  - standingbookId: {}, paramCode: {}, aggregateTime: {}, fullValue: {}, energyFlag: {}", 
                    data.getStandingbookId(), data.getParamCode(), data.getAggregateTime(), 
                    data.getFullValue(), data.getEnergyFlag()));
        }

        // 一次性查询所有增量数据
        log.info("📊 开始查询增量数据...");
        List<MinuteAggregateDataDO> incrementalData = minuteAggregateDataService
                .selectIncrementalSumByDateBatch(standingbookIds, paramCodes, startTime, endTime);
        log.info("✅ 查询增量数据完成，结果数量: {}", incrementalData.size());
        
        if (!incrementalData.isEmpty()) {
            log.info("📋 增量数据示例:");
            incrementalData.stream().limit(3).forEach(data -> 
                log.info("  - standingbookId: {}, paramCode: {}, aggregateTime: {}, incrementalValue: {}, energyFlag: {}", 
                    data.getStandingbookId(), data.getParamCode(), data.getAggregateTime(), 
                    data.getIncrementalValue(), data.getEnergyFlag()));
        }

        // 构建缓存Map，key为 "standingbookId:paramCode:date"
        Map<String, MinuteAggregateDataDO> dataCache = new HashMap<>();

        // 处理最后一分钟数据
        for (MinuteAggregateDataDO data : lastMinuteData) {
            String key = String.format("%d:%s:%s",
                    data.getStandingbookId(),
                    data.getParamCode(),
                    data.getAggregateTime().toLocalDate());
            dataCache.put(key, data);
            log.debug("➕ 添加最后一分钟数据到缓存 - Key: {}, standingbookId: {}, paramCode: {}, fullValue: {}", 
                    key, data.getStandingbookId(), data.getParamCode(), data.getFullValue());
        }

        // 处理增量数据
        for (MinuteAggregateDataDO data : incrementalData) {
            String key = String.format("%d:%s:%s:incremental",
                    data.getStandingbookId(),
                    data.getParamCode(),
                    data.getAggregateTime().toLocalDate());
            dataCache.put(key, data);
            log.debug("➕ 添加增量数据到缓存 - Key: {}, standingbookId: {}, paramCode: {}, incrementalValue: {}", 
                    key, data.getStandingbookId(), data.getParamCode(), data.getIncrementalValue());
        }

        log.info("🎯 数据缓存构建完成，总缓存条目数: {}", dataCache.size());
        
        // 如果没有数据，输出详细的调试信息
        if (dataCache.isEmpty()) {
            log.error("❌ 数据缓存为空！可能的原因：");
            log.error("1. 台账ID不匹配 - 检查power_standingbook表中的code字段");
            log.error("2. 参数编码不匹配 - 检查power_standingbook_tmpl_daq_attr表中的配置");
            log.error("3. 时间范围问题 - 检查查询时间是否覆盖数据时间");
            log.error("4. 数据源问题 - 检查@DS('starrocks')注解和数据源配置");
            log.error("5. energy_flag问题 - 检查minute_aggregate_data表中的energy_flag字段");
            
            // 输出建议的SQL查询语句
            log.error("🔍 建议执行以下SQL查询来验证数据：");
            log.error("SELECT COUNT(*) as total_count FROM minute_aggregate_data WHERE energy_flag=1;");
            log.error("SELECT COUNT(*) as filtered_count FROM minute_aggregate_data WHERE standingbook_id IN ({}) AND energy_flag=1;", 
                    standingbookIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
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

        log.debug("开始计算值 - 计量器具: {}, standingbookId: {}, paramCode: {}, calculateType: {}, 日期: {}", 
                info.getMeasurementCode(), standingbookId, paramCode, calculateType, date);

        // 如果 standingbookId 为 null 或 paramCode 为 null，返回0
        if (standingbookId == null || paramCode == null) {
            log.debug("台账ID或参数编码为空，返回0 - standingbookId: {}, paramCode: {}", standingbookId, paramCode);
            return BigDecimal.ZERO;
        }

        // 如果calculateType为null，返回0
        if (calculateType == null) {
            log.debug("计算类型为空，返回0");
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal result = BigDecimal.ZERO;
            switch (calculateType) {
                case 0:
                    // 取得今天有数据的最后一分钟的数值full_value
                    result = getLastMinuteFullValueOptimized(standingbookId, paramCode, date, dataCache);
                    log.debug("计算类型0(稳态值) - 结果: {}", result);
                    break;

                case 1:
                    // 取得今天所有increment_value值之和
                    result = getIncrementalSumOptimized(standingbookId, paramCode, date, dataCache);
                    log.debug("计算类型1(累计值) - 结果: {}", result);
                    break;

                case 2:
                    // 取得今天有数据的最后一分钟的数值full_value，带入到公式H=Δp/(ρg)求出的H值
                    result = calculateHValueOptimized(standingbookId, paramCode, info.getMeasurementCode(), date, dataCache, tankSettingsMap);
                    log.debug("计算类型2(液压值) - 结果: {}", result);
                    break;

                default:
                    log.debug("未知计算类型: {}, 返回0", calculateType);
                    result = BigDecimal.ZERO;
                    break;
            }
            
            log.debug("计算完成 - 计量器具: {}, 结果: {}", info.getMeasurementCode(), result);
            return result;
            
        } catch (Exception e) {
            log.error("计算值失败，standingbookId: {}, paramCode: {}, date: {}, calculateType: {}",
                    standingbookId, paramCode, date, calculateType, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 优化后的获取最后一分钟full_value方法
     */
    private BigDecimal getLastMinuteFullValueOptimized(Long standingbookId, String paramCode, LocalDateTime date, Map<String, MinuteAggregateDataDO> dataCache) {
        if (standingbookId == null || paramCode == null) {
            log.debug("❌ 台账ID或参数编码为空，返回0 - standingbookId: {}, paramCode: {}", standingbookId, paramCode);
            return BigDecimal.ZERO;
        }
        
        String key = String.format("%d:%s:%s", standingbookId, paramCode, date.toLocalDate());
        log.debug("🔍 查找缓存键: {}", key);
        
        MinuteAggregateDataDO data = dataCache.get(key);
        if (data != null && data.getFullValue() != null) {
            log.debug("✅ 找到缓存数据 - Key: {}, fullValue: {}", key, data.getFullValue());
            return data.getFullValue();
        } else {
            log.debug("❌ 未找到缓存数据 - Key: {}, data存在: {}, fullValue: {}", 
                    key, data != null, data != null ? data.getFullValue() : "N/A");
            return BigDecimal.ZERO;
        }
    }

    /**
     * 优化后的获取增量值之和方法
     */
    private BigDecimal getIncrementalSumOptimized(Long standingbookId, String paramCode, LocalDateTime date, Map<String, MinuteAggregateDataDO> dataCache) {
        if (standingbookId == null || paramCode == null) {
            log.debug("❌ 台账ID或参数编码为空，返回0 - standingbookId: {}, paramCode: {}", standingbookId, paramCode);
            return BigDecimal.ZERO;
        }
        
        String key = String.format("%d:%s:%s:incremental", standingbookId, paramCode, date.toLocalDate());
        log.debug("🔍 查找增量缓存键: {}", key);
        
        MinuteAggregateDataDO data = dataCache.get(key);
        if (data != null && data.getIncrementalValue() != null) {
            log.debug("✅ 找到增量缓存数据 - Key: {}, incrementalValue: {}", key, data.getIncrementalValue());
            return data.getIncrementalValue();
        } else {
            log.debug("❌ 未找到增量缓存数据 - Key: {}, data存在: {}, incrementalValue: {}", 
                    key, data != null, data != null ? data.getIncrementalValue() : "N/A");
            return BigDecimal.ZERO;
        }
    }

    /**
     * 优化后的计算H值方法
     */
        private BigDecimal calculateHValueOptimized(Long standingbookId, String paramCode, String measurementCode, LocalDateTime date,
                                                 Map<String, MinuteAggregateDataDO> dataCache,
                                                 Map<String, PowerTankSettingsDO> tankSettingsMap) {
        if (standingbookId == null || paramCode == null || measurementCode == null) {
            return BigDecimal.ZERO;
        }

        // 获取储罐设置 - 通过计量器具编码查找
        PowerTankSettingsDO tankSettings = tankSettingsMap.get(measurementCode);
        
        if (tankSettings == null || tankSettings.getPressureDiffId() == null) {
            log.warn("储罐设置数据不完整，measurementCode: {}", measurementCode);
            return BigDecimal.ZERO;
        }

        // 获取Δp值（最后一分钟的full_value）
        BigDecimal deltaP = getLastMinuteFullValueOptimized(tankSettings.getPressureDiffId(), paramCode, date, dataCache);

        if (deltaP.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (tankSettings.getDensity() == null || tankSettings.getGravityAcceleration() == null) {
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
    private BigDecimal calculateHValue(Long standingbookId, String paramCode, String measurementCode, LocalDateTime date,
                                       Map<String, PowerTankSettingsDO> tankSettingsMap) {
        // 获取储罐设置
        PowerTankSettingsDO tankSettings = tankSettingsMap.get(measurementCode);
        
        if (tankSettings == null || tankSettings.getDensity() == null || tankSettings.getGravityAcceleration() == null) {
            log.warn("储罐设置数据不完整，measurementCode: {}", measurementCode);
            return BigDecimal.ZERO;
        }
        
        // 获取Δp值（最后一分钟的full_value）
        BigDecimal deltaP = getLastMinuteFullValue(tankSettings.getPressureDiffId(), paramCode, date);

        if (deltaP.compareTo(BigDecimal.ZERO) == 0) {
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
                    log.debug("找到计量器具 {} 的参数编码: {}", measurement.getMeasurementCode(), tmplAttr.getCode());
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
}