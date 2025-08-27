package cn.bitlinks.ems.module.power.service.usagecost;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.power.controller.admin.coalfactorhistory.vo.CoalFactorFormulaData;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookEnergyTypeVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.EnergyTimeResultVO;
import cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo.QueryEnergyFormula;
import cn.bitlinks.ems.module.power.dal.dataobject.daparamformula.DaParamFormulaDO;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import cn.bitlinks.ems.module.power.dal.dataobject.usagecost.UsageCostDO;
import cn.bitlinks.ems.module.power.dal.mysql.unitpriceconfiguration.UnitPriceConfigurationMapper;
import cn.bitlinks.ems.module.power.dto.UsageCostDTO;
import cn.bitlinks.ems.module.power.enums.BillingMethod;
import cn.bitlinks.ems.module.power.enums.CalculateParamsEnum;
import cn.bitlinks.ems.module.power.enums.FormulaTypeEnum;
import cn.bitlinks.ems.module.power.service.coalfactorhistory.CoalFactorHistoryService;
import cn.bitlinks.ems.module.power.service.daparamformula.DaParamFormulaService;
import cn.bitlinks.ems.module.power.service.pricedetail.PriceDetailService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wangl
 * @date 2025年05月23日 13:49
 */
@Slf4j
@Service
@Validated
public class CalcUsageCostService {

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private UnitPriceConfigurationMapper unitPriceConfigurationMapper;

    @Resource
    private PriceDetailService priceDetailService;

    @Resource
    private CoalFactorHistoryService coalFactorHistoryService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private DaParamFormulaService daParamFormulaService;


    @Value("${ems.calc.scale}")
    private Integer scale;





    /**
     * 计算能源折标煤和用能成本任务
     *
     * @return
     */
    @TenantJob
    public void process(List<MinuteAggregateDataDTO> byAggregateTime) {

        log.info("【计算计量器具折标煤和用能成本任务开始】");
        try {
            if (CollectionUtil.isEmpty(byAggregateTime)) {
                log.info("计算计量器具折标煤和用能成本暂无数据");
                return;
            }
            long start = System.currentTimeMillis();
            //台账ID
            Set<Long> standingbookIds = byAggregateTime.stream().map(MinuteAggregateDataDTO::getStandingbookId).collect(Collectors.toSet());
            //台账能源类型关系
            List<StandingbookEnergyTypeVO> byStandingbookIds = standingbookService.getEnergyAndTypeByStandingbookIds(new ArrayList<>(standingbookIds));
            // 台账能源类型关系Map
            Map<Long, StandingbookEnergyTypeVO> standingbookEnergyTypeVOMap = byStandingbookIds.stream().collect(Collectors.toMap(StandingbookEnergyTypeVO::getStandingbookId, Function.identity()));
            //能源ID
            List<QueryEnergyFormula> queryEnergyTimeList = new ArrayList<>();
            byAggregateTime.forEach(minuteAggregateDataDTO -> {
                StandingbookEnergyTypeVO standingbookEnergyTypeVO = standingbookEnergyTypeVOMap.get(minuteAggregateDataDTO.getStandingbookId());
                QueryEnergyFormula queryEnergyTime = new QueryEnergyFormula();
                queryEnergyTime.setEnergyId(standingbookEnergyTypeVO.getEnergyId());
                queryEnergyTime.setAggregateTime(minuteAggregateDataDTO.getAggregateTime());
                queryEnergyTimeList.add(queryEnergyTime);
            });
            //能源单价公式
            List<EnergyTimeResultVO> byEnergyTime = unitPriceConfigurationMapper.getByEnergyTime(queryEnergyTimeList);


            Set<Long> formulaIds = new HashSet<>();
            Map<Long, List<PriceDetailDO>> detailsByPriceIds = new HashMap<>();
            if (CollectionUtil.isNotEmpty(byEnergyTime)) {
                //单价ID
                List<Long> priceIds = byEnergyTime.stream().map(EnergyTimeResultVO::getId).collect(Collectors.toList());
                //公式IDmap
                detailsByPriceIds = priceDetailService.getDetailsByPriceIds(priceIds);
                //单价公式ID
                Set<Long> priceFormulaIds = byEnergyTime.stream().map(EnergyTimeResultVO::getFormulaId).collect(Collectors.toSet());
                formulaIds.addAll(priceFormulaIds);
            }


            //折标煤公式
            List<CoalFactorFormulaData> coalFactorFormulaList = coalFactorHistoryService.getByEnergyIdsAndTime(queryEnergyTimeList);

            if (CollectionUtil.isNotEmpty(coalFactorFormulaList)) {
                //折标煤公式ID
                Set<Long> coalFactorFormulaIds = coalFactorFormulaList.stream().map(CoalFactorFormulaData::getFormulaId).collect(Collectors.toSet());
                //折标煤和单价公式ID集合
                formulaIds.addAll(coalFactorFormulaIds);
            }


            //单价/折标煤 公式列表
            List<DaParamFormulaDO> priceFormulaList = daParamFormulaService.getByIds((new ArrayList<>(formulaIds)));

            Map<Long, DaParamFormulaDO> priceFormulaIdMap = new HashMap<>();
            if (CollectionUtil.isNotEmpty(priceFormulaList)) {
                priceFormulaIdMap = priceFormulaList.stream().collect(Collectors.toMap(DaParamFormulaDO::getId, Function.identity()));
            }

            //能源单价公式
            Map<Long, DaParamFormulaDO> finalPriceFormulaIdMap = priceFormulaIdMap;

            Map<String, EnergyTimeResultVO> energyTimeFormula = new HashMap<>();
            if (CollectionUtil.isNotEmpty(byEnergyTime)) {
                energyTimeFormula = byEnergyTime.stream()
                        .map(energyTimeResultVO -> {
                            if (finalPriceFormulaIdMap.containsKey(energyTimeResultVO.getFormulaId())) {
                                DaParamFormulaDO daParamFormulaDO = finalPriceFormulaIdMap.get(energyTimeResultVO.getFormulaId());
                                energyTimeResultVO.setEnergyFormula(daParamFormulaDO.getEnergyFormula());
                                energyTimeResultVO.setFormulaScale(daParamFormulaDO.getFormulaScale());
                            }
                            return energyTimeResultVO;
                        })
                        .collect(Collectors.toMap(v -> v.getEnergyId() + "_" + LocalDateTimeUtil.format(v.getAggregateTime(), DatePattern.NORM_DATETIME_MINUTE_PATTERN)+"_"+ FormulaTypeEnum.USAGE_COST.getCode(), Function.identity()));
            }

            Map<String, CoalFactorFormulaData> coalFactorFormulaDataMap = new HashMap<>();
            if (CollectionUtil.isNotEmpty(coalFactorFormulaList)) {
                //能源折标煤系数公式
                coalFactorFormulaDataMap = coalFactorFormulaList.stream()
                        .map(coalFactorFormulaData -> {
                            if (finalPriceFormulaIdMap.containsKey(coalFactorFormulaData.getFormulaId())) {
                                DaParamFormulaDO daParamFormulaDO = finalPriceFormulaIdMap.get(coalFactorFormulaData.getFormulaId());
                                coalFactorFormulaData.setEnergyFormula(daParamFormulaDO.getEnergyFormula());
                                coalFactorFormulaData.setFormulaScale(daParamFormulaDO.getFormulaScale());
                            }
                            return coalFactorFormulaData;
                        })
                        .collect(Collectors.toMap(v -> v.getEnergyId() + "_" + LocalDateTimeUtil.format(v.getAggregateTime(), DatePattern.NORM_DATETIME_MINUTE_PATTERN)+"_"+ FormulaTypeEnum.COAL.getCode(), Function.identity()));
            }


            List<UsageCostDTO> saveList = new ArrayList<>();

            log.info("开始计算折标煤和用能成本");

            Map<Long, List<PriceDetailDO>> finalDetailsByPriceIds = detailsByPriceIds;
            Map<String, EnergyTimeResultVO> finalEnergyTimeFormula = energyTimeFormula;
            Map<String, CoalFactorFormulaData> finalCoalFactorFormulaDataMap = coalFactorFormulaDataMap;

            byAggregateTime.forEach(dto -> {
                //台账能源类型
                StandingbookEnergyTypeVO standingbookEnergyTypeVO = standingbookEnergyTypeVOMap.get(dto.getStandingbookId());
                String energyIdAndTimeUsageCostKey = standingbookEnergyTypeVO.getEnergyId() + "_" + LocalDateTimeUtil.format(dto.getAggregateTime(), DatePattern.NORM_DATETIME_MINUTE_PATTERN) +"_"+ FormulaTypeEnum.USAGE_COST.getCode();
                String energyIdAndTimeCoalKey = standingbookEnergyTypeVO.getEnergyId() + "_" + LocalDateTimeUtil.format(dto.getAggregateTime(), DatePattern.NORM_DATETIME_MINUTE_PATTERN) +"_"+ FormulaTypeEnum.COAL.getCode();
                BigDecimal currentUsage = dto.getIncrementalValue();


                //成本
                BigDecimal cost = BigDecimal.valueOf(-1);
                if(MapUtil.isNotEmpty(finalEnergyTimeFormula) && finalEnergyTimeFormula.containsKey(energyIdAndTimeUsageCostKey)&& MapUtil.isNotEmpty(finalDetailsByPriceIds)){
                    //单价公式
                    EnergyTimeResultVO energyTimeResultVO = finalEnergyTimeFormula.get(energyIdAndTimeUsageCostKey);
                    if(finalDetailsByPriceIds.containsKey(energyTimeResultVO.getId())){
                        //能源单价详细
                        List<PriceDetailDO> priceDetailList = finalDetailsByPriceIds.get(energyTimeResultVO.getId());
                        cost = calcCost(priceDetailList, currentUsage, energyTimeResultVO, dto.getAggregateTime());
                    }
                }

                //折标煤
                BigDecimal coalNum = BigDecimal.valueOf(-1);
                if(finalCoalFactorFormulaDataMap.containsKey(energyIdAndTimeCoalKey)){
                    //折标煤公式
                    CoalFactorFormulaData coalFactorFormulaData = finalCoalFactorFormulaDataMap.get(energyIdAndTimeCoalKey);
                    coalNum = calcCoalFactorFormula(coalFactorFormulaData, currentUsage);
                }

                //组装成本
                UsageCostDTO usageCostDO = new UsageCostDTO();
                usageCostDO.setStandingbookId(dto.getStandingbookId());
                usageCostDO.setEnergyId(standingbookEnergyTypeVO.getEnergyId());
                usageCostDO.setCurrentUsage(currentUsage);
                usageCostDO.setAggregateTime(dto.getAggregateTime());
                usageCostDO.setCost(cost);
                usageCostDO.setStandardCoalEquivalent(coalNum);
                usageCostDO.setTotalUsage(dto.getFullValue());
                saveList.add(usageCostDO);

            });
            log.info("计算折标煤和用能成本完成，存入starrocks, 耗时：{} 毫秒", System.currentTimeMillis() - start);
            usageCostService.saveList(saveList);
            log.info("【计算计量器具折标煤和用能成本任务结束】");

        } catch (Exception ex) {
            log.error("[计算计量器具折标煤和用能成本任务][执行异常]", ex);
            //失败数据以JSON格式记录到日志中
            log.error("[计算计量器具折标煤和用能成本任务]失败数据共：{} 条， 数据明细：{}", byAggregateTime.size(), JSONUtil.toJsonStr(byAggregateTime));
        }


    }

    private BigDecimal calcCoalFactorFormula(CoalFactorFormulaData coalFactorFormulaData, BigDecimal currentUsage) {
        log.info("【计算计量器具折标煤】");
        if (Objects.isNull(coalFactorFormulaData) || StrUtil.isBlank(coalFactorFormulaData.getEnergyFormula())) {
            log.info("当前周期能源类型未配置折标煤公式，energyId:{}，aggregateTime：{}", coalFactorFormulaData.getEnergyId(), LocalDateTimeUtil.format(coalFactorFormulaData.getAggregateTime(), DatePattern.NORM_DATETIME_MINUTE_PATTERN));
            return BigDecimal.ZERO;
        }
        Map<String, Object> calcData = new HashMap<>();
        calcData.put(CalculateParamsEnum.ENERGY_CONSUMPTION.getDetail(), currentUsage);
        calcData.put(CalculateParamsEnum.STANDARD_COAL_COEFFICIENT.getDetail(), coalFactorFormulaData.getFactor());
        Integer formulaScale = coalFactorFormulaData.getFormulaScale();
        //结果
        BigDecimal execute = (BigDecimal) CalculateUtil.execute(coalFactorFormulaData.getEnergyFormula(), calcData);
        if (Objects.isNull(formulaScale) || formulaScale == 0) {
            return execute.setScale(scale, RoundingMode.HALF_UP);
        }
        return execute.setScale(formulaScale, RoundingMode.HALF_UP);
    }

    /**
     * 计算成本
     */
    private BigDecimal calcCost(List<PriceDetailDO> priceDetailList, BigDecimal currentUsage, EnergyTimeResultVO energyTimeResultVO, LocalDateTime queryTime) {
        log.info("计算计量器具用能成本，计价方式：{}，当前用量，{}", energyTimeResultVO.getBillingMethod(), currentUsage.toString());
        if (Objects.isNull(energyTimeResultVO) || StrUtil.isBlank(energyTimeResultVO.getEnergyFormula())) {
            log.info("当前周期能源类型未配置单价公式，energyId:{}，aggregateTime：{}", energyTimeResultVO.getEnergyId(), LocalDateTimeUtil.format(energyTimeResultVO.getAggregateTime(), DatePattern.NORM_DATETIME_MINUTE_PATTERN));
            return BigDecimal.ZERO;
        }
        //统一计价
        if (energyTimeResultVO.getBillingMethod().equals(BillingMethod.UNIFIED_PRICE.getCode())) {
            PriceDetailDO priceDetailDOS = priceDetailList.get(0);
            BigDecimal unitPrice = priceDetailDOS.getUnitPrice();
            Map<String, Object> calcData = new HashMap<>();
            calcData.put(CalculateParamsEnum.ENERGY_CONSUMPTION.getDetail(), currentUsage);
            calcData.put(CalculateParamsEnum.PRICE.getDetail(), unitPrice);
            //结果
            BigDecimal execute = (BigDecimal) CalculateUtil.execute(energyTimeResultVO.getEnergyFormula(), calcData);
            return execute;
        } else if (energyTimeResultVO.getBillingMethod().equals(BillingMethod.TIME_SPAN_PRICE.getCode())) { //分时段计价
            PriceDetailDO priceDetailDO = priceDetailList.stream()
                    .filter(pd -> pd.getPeriodStart().compareTo(queryTime.toLocalTime()) <= 0
                            && pd.getPeriodEnd().compareTo(queryTime.toLocalTime()) >= 0)
                    .findAny().orElse(null);
            if (Objects.isNull(priceDetailDO)) {
                return BigDecimal.ZERO;
            } else {
                BigDecimal unitPrice = priceDetailDO.getUnitPrice();
                Map<String, Object> calcData = new HashMap<>();
                calcData.put(CalculateParamsEnum.ENERGY_CONSUMPTION.getDetail(), currentUsage);
                calcData.put(CalculateParamsEnum.PRICE.getDetail(), unitPrice);
                //结果
                BigDecimal execute = (BigDecimal) CalculateUtil.execute(energyTimeResultVO.getEnergyFormula(), calcData);
                return execute;
            }
        } else if (energyTimeResultVO.getBillingMethod().equals(BillingMethod.STAIR_PRICE.getCode())) {
            //TODO 阶梯计价
            return BigDecimal.ZERO;
        }
        return BigDecimal.ZERO;
    }

}
