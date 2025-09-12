package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.BigScreenCopChartData;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StructureInfoData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerMonthPlanSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;
import cn.bitlinks.ems.module.power.dal.mysql.bigscreen.PowerPureWasteWaterGasSettingsMapper;
import cn.bitlinks.ems.module.power.service.collectrawdata.CollectRawDataService;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.production.ProductionService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.bitlinks.ems.module.power.utils.CommonUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.util.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
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
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private ProductionService productionService;

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
     * 获取室外工况
     *
     * @param paramVO
     * @return
     */
    @Override
    public OutsideEnvData getOutsideEnvData(BigScreenParamReqVO paramVO) {
        OutsideEnvData outsideEnvData = new OutsideEnvData();
        List<String> dataSites = Arrays.asList(WIND_DIRECTION_IO, WIND_SPEED_IO, TEMPERATURE_IO, HUMIDITY_IO, DEW_POINT_IO, ATMOSPHERIC_PRESSURE_IO, NOISE_IO);
        List<CollectRawDataDO> outsideDataList = collectRawDataService.getOutsideDataByDataSite(dataSites);
        if (CollUtil.isNotEmpty(outsideDataList)) {
            Map<String, CollectRawDataDO> outsideDataMap = outsideDataList
                    .stream()
                    .collect(Collectors.toMap(CollectRawDataDO::getDataSite, Function.identity()));

//          todo   outsideEnvData.setWindDirection();
            outsideEnvData.setWindDirectionValue(new BigDecimal(outsideDataMap.get(WIND_DIRECTION_IO).getRawValue()));
            outsideEnvData.setWindSpeed((new BigDecimal(outsideDataMap.get(WIND_SPEED_IO).getRawValue())));
            outsideEnvData.setTemperature(new BigDecimal(outsideDataMap.get(TEMPERATURE_IO).getRawValue()));
            outsideEnvData.setHumidity(new BigDecimal(outsideDataMap.get(HUMIDITY_IO).getRawValue()));
            outsideEnvData.setDewPoint(new BigDecimal(outsideDataMap.get(DEW_POINT_IO).getRawValue()));
            outsideEnvData.setAtmosphericPressure(new BigDecimal(outsideDataMap.get(ATMOSPHERIC_PRESSURE_IO).getRawValue()));
            outsideEnvData.setNoise(new BigDecimal(outsideDataMap.get(NOISE_IO).getRawValue()));

        }
        return outsideEnvData;
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

        // 当期
        // 今日
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime beginOfDay = LocalDateTimeUtils.beginOfDay(now);
        LocalDateTime endOfDay = LocalDateTimeUtils.endOfDay(now);
        List<UsageCostData> todayData = usageCostService.getEnergyStandardCoalByEnergyIds(
                beginOfDay,
                endOfDay,
                energyIds);


        // 本月
        LocalDateTime beginOfMonth = LocalDateTimeUtils.beginOfMonth(now);
        LocalDateTime endOfMonth = LocalDateTimeUtils.endOfMonth(now);
        List<UsageCostData> monthData = usageCostService.getEnergyStandardCoalByEnergyIds(
                beginOfMonth,
                endOfMonth,
                energyIds);

        // 上期
        // 上日
        List<UsageCostData> yesterdayData = usageCostService.getEnergyStandardCoalByEnergyIds(
                beginOfDay.minusDays(1),
                endOfDay.minusDays(1),
                energyIds);

        // 上月
        List<UsageCostData> lastMonthData = usageCostService.getEnergyStandardCoalByEnergyIds(
                beginOfMonth.minusMonths(1),
                endOfMonth.minusMonths(1),
                energyIds);


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

        // 最近七天
        LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(6L);
        LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();

        // 4.1.x轴处理
        List<String> tempXData = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, DataTypeEnum.DAY);

        List<String> xdata = LocalDateTimeUtils.getBigScreenTimeRangeList(startTime, endTime, DataTypeEnum.DAY);

        List<UsageCostData> usageCostDataList = usageCostService.getEnergyUsageByEnergyIds(
                DataTypeEnum.DAY.getCode(),
                startTime,
                endTime,
                energyIds);

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

        List<ProductionDO> eightList = productionSizeMap.get(8);
        Map<String, BigDecimal> eightTimeMap = eightList
                .stream()
                .collect(Collectors.toMap(ProductionDO::getStrTime, ProductionDO::getValue));

        List<ProductionDO> twelveList = productionSizeMap.get(12);
        Map<String, BigDecimal> twelveTimeMap = twelveList
                .stream()
                .collect(Collectors.toMap(ProductionDO::getStrTime, ProductionDO::getValue));

        // 综合能耗
        // 能源处理 外购
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getByEnergyClassify(1);
        if (CollUtil.isEmpty(energyList)) {
            return resultVO;
        }
        List<Long> energyIdList = energyList.stream().map(EnergyConfigurationDO::getId).collect(Collectors.toList());

        // 外购总能耗
        List<UsageCostData> usageCostDataList = usageCostService.getEnergyTimeUsageEnergyIds(DataTypeEnum.DAY.getCode(), startTime, endTime, energyIdList);

        Map<String, BigDecimal> usageCostDataMap = usageCostDataList.stream()
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getTotalStandardCoalEquivalent));

        List<BigDecimal> production8 = ListUtils.newArrayList();
        List<BigDecimal> production12 = ListUtils.newArrayList();

        tempXData.forEach(time -> {
            BigDecimal eightValue = eightTimeMap.get(time);
            BigDecimal twelveValue = twelveTimeMap.get(time);
            BigDecimal sum = eightValue.add(twelveValue);

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
        resultVO = deal(PURE);

        return resultVO;
    }


    private BigScreenChartData deal(String system) {
        List<PowerPureWasteWaterGasSettingsDO> pureWasteWaterList = powerPureWasteWaterGasSettingsMapper.selectList(new LambdaQueryWrapperX<PowerPureWasteWaterGasSettingsDO>()
                .eq(PowerPureWasteWaterGasSettingsDO::getSystem, system));
        if (CollUtil.isNotEmpty(pureWasteWaterList)) {

            Map<String, List<Long>> codeSbIdList = pureWasteWaterList
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

            List<Long> sbIdList = codeSbIdList.values().stream().flatMap(List::stream)
                    .collect(Collectors.toList());


            // 按台账和日分组求成本和
            // 最近七天
            LocalDateTime startTime = LocalDateTimeUtils.lastNDaysStartTime(6L);
            LocalDateTime endTime = LocalDateTimeUtils.lastNDaysEndTime();

            List<UsageCostData> usageCostDataList = usageCostService.getTimeSbCostList(
                    DataTypeEnum.DAY.getCode(),
                    startTime,
                    endTime,
                    sbIdList);

            Map<Long, List<UsageCostData>> sbCostDataMap = usageCostDataList.stream().collect(Collectors.groupingBy(UsageCostData::getStandingbookId));


//            codeSbIdList.forEach();


            Map<String, BigDecimal> collect1 = usageCostDataList.stream().collect(Collectors.groupingBy(
                    UsageCostData::getTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> list.stream()
                                    .map(UsageCostData::getTotalCost)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal::add).orElse(null)
                    )
            ));


            // 加上化学品的成本


            // 查找用量
        }

        return null;
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
        List<String> system = Arrays.asList(GAS);
        List<PowerPureWasteWaterGasSettingsDO> pureWasteWaterList = powerPureWasteWaterGasSettingsMapper.selectList(new LambdaQueryWrapperX<PowerPureWasteWaterGasSettingsDO>()
                .in(PowerPureWasteWaterGasSettingsDO::getSystem, system));
        if (CollUtil.isNotEmpty(pureWasteWaterList)) {
            List<Long> sbList = pureWasteWaterList
                    .stream()
                    .map(PowerPureWasteWaterGasSettingsDO::getStandingbookIds)
                    .filter(Objects::nonNull)
                    .map(s -> {
                        String[] split = s.split(",");
                        return Arrays.stream(split).map(Long::valueOf).collect(Collectors.toList());
                    })
                    .flatMap(List::stream)
                    .collect(Collectors.toList());


            // 按台账和日分组求成本和
            List<UsageCostData> list = usageCostService.getList(
                    paramVO.getRange()[0],
                    paramVO.getRange()[1],
                    sbList);

            // 加上化学品的成本

            // 查找用量
        }
        return resultVO;
    }

    @Override
    public OriginMiddleData getMiddleData(BigScreenParamReqVO paramVO) {
        return null;
    }

    private BigDecimal dealProductionConsumption(BigDecimal value, BigDecimal sum, BigDecimal energySumStandardCoal) {
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
