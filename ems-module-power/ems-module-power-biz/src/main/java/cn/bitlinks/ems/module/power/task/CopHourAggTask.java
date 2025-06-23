package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.api.minuteaggregatedata.MinuteAggregateDataApi;
import cn.bitlinks.ems.module.acquisition.api.starrocks.StreamLoadApi;
import cn.bitlinks.ems.module.acquisition.api.starrocks.dto.StreamLoadDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopFormulaDO;
import cn.bitlinks.ems.module.power.dto.CopHourAggDataDTO;
import cn.bitlinks.ems.module.power.enums.energyparam.ParamDataFeatureEnum;
import cn.bitlinks.ems.module.power.service.copsettings.CopSettingsService;
import cn.bitlinks.ems.module.power.service.copsettings.dto.CopSettingsDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_PREFIX;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.COP_HOUR_AGG_TABLE_NAME;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.COP_HOUR_AGG_TASK_LOCK_KEY;

/**
 * cop小时计算值的任务
 */
@Slf4j
@Component
public class CopHourAggTask {

    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    @Lazy
    private StreamLoadApi streamLoadApi;
    @Resource
    private CopSettingsService copSettingsService;
    @Resource
    private MinuteAggregateDataApi minuteAggregateDataApi;

    @Scheduled(cron = "0 20 * * * ?") // 每小时的20分钟时执行一次
    @TenantJob
    public void execute() {
        // 从聚合表中计算当前小时的值
        String LOCK_KEY = String.format(COP_HOUR_AGG_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MICROSECONDS)) {
                log.info("COP HOUR 聚合任务Task 已由其他节点执行");
            }
            try {
                log.info("COP HOUR 聚合任务Task 开始执行");
                calculateCop(LocalDateTime.now());
                log.info("COP HOUR 聚合任务Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("COP HOUR 聚合任务Task 执行失败", e);
        }
    }

    public void calculateCop(LocalDateTime aggTime) {
        // 1. 加载 COP 参数设置（可从数据库提前缓存成 Map）
        List<CopSettingsDTO> settings = copSettingsService.getCopSettingsWithParamsList();
        if (CollUtil.isEmpty(settings)) {
            return;
        }
        // 2. 获取cop 公式
        List<CopFormulaDO> copFormulaDOS = copSettingsService.getCopFormulaList();
        if (CollUtil.isEmpty(copFormulaDOS)) {
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

            LocalDateTime startHour = aggTime.truncatedTo(ChronoUnit.HOURS);
            LocalDateTime endHour = aggTime.plusHours(1);
            // 3.4 一次性查询数据库，查询cop对应的台账采集参数，
            List<Long> allSbIds = copParams.stream()
                    .map(CopSettingsDTO::getStandingbookId)
                    .distinct()
                    .collect(Collectors.toList());

            // 依赖的所有台账id和参数的数据们
            List<MinuteAggregateDataDTO> dbHourData = minuteAggregateDataApi.getRangeDataRequestParam(allSbIds, startHour, endHour);

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

                    List<MinuteAggregateDataDTO> dbValues = dbHourData.stream()
                            .filter(d -> sbId.equals(d.getStandingbookId()) && paramCode.equals(d.getParamCode()))
                            .sorted(Comparator.comparing(MinuteAggregateDataDTO::getAggregateTime))
                            .collect(Collectors.toList());

                    if (CollUtil.isEmpty(dbValues)) {
                        log.info("COP [{}] 时间范围[{},{}) ，参数【{}】缺失数据", copFormulaDO.getCopType(), hourStart, hourEnd, formulaParam);
                        return;
                    }
                    // 放入变量Map中
                    if (ParamDataFeatureEnum.Accumulated.getCode().equals(copParam.getDataFeature())) {
                        // 累加 dbValues 所有的增量值
                        BigDecimal sumIncremental = dbValues.stream()
                                .map(MinuteAggregateDataDTO::getIncrementalValue)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        formulaVariables.put(formulaParam, sumIncremental);
                    } else if (ParamDataFeatureEnum.STEADY.getCode().equals(copParam.getDataFeature())) {
                        // 获取当前小时最晚的时间值
                        formulaVariables.put(formulaParam, dbValues.get(dbValues.size() - 1).getFullValue());
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
            log.info("COP 该小时[{}]数据: 没有可计算的数据", aggTime);
            return;
        }
        saveCopHourList(copHourAggDataBatchToAdd);


    }

    @TenantIgnore
    public void saveCopHourList(List<CopHourAggDataDTO> copHourAggDataDTOS) {
        log.info("COP 该小时数据: {}", JSONUtil.toJsonStr(copHourAggDataDTOS));
        StreamLoadDTO dto = new StreamLoadDTO();
        dto.setData(copHourAggDataDTOS);
        dto.setLabel(System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6));
        dto.setTableName(COP_HOUR_AGG_TABLE_NAME);
        streamLoadApi.streamLoadData(dto);
        log.info("saveList end");
    }


}