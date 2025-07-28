package cn.bitlinks.ems.module.power.service.copsettings;

import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.dto.MinuteRangeDataCopParamDTO;
import cn.bitlinks.ems.module.acquisition.api.starrocks.StreamLoadApi;
import cn.bitlinks.ems.module.acquisition.api.starrocks.dto.StreamLoadDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopFormulaDO;
import cn.bitlinks.ems.module.power.dto.CopHourAggDataDTO;
import cn.bitlinks.ems.module.power.enums.energyparam.ParamDataFeatureEnum;
import cn.bitlinks.ems.module.power.service.copsettings.dto.CopSettingsDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_COP_PREFIX;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.COP_HOUR_AGG_TABLE_NAME;

/**
 * cop计算 Service 接口
 *
 * @author bitlinks
 */
@Slf4j
@Service
@Validated
public class CopCalcService {
    @Resource
    @Lazy
    private StreamLoadApi streamLoadApi;
    @Resource
    private CopSettingsService copSettingsService;
    @Resource
    private MinuteAggregateDataApi minuteAggregateDataApi;

    /**
     * cop计算逻辑（每小时聚合和重算逻辑）
     *
     * @param startHour
     * @param endHour
     * @param newHourData
     */
    @TenantJob
    public void calculateCop(LocalDateTime startHour, LocalDateTime endHour, List<MinuteAggregateDataDTO> newHourData) {
        log.info("COP计算开始，影响小时区间：{} ~ {}", startHour, endHour);
        // 1. 加载 COP 参数设置（可从数据库提前缓存成 Map）
        List<CopSettingsDTO> settings = copSettingsService.getCopSettingsWithParamsList();
        if (CollUtil.isEmpty(settings)) {
            log.info("COP计算，无COP参数设置数据");
            return;
        }
        // 2. 获取cop 公式
        List<CopFormulaDO> copFormulaDOS = copSettingsService.getCopFormulaList();
        if (CollUtil.isEmpty(copFormulaDOS)) {
            log.info("COP计算，无COP公式配置");
            return;
        }

        List<CopHourAggDataDTO> copHourAggDataBatchToAdd = new ArrayList<>();
        // 3. 遍历公式，四条公式
        for (CopFormulaDO copFormulaDO : copFormulaDOS) {
            String formula = copFormulaDO.getFormula();
            // 3.1 找出该 COP 类型所需的所有台账id和参数编码
            List<CopSettingsDTO> copParams = settings.stream()
                    .filter(s -> copFormulaDO.getCopType().equals(s.getCopType()))
                    .collect(Collectors.toList());
            // 按照 dataFeature 分组 ,稳态值查询所有的末尾值、累积值查询 小时级别数据中的差值、
            Map<Integer, List<CopSettingsDTO>> groupedByDataFeature = copParams.stream()
                    .collect(Collectors.groupingBy(CopSettingsDTO::getDataFeature));
            List<MinuteAggregateDataDTO> dbUsageDataList = new ArrayList<>();
            List<MinuteAggregateDataDTO> dbSteadyDataList = new ArrayList<>();
            for (Map.Entry<Integer, List<CopSettingsDTO>> entry : groupedByDataFeature.entrySet()) {
                if (ParamDataFeatureEnum.Accumulated.getCode().equals(entry.getKey())) {
                    // 查询小时级别数据，多查前一小时的全量值
                    List<CopSettingsDTO> copSettingsDTOS = entry.getValue();
                    if (CollUtil.isEmpty(copSettingsDTOS)) {
                        log.info("COP [{}] ，缺失台账/参数设置", copFormulaDO.getCopType());
                        continue;
                    }
                    List<Long> sbIds = copSettingsDTOS.stream()
                            .map(CopSettingsDTO::getStandingbookId)
                            .distinct()
                            .collect(Collectors.toList());
                    List<String> paramCodes = copSettingsDTOS.stream()
                            .map(CopSettingsDTO::getParamCode)
                            .distinct()
                            .collect(Collectors.toList());
                    if (CollUtil.isEmpty(sbIds) || CollUtil.isEmpty(paramCodes)) {
                        log.info("COP [{}] ，缺失台账/参数设置", copFormulaDO.getCopType());
                        continue;
                    }
                    // 获取所有台账id和所有的参数编码
                    MinuteRangeDataCopParamDTO minuteRangeDataCopParamDTO = new MinuteRangeDataCopParamDTO();
                    minuteRangeDataCopParamDTO.setParamCodes(paramCodes);
                    minuteRangeDataCopParamDTO.setSbIds(sbIds);
                    minuteRangeDataCopParamDTO.setStarTime(startHour);
                    minuteRangeDataCopParamDTO.setEndTime(endHour);
                    dbUsageDataList = minuteAggregateDataApi.getCopRangeData(minuteRangeDataCopParamDTO).getData();
                } else if (ParamDataFeatureEnum.STEADY.getCode().equals(entry.getKey())) {
                    // 查询小时数据的末尾值
                    List<CopSettingsDTO> copSettingsDTOS = entry.getValue();
                    if (CollUtil.isEmpty(copSettingsDTOS)) {
                        log.info("COP [{}] ，缺失台账/参数设置", copFormulaDO.getCopType());
                        continue;
                    }
                    List<Long> sbIds = copSettingsDTOS.stream()
                            .map(CopSettingsDTO::getStandingbookId)
                            .distinct()
                            .collect(Collectors.toList());
                    List<String> paramCodes = copSettingsDTOS.stream()
                            .map(CopSettingsDTO::getParamCode)
                            .distinct()
                            .collect(Collectors.toList());
                    if (CollUtil.isEmpty(sbIds) || CollUtil.isEmpty(paramCodes)) {
                        log.info("COP [{}] ，缺失台账/参数设置", copFormulaDO.getCopType());
                        continue;
                    }
                    // 获取所有台账id和所有的参数编码
                    MinuteRangeDataCopParamDTO minuteRangeDataCopParamDTO = new MinuteRangeDataCopParamDTO();
                    minuteRangeDataCopParamDTO.setParamCodes(paramCodes);
                    minuteRangeDataCopParamDTO.setSbIds(sbIds);
                    minuteRangeDataCopParamDTO.setStarTime(startHour);
                    minuteRangeDataCopParamDTO.setEndTime(endHour);
                    dbSteadyDataList = minuteAggregateDataApi.getCopRangeDataSteady(minuteRangeDataCopParamDTO).getData();
                }
            }

            log.info("COP [{}] ，影响小时区间：{} ~ {}", copFormulaDO.getCopType(), startHour, endHour);
            // 筛选出newHourData中台账这些夏普手机哦的
            // 3.5 循环影响的小时，计算cop的小时值
            LocalDateTime cursor = startHour;   //15：01：01   19：01：01

            while (cursor.isBefore(endHour)) {
                LocalDateTime hourStart = cursor;
                LocalDateTime hourEnd = hourStart.plusHours(1);

                // 3.5.1 循环所有的参数，计算该小时的cop公式值
                Map<String, BigDecimal> formulaVariables = new HashMap<>();
                List<MinuteAggregateDataDTO> finalDbUsageDataList = dbUsageDataList;
                List<MinuteAggregateDataDTO> finalDbSteadyDataList = dbSteadyDataList;
                copParams.forEach(copParam -> {
                    Long sbId = copParam.getStandingbookId();
                    String paramCode = copParam.getParamCode();
                    String formulaParam = copParam.getParam();
                    Integer dataFeature = copParam.getDataFeature();
                    // 最终合并后的值（用于计算公式）
                    Map<LocalDateTime, MinuteAggregateDataDTO> mergedMap = new LinkedHashMap<>();
                    // 先构建 dbValues 和 newHourValues（你已有代码）
                    // 如果是用量
                    if (ParamDataFeatureEnum.Accumulated.getCode().equals(dataFeature)) {
                        // 数据库的该小时的该参数值
                        if (CollUtil.isNotEmpty(finalDbUsageDataList)) {
                            List<MinuteAggregateDataDTO> dbValues = finalDbUsageDataList.stream()
                                    .filter(d -> sbId.equals(d.getStandingbookId()) && paramCode.equals(d.getParamCode())
                                            &&
                                            (!d.getAggregateTime().isBefore(hourStart) && !d.getAggregateTime().isAfter(hourEnd)))
                                    .sorted(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime))
                                    .collect(Collectors.toList());
                            if (CollUtil.isNotEmpty(dbValues)) {
                                dbValues.forEach(d -> mergedMap.put(d.getAggregateTime(), d));
                            }
                        }
                        // 如果有新修改的数据，筛选出新修改的该小时的该参数值
                        if (CollUtil.isNotEmpty(newHourData)) {
                            List<MinuteAggregateDataDTO> newHourValues = newHourData.stream()
                                    .filter(d -> sbId.equals(d.getStandingbookId()) && paramCode.equals(d.getParamCode())
                                            &&
                                            (!d.getAggregateTime().isBefore(hourStart) && !d.getAggregateTime().isAfter(hourEnd)))
                                    .sorted(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime))
                                    .collect(Collectors.toList());
                            if (CollUtil.isNotEmpty(newHourData)) {
                                newHourValues.forEach(d -> mergedMap.put(d.getAggregateTime(), d)); // 覆盖相同时间点
                            }
                        }
                        if (CollUtil.isEmpty(mergedMap.values())) {
                            log.info("COP [{}] 时间范围[{},{}) ，参数【{}】缺失数据", copFormulaDO.getCopType(), hourStart, hourEnd, formulaParam);
                            return;
                        }
                        // 获取当前小时最晚的时间值
                        Optional<MinuteAggregateDataDTO> optionalOldest = mergedMap.entrySet().stream()
                                .min(Map.Entry.comparingByKey())
                                .map(Map.Entry::getValue);

                        // 获取当前小时最晚的时间值
                        Optional<MinuteAggregateDataDTO> optionalLatest = mergedMap.entrySet().stream()
                                .max(Map.Entry.comparingByKey())
                                .map(Map.Entry::getValue);
                        if (!optionalLatest.isPresent() || !optionalOldest.isPresent()) {
                            log.info("COP [{}] 时间范围[{},{}) ，参数【{}】缺失数据", copFormulaDO.getCopType(), hourStart, hourEnd, formulaParam);
                            return;
                        }
                        formulaVariables.put(formulaParam, optionalLatest.get().getFullValue().subtract(optionalOldest.get().getFullValue()));

                    } else if (ParamDataFeatureEnum.STEADY.getCode().equals(dataFeature)) {
                        // 数据库的该小时的该参数值
                        if (CollUtil.isNotEmpty(finalDbSteadyDataList)) {
                            List<MinuteAggregateDataDTO> dbValues = finalDbSteadyDataList.stream()
                                    .filter(d -> sbId.equals(d.getStandingbookId()) && paramCode.equals(d.getParamCode())
                                            &&
                                            (!d.getAggregateTime().isBefore(hourStart) && d.getAggregateTime().isBefore(hourEnd)))
                                    .sorted(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime))
                                    .collect(Collectors.toList());
                            if (CollUtil.isNotEmpty(dbValues)) {
                                dbValues.forEach(d -> mergedMap.put(d.getAggregateTime(), d));
                            }
                        }
                        // 如果有新修改的数据，筛选出新修改的该小时的该参数值
                        if (CollUtil.isNotEmpty(newHourData)) {
                            List<MinuteAggregateDataDTO> newHourValues = newHourData.stream()
                                    .filter(d -> sbId.equals(d.getStandingbookId()) && paramCode.equals(d.getParamCode())
                                            &&
                                            (!d.getAggregateTime().isBefore(hourStart) && d.getAggregateTime().isBefore(hourEnd)))
                                    .sorted(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime))
                                    .collect(Collectors.toList());
                            if (CollUtil.isNotEmpty(newHourData)) {
                                newHourValues.forEach(d -> mergedMap.put(d.getAggregateTime(), d)); // 覆盖相同时间点
                            }
                        }
                        if (CollUtil.isEmpty(mergedMap.values())) {
                            log.info("COP [{}] 时间范围[{},{}) ，参数【{}】缺失数据", copFormulaDO.getCopType(), hourStart, hourEnd, formulaParam);
                            return;
                        }
                        // 获取当前小时最晚的时间值
                        Optional<MinuteAggregateDataDTO> optionalLatest = mergedMap.entrySet().stream()
                                .max(Map.Entry.comparingByKey())
                                .map(Map.Entry::getValue);
                        if (!optionalLatest.isPresent()) {
                            log.info("COP [{}] 时间范围[{},{}) ，参数【{}】缺失数据", copFormulaDO.getCopType(), hourStart, hourEnd, formulaParam);
                            return;
                        }
                        formulaVariables.put(formulaParam, optionalLatest.get().getFullValue());
                    }
                });
                // 3.5.2 校验这一小时的所需要的参数值是否齐全
                boolean complete = copParams.stream()
                        .allMatch(param -> formulaVariables.containsKey(param.getParam()));

                if (!complete) {
                    log.warn("COP [{}] 时间范围[{},{}) 参数不全，跳过", copFormulaDO.getCopType(), hourStart, hourEnd);
                    cursor = cursor.plusHours(1);
                    continue;
                }

                // 3.5.3 执行该小时的cop公式计算出cop值保存。
                try {
                    Object result = CalculateUtil.copCalculate(formula, formulaVariables);
                    if (result != null) {
                        CopHourAggDataDTO copHourAggDataDTO = new CopHourAggDataDTO();
                        copHourAggDataDTO.setCopType(copFormulaDO.getCopType());
                        copHourAggDataDTO.setCopValue(new BigDecimal(result.toString()));
                        copHourAggDataDTO.setAggregateTime(hourEnd);
                        copHourAggDataBatchToAdd.add(copHourAggDataDTO);
                    }
                    log.info("COP [{}] 时间范围[{},{})  计算结果: {},参数：{}", copFormulaDO.getCopType(), hourStart, hourEnd, result, JSONUtil.toJsonStr(formulaVariables));
                } catch (Exception e) {
                    log.error("COP [{}] 时间范围[{},{})  计算失败: {}", copFormulaDO.getCopType(), hourStart, hourEnd, e.getMessage(), e);
                }

                cursor = cursor.plusHours(1);
            }
        }
        if (CollUtil.isEmpty(copHourAggDataBatchToAdd)) {
            log.info("COP计算逻辑，多cop无数据: 没有可计算的数据");
            return;
        }
        saveCopHourList(copHourAggDataBatchToAdd);

    }

    public void saveCopHourList(List<CopHourAggDataDTO> copHourAggDataDTOS) {
        log.info("COP 计算逻辑 保存数据条目大小: {}", copHourAggDataDTOS.size());
        StreamLoadDTO dto = new StreamLoadDTO();
        dto.setData(copHourAggDataDTOS);
        dto.setLabel(System.currentTimeMillis() + STREAM_LOAD_COP_PREFIX + RandomUtil.randomNumbers(6));
        dto.setTableName(COP_HOUR_AGG_TABLE_NAME);
        streamLoadApi.streamLoadData(dto);
        log.info("COP 计算逻辑 保存end");
    }
}