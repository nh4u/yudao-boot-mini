package cn.bitlinks.ems.module.power.service.copsettings;

import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.bitlinks.ems.module.acquisition.api.starrocks.StreamLoadApi;
import cn.bitlinks.ems.module.acquisition.api.starrocks.dto.StreamLoadDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopFormulaDO;
import cn.bitlinks.ems.module.power.dto.CopHourAggDataDTO;
import cn.bitlinks.ems.module.power.enums.energyparam.ParamDataFeatureEnum;
import cn.bitlinks.ems.module.power.service.copsettings.dto.CopSettingsDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
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

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_PREFIX;
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

//            LocalDateTime startHour = aggTime.truncatedTo(ChronoUnit.HOURS);
//            LocalDateTime endHour = aggTime.plusHours(1);
            // 3.4 一次性查询数据库，查询cop对应的台账采集参数，
            List<Long> allSbIds = copParams.stream()
                    .map(CopSettingsDTO::getStandingbookId)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollUtil.isEmpty(allSbIds)) {
                log.info("COP [{}] ，缺失台账数据", copFormulaDO.getCopType());
                return;
            }
            String allSbIdsStr = String.join(StrPool.COMMA, allSbIds.stream().map(String::valueOf).collect(Collectors.toList()));
            // 依赖的所有台账id和参数的数据们
            List<MinuteAggregateDataDTO> dbHourData = minuteAggregateDataApi.getRangeDataRequestParam(allSbIdsStr, startHour, endHour).getData();

            // 筛选出newHourData中台账这些夏普手机哦的
            // 3.5 循环影响的小时，计算cop的小时值
            LocalDateTime cursor = startHour;   //15：01：01   19：01：01

            while (cursor.isBefore(endHour)) {
                LocalDateTime hourStart = cursor;
                LocalDateTime hourEnd = hourStart.plusHours(1);

                // 3.5.1 循环所有的参数，计算该小时的cop公式值
                Map<String, BigDecimal> formulaVariables = new HashMap<>();
                copParams.forEach(copParam -> {
                    Long sbId = copParam.getStandingbookId();
                    String paramCode = copParam.getParamCode();
                    String formulaParam = copParam.getParam();
                    // 最终合并后的值（用于计算公式）
                    List<MinuteAggregateDataDTO> mergedValues;

                    // 先构建 dbValues 和 newHourValues（你已有代码）

                    Map<LocalDateTime, MinuteAggregateDataDTO> mergedMap = new LinkedHashMap<>();
                    // 数据库的该小时的该参数值
                    if (CollUtil.isNotEmpty(dbHourData)) {
                        List<MinuteAggregateDataDTO> dbValues = dbHourData.stream()
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
                    // 3. 转回 List，按时间排序
                    mergedValues = mergedMap.values().stream()
                            .sorted(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime))
                            .collect(Collectors.toList());


                    // 放入变量Map中
                    if (ParamDataFeatureEnum.Accumulated.getCode().equals(copParam.getDataFeature())) {
                        // 累加 mergedValues 所有的增量值
                        BigDecimal sumIncremental = mergedValues.stream()
                                .map(MinuteAggregateDataDTO::getIncrementalValue)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        formulaVariables.put(formulaParam, sumIncremental);
                    } else if (ParamDataFeatureEnum.STEADY.getCode().equals(copParam.getDataFeature())) {
                        // 获取当前小时最晚的时间值
                        formulaVariables.put(formulaParam, mergedValues.get(mergedValues.size() - 1).getFullValue());
                    }
                });
                // 3.5.2 校验这一小时的所需要的参数值是否齐全
                boolean complete = copParams.stream()
                        .allMatch(param -> formulaVariables.containsKey(param.getParamCode()));

                if (!complete) {
                    log.warn("COP [{}] 时间范围[{},{}) 参数不全，跳过", copFormulaDO.getCopType(), hourStart, hourEnd);
                    cursor = cursor.plusHours(1);
                    continue;
                }

                // 3.5.3 执行该小时的cop公式计算出cop值保存。
                try {
                    BigDecimal result = CalculateUtil.copCalculate(formula, formulaVariables);
                    CopHourAggDataDTO copHourAggDataDTO = new CopHourAggDataDTO();
                    copHourAggDataDTO.setCopType(copFormulaDO.getCopType());
                    copHourAggDataDTO.setCopValue(result);
                    copHourAggDataDTO.setAggregateTime(hourEnd);
                    copHourAggDataBatchToAdd.add(copHourAggDataDTO);
                    log.info("COP [{}] 时间范围[{},{})  计算结果: {}", copFormulaDO.getCopType(), hourStart, hourEnd, result);
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
        System.err.println(JSONUtil.toJsonStr(copHourAggDataBatchToAdd));
//        saveCopHourList(copHourAggDataBatchToAdd);

    }

    public void saveCopHourList(List<CopHourAggDataDTO> copHourAggDataDTOS) {
        log.info("COP 计算逻辑 保存数据明细: {}", JSONUtil.toJsonStr(copHourAggDataDTOS));
        StreamLoadDTO dto = new StreamLoadDTO();
        dto.setData(copHourAggDataDTOS);
        dto.setLabel(System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6));
        dto.setTableName(COP_HOUR_AGG_TABLE_NAME);
        streamLoadApi.streamLoadData(dto);
        log.info("COP 计算逻辑 保存end");
    }
}