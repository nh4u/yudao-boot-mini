package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.EnergyClassifyEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsRespVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigDTO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.BigScreenCopChartData;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerMonthPlanSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.bigscreen.PowerPureWasteWaterGasSettingsMapper;
import cn.bitlinks.ems.module.power.enums.standingbook.StandingBookStageEnum;
import cn.bitlinks.ems.module.power.service.chemicals.PowerChemicalsSettingsService;
import cn.bitlinks.ems.module.power.service.collectrawdata.CollectRawDataService;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.production.ProductionService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsCommonService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.bitlinks.ems.module.power.utils.CommonUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.util.ListUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.bitlinks.ems.framework.common.enums.CommonConstants.*;
import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.PARK_FLAG_NOT_EXISTS;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class BigScreenServiceImpl implements BigScreenService {

    @Resource
    private CopHourAggDataService copHourAggDataService;

    @Resource
    private CollectRawDataService collectRawDataService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private PowerPureWasteWaterGasSettingsMapper powerPureWasteWaterGasSettingsMapper;

    @Resource
    private PowerMonthPlanSettingsService powerMonthPlanSettingsService;

    @Resource
    private ProductionService productionService;

    @Resource
    private PowerChemicalsSettingsService powerChemicalsSettingsService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private StatisticsCommonService statisticsCommonService;

    @Value("${spring.profiles.active}")
    private String env;


    @Override
    public BigScreenRespVO getBigScreenDetails(BigScreenParamReqVO paramVO) {

        BigScreenRespVO resultVO = new BigScreenRespVO();

        // 1. 中部
        // 中1 4#宿舍楼
        // 中2 2#生产厂房
        // 中3 3#办公楼
        // 中4 5#CUB
        // 中5 1#生产厂房

        // 2. 右部
        // 2.1. 右1 室外工况
        OutsideEnvData outsideEnvData = getOutsideEnvData(paramVO);
        resultVO.setOutside(outsideEnvData);

        // 2.2. 右2 获取cop数据
        BigScreenCopChartData copChart = getCopChartData(paramVO);
        resultVO.setCop(copChart);

        // 2.3. 右3 纯废水单价
        BigScreenChartData pureWasteWaterChart = getPureWasteWaterChart(paramVO);
        resultVO.setPureWasteWater(pureWasteWaterChart);


        // 2.4. 右4 压缩空气单价
        BigScreenChartData compressedGasChart = getCompressedGasChart(paramVO);
        resultVO.setGas(compressedGasChart);

        // 3. 底部
        // 3.1. 单位产品综合能耗
        ProductionFifteenDayResultVO recentFifteenDayProduction = getRecentFifteenDayProduction(paramVO);
        resultVO.setProductConsumption(recentFifteenDayProduction);

        // 4. 顶部
        // 4.1. 今日能耗 本月能耗
        BannerResultVO bannerResultVO = getBannerData(paramVO);
        resultVO.setBannerResultVO(bannerResultVO);

        // 5. 电力 天然气  自来水 高品质再生水 热力
        List<RecentSevenDayResultVO> recentSevenDayList = getRecentSevenDay(paramVO);
        resultVO.setRecentSevenDayList(recentSevenDayList);

        return resultVO;
    }


    /**
     * 获取实时室外工况数据
     *
     * @param paramVO
     * @return
     */
    @Override
    public OutsideEnvData getRealOutsideEnvData(BigScreenParamReqVO paramVO) {
        OutsideEnvData outsideEnvData = new OutsideEnvData();
        List<String> ioAddresses = Arrays.asList(
                WIND_DIRECTION_VALUE_IO,
                WIND_DIRECTION_NE_IO,
                WIND_DIRECTION_NW_IO,
                WIND_DIRECTION_SE_IO,
                WIND_DIRECTION_SW_IO,
                WIND_SPEED_IO,
                TEMPERATURE_IO,
                HUMIDITY_IO,
                DEW_POINT_IO,
                ATMOSPHERIC_PRESSURE_IO,
                NOISE_IO);


        if (env.equals(SPRING_PROFILES_ACTIVE_LOCAL) || env.equals(SPRING_PROFILES_ACTIVE_DEV) || env.equals(SPRING_PROFILES_ACTIVE_TEST)) {
            return outsideEnvData;
        }

        String host = "172.16.150.34";
        String user = "Administrator";
        String password = "12345678";
        String clsid = "7BC0CC8E-482C-47CA-ABDC-0FE7F9C6E729";
        // 执行OPC数据采集
        Map<String, ItemStatus> result = OpcDaUtils.readOnly(host, user, password, clsid, ioAddresses);
        if (CollUtil.isEmpty(result)) {
            return outsideEnvData;
        }

        for (Map.Entry<String, ItemStatus> entry : result.entrySet()) {
            String dataSite = entry.getKey();
            String value = entry.getValue().getValue();
            if (CharSequenceUtil.isNotBlank(value)) {
                switch (dataSite) {
                    case WIND_DIRECTION_VALUE_IO:
                        outsideEnvData.setWindDirectionValue(new BigDecimal(value));
                        break;
                    case WIND_DIRECTION_NE_IO:
                        if ("true".equals(value)) {
                            outsideEnvData.setWindDirection("东北");
                        }
                        break;
                    case WIND_DIRECTION_NW_IO:
                        if ("true".equals(value)) {
                            outsideEnvData.setWindDirection("西北");
                        }
                        break;
                    case WIND_DIRECTION_SE_IO:
                        if ("true".equals(value)) {
                            outsideEnvData.setWindDirection("东南");
                        }
                        break;
                    case WIND_DIRECTION_SW_IO:
                        if ("true".equals(value)) {
                            outsideEnvData.setWindDirection("西南");
                        }
                        break;
                    case WIND_SPEED_IO:
                        outsideEnvData.setWindSpeed(new BigDecimal(value));
                        break;
                    case TEMPERATURE_IO:
                        outsideEnvData.setTemperature(new BigDecimal(value));
                        break;
                    case HUMIDITY_IO:
                        outsideEnvData.setHumidity(new BigDecimal(value));
                        break;
                    case DEW_POINT_IO:
                        outsideEnvData.setDewPoint(new BigDecimal(value));
                        break;
                    case ATMOSPHERIC_PRESSURE_IO:
                        outsideEnvData.setAtmosphericPressure(new BigDecimal(value));
                        break;
                    case NOISE_IO:
                        outsideEnvData.setNoise(new BigDecimal(value));
                        break;
                    default:
                        break;
                }
            }
        }
        return outsideEnvData;
    }

    /**
     * 获取室外工况
     *
     * @param paramVO
     * @return
     */
    @Override
    public OutsideEnvData getOutsideEnvData(BigScreenParamReqVO paramVO) {
        OutsideEnvData outsideEnvData = new OutsideEnvData();
        List<String> dataSites = Arrays.asList(
                WIND_DIRECTION_VALUE_IO,
                WIND_DIRECTION_NE_IO,
                WIND_DIRECTION_NW_IO,
                WIND_DIRECTION_SE_IO,
                WIND_DIRECTION_SW_IO,
                WIND_SPEED_IO,
                TEMPERATURE_IO,
                HUMIDITY_IO,
                DEW_POINT_IO,
                ATMOSPHERIC_PRESSURE_IO,
                NOISE_IO);
        List<CollectRawDataDO> outsideDataList = collectRawDataService.getOutsideDataByDataSite(dataSites);
        if (CollUtil.isNotEmpty(outsideDataList)) {
            Map<String, CollectRawDataDO> outsideDataMap = outsideDataList
                    .stream()
                    .collect(Collectors.toMap(CollectRawDataDO::getDataSite, Function.identity(), (existing, replacement) -> existing));

            outsideEnvData.setWindDirection(dealWindDirection(outsideDataMap));

            CollectRawDataDO windDirection = outsideDataMap.get(WIND_DIRECTION_VALUE_IO);
            outsideEnvData.setWindDirectionValue(Objects.nonNull(windDirection) ? new BigDecimal(windDirection.getRawValue()) : null);

            CollectRawDataDO windSpeed = outsideDataMap.get(WIND_SPEED_IO);
            outsideEnvData.setWindSpeed(Objects.nonNull(windSpeed) ? new BigDecimal(windSpeed.getRawValue()) : null);

            CollectRawDataDO temperature = outsideDataMap.get(TEMPERATURE_IO);
            outsideEnvData.setTemperature(Objects.nonNull(temperature) ? new BigDecimal(temperature.getRawValue()) : null);

            CollectRawDataDO humidity = outsideDataMap.get(HUMIDITY_IO);
            outsideEnvData.setHumidity(Objects.nonNull(humidity) ? new BigDecimal(humidity.getRawValue()) : null);

            CollectRawDataDO dewPoint = outsideDataMap.get(DEW_POINT_IO);
            outsideEnvData.setDewPoint(Objects.nonNull(dewPoint) ? new BigDecimal(dewPoint.getRawValue()) : null);

            CollectRawDataDO atmosphericPressure = outsideDataMap.get(ATMOSPHERIC_PRESSURE_IO);
            outsideEnvData.setAtmosphericPressure(Objects.nonNull(atmosphericPressure) ? new BigDecimal(atmosphericPressure.getRawValue()) : null);

            CollectRawDataDO noise = outsideDataMap.get(NOISE_IO);
            outsideEnvData.setNoise(Objects.nonNull(noise) ? new BigDecimal(noise.getRawValue()) : null);

        }
        return outsideEnvData;
    }

    private String dealWindDirection(Map<String, CollectRawDataDO> outsideDataMap) {

        String windDirection = null;

        CollectRawDataDO ne = outsideDataMap.get(WIND_DIRECTION_NE_IO);
        CollectRawDataDO nw = outsideDataMap.get(WIND_DIRECTION_NW_IO);
        CollectRawDataDO se = outsideDataMap.get(WIND_DIRECTION_SE_IO);
        CollectRawDataDO sw = outsideDataMap.get(WIND_DIRECTION_SW_IO);

        List<CollectRawDataDO> list = new ArrayList<>();

        if (Objects.nonNull(ne)) {
            list.add(ne);
        }
        if (Objects.nonNull(nw)) {
            list.add(nw);
        }
        if (Objects.nonNull(se)) {
            list.add(se);
        }
        if (Objects.nonNull(sw)) {
            list.add(sw);
        }
        CollectRawDataDO collectRawDataDO = list
                .stream()
                .max(Comparator.comparing(CollectRawDataDO::getSyncTime))
                .orElse(null);

        windDirection = Objects.nonNull(collectRawDataDO) ? collectRawDataDO.getRawValue() : null;
        return windDirection;
    }


    /**
     * 获取banner
     *
     * @param paramVO
     * @return
     */
    @Override
    public BannerResultVO getBannerData(BigScreenParamReqVO paramVO) {

        BannerResultVO resultVO = new BannerResultVO();
        // 4. 顶部
        // 4.1. 今日能耗 本月能耗
        List<PowerMonthPlanSettingsDO> powerMonthPlanList = powerMonthPlanSettingsService.selectList();
        if (CollUtil.isEmpty(powerMonthPlanList)) {
            return resultVO;
        }
        List<String> energyCodes = powerMonthPlanList
                .stream()
                .map(PowerMonthPlanSettingsDO::getEnergyCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyCodes(energyCodes);
        if (CollUtil.isEmpty(energyList)) {
            return resultVO;
        }

        List<Long> energyIds = energyList
                .stream()
                .map(EnergyConfigurationDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, EnergyConfigurationDO> energyMap = energyList
                .stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getCode, Function.identity()));


        List<Long> stageSbIds = new ArrayList<>();

        for (EnergyConfigurationDO energy : energyList) {

            String code = energy.getCode();
            if ("W_Reclaimed Water".equals(code)) {
                // 园区
                List<Long> sbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                        StandingBookStageEnum.PROCESSING_CONVERSION.getCode(),
                        true,
                        Collections.singletonList(energy.getId()));
                stageSbIds.addAll(sbIds);
            } else {
                // 外购
                List<Long> sbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                        StandingBookStageEnum.PROCUREMENT_STORAGE.getCode(),
                        false,
                        Collections.singletonList(energy.getId()));
                stageSbIds.addAll(sbIds);
            }
        }

        // 当期
        // 今日
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime beginOfDay = LocalDateTimeUtils.beginOfDay(now);
        LocalDateTime endOfDay = LocalDateTimeUtils.endOfDay(now);
        List<UsageCostData> todayData = usageCostService.getEnergyStandardCoalCostBySbIds(
                beginOfDay,
                endOfDay,
                stageSbIds);


        // 本月
        LocalDateTime beginOfMonth = LocalDateTimeUtils.beginOfMonth(now);
        LocalDateTime endOfMonth = LocalDateTimeUtils.endOfMonth(now);
        List<UsageCostData> monthData = usageCostService.getEnergyStandardCoalCostBySbIds(
                beginOfMonth,
                endOfMonth,
                stageSbIds);

        // 上期
        // 上日
        List<UsageCostData> yesterdayData = usageCostService.getEnergyStandardCoalCostBySbIds(
                beginOfDay.minusDays(1),
                endOfDay.minusDays(1),
                stageSbIds);

        // 上月
        List<UsageCostData> lastMonthData = usageCostService.getEnergyStandardCoalCostBySbIds(
                beginOfMonth.minusMonths(1),
                endOfMonth.minusMonths(1),
                stageSbIds);


        Map<Long, UsageCostData> todayDataMap = dealUsageCostDataMap(todayData);
        Map<Long, UsageCostData> yesterdayDataMap = dealUsageCostDataMap(yesterdayData);
        Map<Long, UsageCostData> monthDataMap = dealUsageCostDataMap(monthData);
        Map<Long, UsageCostData> lastMonthDataMap = dealUsageCostDataMap(lastMonthData);

        List<BannerData> todayDataList = CollUtil.newArrayList();
        List<BannerData> monthDataList = CollUtil.newArrayList();

        powerMonthPlanList.forEach(p -> {

            // 今日
            BannerData today = dealBannerData(p, energyMap, todayDataMap, yesterdayDataMap);
            todayDataList.add(today);

            // 本月
            BannerData month = dealBannerData(p, energyMap, monthDataMap, lastMonthDataMap);
            BigDecimal plan = p.getPlan();
            BigDecimal consumption = month.getConsumption();
            month.setProgress(getProportion(consumption, plan));
            monthDataList.add(month);
        });

        resultVO.setToday(todayDataList);
        resultVO.setMonth(monthDataList);

        return resultVO;
    }


    private Map<Long, UsageCostData> dealUsageCostDataMap(List<UsageCostData> list) {
        return list
                .stream()
                .collect(Collectors.toMap(UsageCostData::getEnergyId, Function.identity()));
    }


    private BannerData dealBannerData(PowerMonthPlanSettingsDO p,
                                      Map<String, EnergyConfigurationDO> energyMap,
                                      Map<Long, UsageCostData> todayDataMap,
                                      Map<Long, UsageCostData> yesterdayDataMap) {
        BannerData bannerData = new BannerData();
        bannerData.setUnit(p.getEnergyUnit());
        bannerData.setName(p.getEnergyName());

        EnergyConfigurationDO energy = energyMap.get(p.getEnergyCode());
        if (Objects.nonNull(energy)) {
            Long energyId = energy.getId();
            bannerData.setEnergyIcon(energy.getEnergyIcon());
            if (Objects.nonNull(todayDataMap)) {
                UsageCostData today = todayDataMap.get(energyId);
                if (Objects.nonNull(today)) {
                    bannerData.setConsumption(dealBigDecimalScale(today.getCurrentTotalUsage(), DEFAULT_SCALE));
                    bannerData.setStandardCoal(dealBigDecimalScale(today.getTotalStandardCoalEquivalent(), DEFAULT_SCALE));

                    if (Objects.nonNull(yesterdayDataMap)) {
                        UsageCostData yesterday = yesterdayDataMap.get(energyId);
                        if (Objects.nonNull(yesterday)) {
                            bannerData.setRatio(calculateYearOnYearRatio(today.getTotalStandardCoalEquivalent(), yesterday.getTotalStandardCoalEquivalent()));
                        }
                    }
                }
            }
        }

        return bannerData;
    }

    /**
     * 获得近7日能源数据
     *
     * @param paramVO
     * @return
     */
    @Override
    public List<RecentSevenDayResultVO> getRecentSevenDay(BigScreenParamReqVO paramVO) {

        List<RecentSevenDayResultVO> resultVOList = CollUtil.newArrayList();
        List<PowerMonthPlanSettingsDO> powerMonthPlanList = powerMonthPlanSettingsService.selectList();
        if (CollUtil.isEmpty(powerMonthPlanList)) {
            return resultVOList;
        }
        List<String> energyCodes = powerMonthPlanList
                .stream()
                .map(PowerMonthPlanSettingsDO::getEnergyCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyCodes(energyCodes);
        if (CollUtil.isEmpty(energyList)) {
            return resultVOList;
        }

        List<Long> energyIds = energyList
                .stream()
                .map(EnergyConfigurationDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, EnergyConfigurationDO> energyMap = energyList
                .stream()
                .collect(Collectors.toMap(EnergyConfigurationDO::getCode, Function.identity()));

        List<Long> stageSbIds = new ArrayList<>();

        for (EnergyConfigurationDO energy : energyList) {

            String code = energy.getCode();
            if ("W_Reclaimed Water".equals(code)) {
                // 园区
                List<Long> sbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                        StandingBookStageEnum.PROCESSING_CONVERSION.getCode(),
                        true,
                        Collections.singletonList(energy.getId()));
                stageSbIds.addAll(sbIds);
            } else {
                // 外购
                List<Long> sbIds = statisticsCommonService.getStageEnergySbIdsByEnergyIds(
                        StandingBookStageEnum.PROCUREMENT_STORAGE.getCode(),
                        false,
                        Collections.singletonList(energy.getId()));
                stageSbIds.addAll(sbIds);
            }
        }


        // 最近七天
        LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(6L);
        LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();

        // 4.1.x轴处理
        List<String> tempXData = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DataTypeEnum.DAY);

        List<String> xdata = LocalDateTimeUtils.getBigScreenTimeRangeList(startTime, endTime, DataTypeEnum.DAY);

        List<UsageCostData> usageCostDataList = usageCostService.getEnergyUsageBySbIds(
                DataTypeEnum.DAY.getCode(),
                startTime,
                endTime,
                stageSbIds);

        Map<Long, List<UsageCostData>> energyTimeUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getEnergyId));

        powerMonthPlanList.forEach(p -> {
            RecentSevenDayResultVO resultVO = new RecentSevenDayResultVO();

            EnergyConfigurationDO energy = energyMap.get(p.getEnergyCode());
            if (Objects.nonNull(energy)) {
                Long energyId = energy.getId();
                List<UsageCostData> usageCostDatas = energyTimeUsageMap.get(energyId);
                resultVO.setY(dealYData(usageCostDatas, tempXData));
                resultVO.setEnergyIcon(energy.getEnergyIcon());
            }
            resultVO.setX(xdata);
            resultVO.setName(p.getEnergyName());
            resultVO.setUnit(p.getEnergyUnit());

            resultVOList.add(resultVO);

        });
        return resultVOList;

    }


    /**
     * 获得近15日产品数据
     *
     * @param paramVO
     * @return
     */
    @Override
    public ProductionFifteenDayResultVO getRecentFifteenDayProduction(BigScreenParamReqVO paramVO) {
        ProductionFifteenDayResultVO resultVO = new ProductionFifteenDayResultVO();

        // 最近15天
        LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(14L);
        LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();

        // 4.1.x轴处理
        List<String> tempXData = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DataTypeEnum.DAY);

        List<String> xdata = LocalDateTimeUtils.getBigScreenTimeRangeList(startTime, endTime, DataTypeEnum.DAY);


        List<ProductionDO> productionList = productionService.getBigScreenProduction(startTime, endTime);

        if (CollUtil.isEmpty(productionList)) {
            return resultVO;
        }

        Map<Integer, List<ProductionDO>> productionSizeMap = productionList
                .stream()
                .collect(Collectors.groupingBy(ProductionDO::getSize));

        List<ProductionDO> eightList = ListUtils.newArrayList();
        if (CollUtil.isNotEmpty(productionSizeMap.get(8))) {
            eightList = productionSizeMap.get(8);
        }
        Map<String, BigDecimal> eightTimeMap = eightList
                .stream()
                .collect(Collectors.toMap(ProductionDO::getStrTime, ProductionDO::getValue));

        List<ProductionDO> twelveList = ListUtils.newArrayList();
        if (CollUtil.isNotEmpty(productionSizeMap.get(12))) {
            twelveList = productionSizeMap.get(12);
        }
        Map<String, BigDecimal> twelveTimeMap = twelveList
                .stream()
                .collect(Collectors.toMap(ProductionDO::getStrTime, ProductionDO::getValue));

        // 综合能耗
        // 能源处理 外购

        List<Long> stageSbIds = statisticsCommonService.getStageEnergySbIds(
                StandingBookStageEnum.PROCUREMENT_STORAGE.getCode(),
                false,
                EnergyClassifyEnum.OUTSOURCED);


//        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(1);
//        if (CollUtil.isEmpty(energyList)) {
//            return resultVO;
//        }
//        List<Long> energyIdList = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());


        // 外购总能耗
        List<UsageCostData> usageCostDataList = usageCostService.getTimeStandardCoalByStandardIds(DataTypeEnum.DAY.getCode(), startTime, endTime, stageSbIds);
        if (CollUtil.isEmpty(stageSbIds)) {
            return resultVO;
        }
        Map<String, BigDecimal> usageCostDataMap = usageCostDataList.stream()
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getTotalStandardCoalEquivalent));

        List<BigDecimal> production8 = ListUtils.newArrayList();
        List<BigDecimal> production12 = ListUtils.newArrayList();

        tempXData.forEach(time -> {

            BigDecimal sum = null;
            BigDecimal eightValue = eightTimeMap.get(time);
            BigDecimal twelveValue = twelveTimeMap.get(time);
            sum = CommonUtil.addBigDecimal(eightValue, twelveValue);

            BigDecimal energySumStandardCoal = usageCostDataMap.get(time);

            BigDecimal eightEnergyStandardCoal = dealProductionConsumption(eightValue, sum, energySumStandardCoal);
            BigDecimal value8 = CommonUtil.divideWithScale(eightValue, eightEnergyStandardCoal, 2);
            production8.add(value8);

            BigDecimal twelveEnergyStandardCoal = dealProductionConsumption(twelveValue, sum, energySumStandardCoal);
            BigDecimal value12 = CommonUtil.divideWithScale(twelveValue, twelveEnergyStandardCoal, 2);
            production12.add(value12);
        });
        resultVO.setY1(production8);
        resultVO.setY2(production12);
        resultVO.setXdata(xdata);
        resultVO.setToday8(production8.get(production8.size() - 1));
        resultVO.setToday12(production12.get(production12.size() - 1));
        return resultVO;
    }

    /**
     * 获取COP
     *
     * @param paramVO
     * @return
     */
    @Override
    public BigScreenCopChartData getCopChartData(BigScreenParamReqVO paramVO) {

        ReportParamVO reportParamVO = BeanUtils.toBean(paramVO, ReportParamVO.class);
        // 最近七天
        LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(6L);
        LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();
        LocalDateTime[] range = new LocalDateTime[]{startTime, endTime};
        reportParamVO.setRange(range);
        reportParamVO.setDateType(DataTypeEnum.DAY.getCode());
        return copHourAggDataService.copChartForBigScreen(reportParamVO);
    }

    /**
     * 获取纯废水单价
     *
     * @param paramVO
     * @return
     */
    @Override
    public BigScreenChartData getPureWasteWaterChart(BigScreenParamReqVO paramVO) {

        BigScreenChartData resultVO = new BigScreenChartData();
        // 纯水台账
        List<Long> pureSbIds = Collections.emptyList();
        Map<String, List<Long>> pureCodeSbIdMap = Collections.emptyMap();
        ImmutablePair<List<Long>, Map<String, List<Long>>> purePair = dealSbIds(PURE);
        if (Objects.nonNull(purePair)) {
            pureSbIds = purePair.getLeft();
            pureCodeSbIdMap = purePair.getRight();
        }


        // 废水台账
        List<Long> wasteSbIds = Collections.emptyList();
        Map<String, List<Long>> wasteCodeSbIdMap = Collections.emptyMap();
        ImmutablePair<List<Long>, Map<String, List<Long>>> wastePair = dealSbIds(WASTE);
        if (Objects.nonNull(wastePair)) {
            wasteSbIds = wastePair.getLeft();
            wasteCodeSbIdMap = wastePair.getRight();
        }

        // 台账ids合并
        List<Long> sbIdList = Stream
                .concat(pureSbIds.stream(), wasteSbIds.stream())
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(sbIdList)) {
            return resultVO;
        }


        // 最近七天
        LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(6L);
        LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();
        // 根据台账ids获取按台账和时间分组的成本和数据
        List<UsageCostData> usageCostDataList = usageCostService.getTimeSbCostList(
                DataTypeEnum.DAY.getCode(),
                startTime,
                endTime,
                sbIdList);

//        if (CollUtil.isEmpty(usageCostDataList)) {
//            return resultVO;
//        }

        // 按台账分组
        Map<Long, List<UsageCostData>> sbCostUsageDataMap = usageCostDataList
                .stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));

        // 自来水
        List<Long> twSbIds = pureCodeSbIdMap.get(TW);
        Map<String, BigDecimal> twTimeCostMap = CollUtil.isNotEmpty(twSbIds) ? dealTimeCostMap(twSbIds, sbCostUsageDataMap, UsageCostData::getTotalCost) : Collections.emptyMap();
        // 高品质再生水
        List<Long> rwSbIds = pureCodeSbIdMap.get(RW);
        Map<String, BigDecimal> rwTimeCostMap = CollUtil.isNotEmpty(rwSbIds) ? dealTimeCostMap(rwSbIds, sbCostUsageDataMap, UsageCostData::getTotalCost) : Collections.emptyMap();
        // 电力
        List<Long> dlSbIds = pureCodeSbIdMap.get(DL);
        Map<String, BigDecimal> dlTimeCostMap = CollUtil.isNotEmpty(dlSbIds) ? dealTimeCostMap(dlSbIds, sbCostUsageDataMap, UsageCostData::getTotalCost) : Collections.emptyMap();
        // 纯水供水量
        List<Long> pwSbIds = pureCodeSbIdMap.get(PW);
        Map<String, BigDecimal> pwTimeUsageMap = CollUtil.isNotEmpty(pwSbIds) ? dealTimeCostMap(pwSbIds, sbCostUsageDataMap, UsageCostData::getCurrentTotalUsage) : Collections.emptyMap();

        // 电力
        List<Long> wasteDlSbIds = wasteCodeSbIdMap.get(DL);
        Map<String, BigDecimal> wasteDlTimeCostMap = CollUtil.isNotEmpty(wasteDlSbIds) ? dealTimeCostMap(wasteDlSbIds, sbCostUsageDataMap, UsageCostData::getTotalCost) : Collections.emptyMap();
        // 废水量
        List<Long> flSbIds = wasteCodeSbIdMap.get(FL);
        Map<String, BigDecimal> flTimeUsageMap = CollUtil.isNotEmpty(flSbIds) ? dealTimeCostMap(flSbIds, sbCostUsageDataMap, UsageCostData::getCurrentTotalUsage) : Collections.emptyMap();

        // 加上化学品的成本
        Map<String, BigDecimal> chemicalsTimeCostMap = dealChemicals(startTime, endTime);

        // X轴
        List<String> x = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DataTypeEnum.DAY);
        List<String> xdataDisplay = LocalDateTimeUtils.getBigScreenTimeRangeList(startTime, endTime, DataTypeEnum.DAY);

        List<BigDecimal> pureYData = dealPureYData(x, twTimeCostMap, rwTimeCostMap, dlTimeCostMap, chemicalsTimeCostMap, pwTimeUsageMap);
        List<BigDecimal> wasteYData = dealWasteYData(x, wasteDlTimeCostMap, chemicalsTimeCostMap, flTimeUsageMap);

        resultVO.setXdata(xdataDisplay);
        resultVO.setY1(pureYData);
        resultVO.setY2(wasteYData);
        return resultVO;
    }


    private ImmutablePair<List<Long>, Map<String, List<Long>>> dealSbIds(String system) {
        List<PowerPureWasteWaterGasSettingsDO> pureWasteWaterList = powerPureWasteWaterGasSettingsMapper.selectList(new LambdaQueryWrapperX<PowerPureWasteWaterGasSettingsDO>()
                .eq(PowerPureWasteWaterGasSettingsDO::getSystem, system));
        if (CollUtil.isNotEmpty(pureWasteWaterList)) {

            Map<String, List<Long>> codeSbIdMap = pureWasteWaterList
                    .stream()
                    .collect(Collectors.toMap(
                            PowerPureWasteWaterGasSettingsDO::getCode,
                            p -> {
                                String standingbookIds = p.getStandingbookIds();
                                if (CharSequenceUtil.isNotBlank(standingbookIds)) {
                                    String[] split = standingbookIds.split(",");
                                    return Arrays.stream(split).map(Long::valueOf).collect(Collectors.toList());
                                } else {
                                    return ListUtils.newArrayList();
                                }
                            }));
            List<Long> sbIds = codeSbIdMap.values()
                    .stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            return ImmutablePair.of(sbIds, codeSbIdMap);
        }

        return null;
    }

    private Map<String, BigDecimal> dealChemicals(LocalDateTime startTime, LocalDateTime endTime) {
        List<PowerChemicalsSettingsRespVO> chemicalsCostList = powerChemicalsSettingsService.getList(startTime, endTime);

        if (CollUtil.isNotEmpty(chemicalsCostList)) {

            return chemicalsCostList.stream()
                    .filter(c -> Objects.nonNull(c.getPrice()))
                    .collect(Collectors.toMap(PowerChemicalsSettingsRespVO::getStrTime, PowerChemicalsSettingsRespVO::getPrice));
        }

        return Collections.emptyMap();

    }


    private Map<String, BigDecimal> dealTimeCostMap(
            List<Long> sbIds,
            Map<Long, List<UsageCostData>> sbCostUsageDataMap,
            Function<UsageCostData, BigDecimal> valueExtractor) {

        // 根据sbIds获取对应数采
        List<UsageCostData> usageCostDataList = sbIds
                .stream()
                .map(sbCostUsageDataMap::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 把数采数据换成 时间 sum
        return usageCostDataList
                .stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getTime,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .map(valueExtractor)
                                        .filter(Objects::nonNull)
                                        .reduce(BigDecimal::add).orElse(null))));
    }

    private List<BigDecimal> dealPureYData(
            List<String> xdata,
            Map<String, BigDecimal> twTimeCostMap,
            Map<String, BigDecimal> rwTimeCostMap,
            Map<String, BigDecimal> dlTimeCostMap,
            Map<String, BigDecimal> chemicalsTimeCostMap,
            Map<String, BigDecimal> pwTimeUsageMap) {


        return xdata.stream().map(x -> {
            BigDecimal purePrice;
            BigDecimal sumCost;

            BigDecimal twCost = twTimeCostMap.get(x);
            BigDecimal rwCost = rwTimeCostMap.get(x);
            sumCost = addBigDecimal(twCost, rwCost);

            BigDecimal dlCost = dlTimeCostMap.get(x);
            sumCost = addBigDecimal(sumCost, dlCost);

            BigDecimal chemicalsCost = chemicalsTimeCostMap.get(x);
            sumCost = addBigDecimal(sumCost, chemicalsCost);

            BigDecimal pwUsage = pwTimeUsageMap.get(x);

            purePrice = divideWithScale(pwUsage, sumCost, 2);

            return purePrice;
        }).collect(Collectors.toList());
    }

    private List<BigDecimal> dealWasteYData(
            List<String> xdata,
            Map<String, BigDecimal> wasteDlTimeCostMap,
            Map<String, BigDecimal> chemicalsTimeCostMap,
            Map<String, BigDecimal> flTimeUsageMap) {

        return xdata.stream().map(x -> {

            BigDecimal wastePrice;
            BigDecimal dlCost = wasteDlTimeCostMap.get(x);
            BigDecimal chemicalsCost = chemicalsTimeCostMap.get(x);
            BigDecimal sumCost = addBigDecimal(dlCost, chemicalsCost);

            BigDecimal flUsage = flTimeUsageMap.get(x);
            wastePrice = divideWithScale(flUsage, sumCost, 2);
            return wastePrice;

        }).collect(Collectors.toList());

    }

    /**
     * 获取压缩空气单价
     *
     * @param paramVO
     * @return
     */
    @Override
    public BigScreenChartData getCompressedGasChart(BigScreenParamReqVO paramVO) {
        BigScreenChartData resultVO = new BigScreenChartData();
        // 纯水台账
        List<Long> gasSbIds = Collections.emptyList();

        ImmutablePair<List<Long>, Map<String, List<Long>>> gasPair = dealSbIds(GAS);
        if (Objects.nonNull(gasPair)) {
            gasSbIds = gasPair.getLeft();
        }
        if (CollUtil.isEmpty(gasSbIds)) {
            return resultVO;
        }

        // 最近七天
        LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(6L);
        LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();
        // 本期 按台账和日分组求成本和
        List<UsageCostData> usageCostDataList = usageCostService.getTimeCostByStandardIds(
                DataTypeEnum.DAY.getCode(),
                startTime,
                endTime,
                gasSbIds);

        // 上月同期 按台账和日分组求成本和
        List<UsageCostData> lastUsageCostDataList = usageCostService.getTimeCostByStandardIds(
                DataTypeEnum.DAY.getCode(),
                startTime.minusMonths(1),
                endTime.minusMonths(1),
                gasSbIds);

//        if (CollUtil.isEmpty(usageCostDataList)) {
//            return resultVO;
//        }

        // 本期 按台账分组
        Map<String, BigDecimal> gasDlTimeCostMap = usageCostDataList
                .stream()
                .filter(u -> Objects.nonNull(u.getTotalCost()))
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getTotalCost));

        // 上月同期 按台账分组
        Map<String, BigDecimal> lastGasDlTimeCostMap = lastUsageCostDataList
                .stream()
                .filter(u -> Objects.nonNull(u.getTotalCost()))
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getTotalCost));

        // X轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DataTypeEnum.DAY);
        List<String> xdataDisplay = LocalDateTimeUtils.getBigScreenTimeRangeList(startTime, endTime, DataTypeEnum.DAY);
        // 本期
        List<BigDecimal> ydata = xdata.stream().map(x -> {
            BigDecimal dlCost = gasDlTimeCostMap.get(x);
            BigDecimal gasDeliverAbility = BigDecimal.valueOf(187200);
            return divideWithScale(gasDeliverAbility, dlCost, 2);
        }).collect(Collectors.toList());

        // 上月同期
        List<BigDecimal> lastYdata = xdata.stream().map(x -> {

            LocalDate date = LocalDate.parse(x, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String lastTime = date.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            BigDecimal dlCost = lastGasDlTimeCostMap.get(lastTime);
            BigDecimal gasDeliverAbility = BigDecimal.valueOf(187200);
            return divideWithScale(gasDeliverAbility, dlCost, 2);
        }).collect(Collectors.toList());

        resultVO.setXdata(xdataDisplay);
        resultVO.setY1(ydata);
        resultVO.setY2(lastYdata);

        return resultVO;
    }

    @Override
    public MiddleData getMiddleData(BigScreenParamReqVO paramVO) {
        Integer parkFlag = paramVO.getParkFlag();
        if (Objects.isNull(parkFlag)) {
            throw exception(PARK_FLAG_NOT_EXISTS);
        }

        MiddleData resultVO = new MiddleData();

        // 获取标签id
        List<String> labelCodes = getLabelCodeByParkFlag(parkFlag);
        Map<String, LabelConfigDTO> nodePathMap = labelConfigService.getLabelFullPathMap(labelCodes);

        // 标签关联的台账id
        List<Long> sbIdsByLabel = dealStandingbookIdsByLabel(nodePathMap);

        if (CollUtil.isEmpty(sbIdsByLabel)) {
            return resultVO;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = LocalDateTimeUtils.beginOfDay(now);
        LocalDateTime endTime = LocalDateTimeUtils.endOfDay(now);

        List<UsageCostData> todayUsageCostDataList = usageCostService.getSbIdCostStandardCoal(
                startTime,
                endTime,
                sbIdsByLabel);

        if (CollUtil.isEmpty(todayUsageCostDataList)) {
            return resultVO;
        }

        BigDecimal sumCost = todayUsageCostDataList
                .stream()
                .map(UsageCostData::getTotalCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(null);

        BigDecimal sumStandardCoal = todayUsageCostDataList
                .stream()
                .map(UsageCostData::getTotalStandardCoalEquivalent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal::add)
                .orElse(null);

        MiddleItemData todayConsumption = new MiddleItemData();
        todayConsumption.setCost(dealBigDecimalScale10000(sumCost, DEFAULT_SCALE));
        todayConsumption.setStandardCoal(dealBigDecimalScale(sumStandardCoal, DEFAULT_SCALE));
        resultVO.setTodayConsumption(todayConsumption);

        if (parkFlag == 1 || parkFlag == 2) {
            dealMiddleDetailData(resultVO, todayUsageCostDataList, sbIdsByLabel);
        }

        return resultVO;
    }

    private void dealMiddleDetailData(MiddleData resultVO,
                                      List<UsageCostData> todayUsageCostDataList,
                                      List<Long> sbIdsByLabel) {
        // 统一获取能源
        List<String> energyCodes = Arrays.asList(
                "W_Dl_10KV",
                "RO_water",
                "UPW",
                "DIW",
                "W_N2",
                "W_H2",
                "W_O2",
                "W_AR",
                "W_HE");

        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyCodes(energyCodes);

        // 能源关联的台账id
        Map<String, List<Long>> energyCodeSbIdMap = dealEnergySbIds(energyList);
        Map<Long, UsageCostData> todayUsageCostDataMap = todayUsageCostDataList
                .stream()
                .collect(Collectors.toMap(UsageCostData::getStandingbookId, Function.identity()));


        energyCodeSbIdMap.forEach((k, v) -> {
            MiddleItemData middleItemData = new MiddleItemData();
            List<UsageCostData> energyUsageCostDataList = v
                    .stream()
                    .map(todayUsageCostDataMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            BigDecimal sumCost = energyUsageCostDataList
                    .stream()
                    .map(UsageCostData::getTotalCost)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal::add)
                    .orElse(null);

            BigDecimal sumStandardCoal = energyUsageCostDataList
                    .stream()
                    .map(UsageCostData::getTotalStandardCoalEquivalent)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal::add)
                    .orElse(null);

            middleItemData.setCost(dealBigDecimalScale10000(sumCost, DEFAULT_SCALE));
            middleItemData.setStandardCoal(dealBigDecimalScale(sumStandardCoal, DEFAULT_SCALE));

            dealDetailItem(resultVO, middleItemData, k);

        });


        BigScreenChartData bigScreenChartData = new BigScreenChartData();
        // 最近七天
        LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(6L);
        LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();
        List<UsageCostData> sevenUsageCostDataList = usageCostService.getTimeCostByStandardIds(
                DataTypeEnum.DAY.getCode(),
                startTime,
                endTime,
                sbIdsByLabel);

        // 上月同期 按台账和日分组求成本和
        List<UsageCostData> lastSevenUsageCostDataList = usageCostService.getTimeCostByStandardIds(
                DataTypeEnum.DAY.getCode(),
                startTime.minusMonths(1),
                endTime.minusMonths(1),
                sbIdsByLabel);

        // 本期 按台账分组
        Map<String, BigDecimal> timeStandardCoalMap = sevenUsageCostDataList
                .stream()
                .filter(u -> Objects.nonNull(u.getTotalCost()))
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getTotalCost));

        // 上月同期 按台账分组
        Map<String, BigDecimal> lastTimeStandardCoalMap = lastSevenUsageCostDataList
                .stream()
                .filter(u -> Objects.nonNull(u.getTotalCost()))
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getTotalCost));

        // X轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DataTypeEnum.DAY);
        List<String> xdataDisplay = LocalDateTimeUtils.getBigScreenTimeRangeList(startTime, endTime, DataTypeEnum.DAY);

        // 本期
        List<BigDecimal> ydata = xdata.stream().map(x -> {
            BigDecimal cost = timeStandardCoalMap.get(x);
            return dealBigDecimalScale10000(cost, DEFAULT_SCALE);
        }).collect(Collectors.toList());

        // 上月同期
        List<BigDecimal> lastYdata = xdata.stream().map(x -> {

            LocalDate date = LocalDate.parse(x, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String lastTime = date.minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            BigDecimal cost = lastTimeStandardCoalMap.get(lastTime);
            return dealBigDecimalScale10000(cost, DEFAULT_SCALE);
        }).collect(Collectors.toList());

        bigScreenChartData.setXdata(xdataDisplay);
        bigScreenChartData.setY1(ydata);
        bigScreenChartData.setY2(lastYdata);
        resultVO.setTrendChart(bigScreenChartData);

    }


    private void dealDetailItem(MiddleData resultVO, MiddleItemData middleItemData, String code) {

        switch (code) {
            case "W_Dl_10KV":
                resultVO.setPower(middleItemData);
                break;
            case "RO_water":
                resultVO.setRoWater(middleItemData);
                break;
            case "UPW":
                resultVO.setUpw(middleItemData);
                break;
            case "DIW":
                resultVO.setDiw(middleItemData);
                break;
            case "W_N2":
                resultVO.setNitrogen(middleItemData);
                break;
            case "W_H2":
                resultVO.setHydrogen(middleItemData);
                break;
            case "W_O2":
                resultVO.setOxygen(middleItemData);
                break;
            case "W_AR":
                resultVO.setArgon(middleItemData);
                break;
            case "W_HE":
                resultVO.setHelium(middleItemData);
                break;
            default:
        }
    }

    List<Long> dealStandingbookIdsByLabel(Map<String, LabelConfigDTO> nodePathMap) {

        List<Long> sbIdsByLabel = new ArrayList<>();

        nodePathMap.forEach((k, v) -> {
            List<StandingbookLabelInfoDO> sbLabelInfo = statisticsCommonService.getStandingbookIdsByLabel(v.getTopLevelLabelId(), v.getCurLabelId());
            if (CollUtil.isNotEmpty(sbLabelInfo)) {
                List<Long> sbs = sbLabelInfo
                        .stream()
                        .map(StandingbookLabelInfoDO::getStandingbookId)
                        .collect(Collectors.toList());
                sbIdsByLabel.addAll(sbs);
            }
        });

        return sbIdsByLabel
                .stream()
                .distinct()
                .collect(Collectors.toList());

    }

    private Map<String, List<Long>> dealEnergySbIds(List<EnergyConfigurationDO> energyList) {

        Map<String, List<Long>> energySbIdsMap = new HashMap<>();

        if (CollUtil.isNotEmpty(energyList)) {
            energyList.forEach(e -> {
                List<StandingbookDO> standingbookList = statisticsCommonService.getStandingbookIdsByEnergy(Collections.singletonList(e.getId()));
                if (CollUtil.isNotEmpty(standingbookList)) {
                    List<Long> sbIds = standingbookList.stream().map(StandingbookDO::getId).collect(Collectors.toList());
                    energySbIdsMap.put(e.getCode(), sbIds);
                }
            });
        }

        return energySbIdsMap;
    }

    private BigDecimal dealProductionConsumption(BigDecimal value, BigDecimal sum, BigDecimal energySumStandardCoal) {
        if (Objects.isNull(value) || Objects.isNull(sum) || sum.compareTo(BigDecimal.ZERO) == 0 || Objects.isNull(energySumStandardCoal)) {
            return null;
        }

        BigDecimal divide = value.divide(sum, 15, RoundingMode.HALF_UP);
        return energySumStandardCoal.multiply(divide);
    }


    private List<BigDecimal> dealYData(List<UsageCostData> usageCostDataList, List<String> tempxData) {

        if (CollUtil.isEmpty(usageCostDataList)) {
            List<BigDecimal> list = ListUtils.newArrayList();
            tempxData.forEach(t -> list.add(null));
            return list;
        }

        Map<String, BigDecimal> usageCostDataMap = usageCostDataList.stream()
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getCurrentTotalUsage));
        return tempxData
                .stream()
                .map(time -> {
                    BigDecimal bigDecimal = usageCostDataMap.get(time);
                    return dealBigDecimalScale(bigDecimal, DEFAULT_SCALE);
                })
                .collect(Collectors.toList());
    }
}
