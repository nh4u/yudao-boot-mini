package cn.bitlinks.ems.module.acquisition.task;


import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.enums.CommonStatusEnum;
import cn.bitlinks.ems.framework.common.enums.FullIncrementEnum;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.collectrawdata.CollectRawDataMapper;
import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata.MinuteAggregateDataService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.AGG_TASK_LOCK_KEY;
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.AGG_TASK_STEADY_LOCK_KEY;

/**
 * 聚合数据任务(用量+稳态值)
 */
@Slf4j
@Component
@DS("starrocks")
public class AggTask {
    @Resource
    private CollectRawDataMapper collectRawDataMapper;
    @Resource
    private MinuteAggregateDataMapper minuteAggregateDataMapper;
    @Resource
    @Lazy
    private MinuteAggregateDataService minuteAggregateDataService;

    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;

    @Scheduled(cron = "0 0/1 * * * ? ") // 每分钟的 0 秒执行一次
//    @Scheduled(cron = "0/10 * * * * ? ") // 每分钟的 0 秒执行一次
    public void execute() {
        String LOCK_KEY = String.format(AGG_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MICROSECONDS)) {
                log.info("聚合任务[用量]Task 已由其他节点执行");
            }
            try {
                log.info("聚合任务[用量]Task 开始执行");
                insertMinuteData();
                log.info("聚合任务[用量]Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("聚合任务[用量]Task 执行失败", e);
        }

    }

    /**
     * 聚合任务-稳态值
     */
    @Scheduled(cron = "30 0/1 * * * ? ") // 每分钟的 30 秒执行一次
    public void executeSteady() {
        String LOCK_KEY = String.format(AGG_TASK_STEADY_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MICROSECONDS)) {
                log.info("聚合任务[稳态值]Task 已由其他节点执行");
            }
            try {
                log.info("聚合任务[稳态值]Task 开始执行");
                insertSteadyData();
                log.info("聚合任务[稳态值]Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("聚合任务[稳态值]Task 执行失败", e);
        }

    }

    /**
     * 聚合稳态值
     */
    private void insertSteadyData() throws IOException {
        //只要台账有稳态值的话，数采的频率需要在一分钟内
//        1、所有稳态值都要聚合（指1分钟的值），稳态值聚合规则如下：
//        取1分钟内采集到的末尾值作为该1分钟的值。
//        例如：12:58:13 值8->12:58:30 值4->12:58:50  值15    。则12:59分的值是15。
//        2、COP报表稳态值取值规则如下：
//        取1小时内聚合的末尾值作为该1小时的值。
//        例如：如上时间顺序的聚合值。COP报表中，13时的值是15。
        LocalDateTime currentMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusMinutes(1L);

        //        LocalDateTime currentMinute = LocalDateTime.of(2025, 6, 9, 19, 20, 0);
        // List<MinuteAggregateDataDO> list = new ArrayList<>();
        // 1.获取每个台账的所有编码对应的该分钟的上一分钟的末尾值当成此分钟点的稳态值
        List<CollectRawDataDO> collectRawDataDOList = collectRawDataMapper.getGroupedSteadyFinalValue(currentMinute.minusMinutes(1L), currentMinute);
        if (CollUtil.isEmpty(collectRawDataDOList)) {
            return;
        }
        // 2.把实时数据转为聚合数据
        List<MinuteAggregateDataDO> currentAggDataList = new ArrayList<>();
        collectRawDataDOList.forEach(finalValue -> {
            MinuteAggregateDataDO minuteAggregateDataDO = BeanUtils.toBean(finalValue, MinuteAggregateDataDO.class);
            minuteAggregateDataDO.setAggregateTime(currentMinute);
            minuteAggregateDataDO.setFullValue(new BigDecimal(finalValue.getCalcValue()));
            minuteAggregateDataDO.setIncrementalValue(null);
            // 稳态值的分钟数据都是采集点，因为不是采集点的不会在表中出现
            minuteAggregateDataDO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
            currentAggDataList.add(minuteAggregateDataDO);
        });

        // 4.将计算出的当前分钟的聚合数据插入到聚合数据表中
        minuteAggregateDataService.insertSteadyAggDataBatch(currentAggDataList);
    }

    /**
     * 聚合用量值
     * 当前分钟（-10min）的聚合时间的数据计算与插入
     */
    private void insertMinuteData() throws IOException {

        LocalDateTime currentMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusMinutes(1L);
//        LocalDateTime currentMinute = LocalDateTime.of(2025, 6, 24, 21, 35, 0);
//
//        2025-06-09 19:47:12
        // 1.先获取所有的台账id、能源参数、和id
        List<CollectRawDataDO> collectRawDataDOList = collectRawDataMapper.getGroupedStandingbookIdData();
        if (CollUtil.isEmpty(collectRawDataDOList)) {
            return;
        }
        List<Long> standingbookIds = collectRawDataDOList.stream()
                .map(CollectRawDataDO::getStandingbookId)
                .collect(Collectors.toList());
        // 2.获取聚合数据表的最新一分钟的聚合时间的数据
        List<MinuteAggregateDataDO> minuteAggregateDataDOList = minuteAggregateDataMapper.getLatestData();
        Map<Long, MinuteAggregateDataDO> latestTimeDataMap = new HashMap<>();
        if (CollUtil.isNotEmpty(minuteAggregateDataDOList)) {
            latestTimeDataMap = minuteAggregateDataDOList.stream()
                    .collect(Collectors.toMap(
                            MinuteAggregateDataDO::getStandingbookId, // key: 台账 ID
                            Function.identity(),                      // value: 当前对象
                            (existing, replacement) -> replacement    // 冲突处理：保留后者（理论上不应该冲突）
                    ));
        }
        // 3.根据台账查询id 查询实时数据表中距离聚合时间+1分钟往后的最新的时间譬如1：00包含2：00的用量=1的数据
        // 获取这些台账的当前分钟的聚合值
        //Map<Long, MinuteAggregateDataDO> currentAggDataMap = new HashMap<>();
        List<MinuteAggregateDataDO> currentAggDataList = new ArrayList<>();
        Map<Long, MinuteAggregateDataDO> finalLatestTimeDataMap = latestTimeDataMap;

        List<CollectRawDataDO> exactList = collectRawDataMapper.selectExactDataBatch(standingbookIds, currentMinute);

        Map<Long, CollectRawDataDO> exactMap = CollUtil.isEmpty(exactList)
                ? new HashMap<>()
                : exactList.stream().collect(Collectors.toMap(CollectRawDataDO::getStandingbookId, Function.identity()));

        List<CollectRawDataDO> prevList = collectRawDataMapper.selectPrevDataBatch(standingbookIds, currentMinute);

        Map<Long, CollectRawDataDO> prevMap = CollUtil.isEmpty(prevList)
                ? new HashMap<>()
                : prevList.stream().collect(Collectors.toMap(CollectRawDataDO::getStandingbookId, Function.identity()));

        List<CollectRawDataDO> nextList = collectRawDataMapper.selectNextDataBatch(standingbookIds, currentMinute);

        Map<Long, CollectRawDataDO> nextMap = CollUtil.isEmpty(nextList)
                ? new HashMap<>()
                : nextList.stream().collect(Collectors.toMap(CollectRawDataDO::getStandingbookId, Function.identity()));


        standingbookIds.forEach(standingbookId -> {
            CollectRawDataDO exact = exactMap.get(standingbookId);
            // 查询前后最近两条
            // 获取前一条数据（小于目标时间的最大值）
            CollectRawDataDO prev = prevMap.get(standingbookId);

            // 获取后一条数据（大于目标时间的最小值）
            CollectRawDataDO next = nextMap.get(standingbookId);

            // 如果没有前一条数据，按照当前时间为开始时间
            // ✅ 处理 prev 和 next 之间的插值逻辑, 线性计算分钟值(批量当前分钟的前一条实时数据的时间点~当前分钟的所有分钟值，)
            interpolate(exact, prev, next, currentMinute, currentAggDataList, finalLatestTimeDataMap,
                    standingbookId);

        });
        if (CollUtil.isEmpty(currentAggDataList)) {
            log.info("所有台账，当前分钟的聚合数据为空，未获取到任何当前值");
        }
        // 4.将计算出的当前分钟的聚合数据插入到聚合数据表中
        minuteAggregateDataService.sendMsgToUsageCostBatch(currentAggDataList);
//        System.err.println(JSONUtil.toJsonStr(currentAggDataList));

    }


    /**
     * 线性计算分钟值(批量当前分钟的前一条实时数据的时间点~当前分钟的所有分钟值，)
     *
     * @param targetTime 要插值的时间点（应位于 prev 和 next 之间）
     * @return 插值结果（精确小数）
     */
    public static void interpolate(CollectRawDataDO exact, CollectRawDataDO prev, CollectRawDataDO next,
                                   LocalDateTime targetTime,
                                   List<MinuteAggregateDataDO> currentDataList, Map<Long
                    , MinuteAggregateDataDO> latestTimeDataMap, Long standingbookId) {
        if (Objects.nonNull(exact)) {
            // ✅ 正好有这条数据，获取到该台账当前分钟的值
            BigDecimal currentValue = new BigDecimal(exact.getCalcValue());
            MinuteAggregateDataDO latestAggData = latestTimeDataMap.get(standingbookId);
            // 聚合数据表没有最新数据，则增量为0
            if (Objects.isNull(latestAggData)) {
                // ✅ 当前分钟的聚合表数据
                MinuteAggregateDataDO currentMinuteAggregateDataDO = BeanUtils.toBean(exact, MinuteAggregateDataDO.class);
                currentMinuteAggregateDataDO.setAggregateTime(targetTime);
                currentMinuteAggregateDataDO.setFullValue(currentValue);
                currentMinuteAggregateDataDO.setIncrementalValue(BigDecimal.ZERO);
                currentMinuteAggregateDataDO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
                currentDataList.add(currentMinuteAggregateDataDO);
                return;
            }
            // 聚合数据表有最新数据，也是准分钟点，一定在当前分钟之前，需要看是不是补充两个分钟之间的分钟数据，计算出增量和全量
            // ✅ 聚合表中有数据（如上一次是 12:05），当前目标时间是 12:10
            LocalDateTime latestAggTime = latestAggData.getAggregateTime();
            if (latestAggTime.plusMinutes(1L).equals(targetTime)) {
                // 上一次聚合与当前时间之间没有时间缺失，则不需要填补时间，直接插入当前时间的值，
                MinuteAggregateDataDO currentMinuteAggregateDataDO = BeanUtils.toBean(exact, MinuteAggregateDataDO.class);
                currentMinuteAggregateDataDO.setAggregateTime(targetTime);
                currentMinuteAggregateDataDO.setFullValue(currentValue);
                currentMinuteAggregateDataDO.setIncrementalValue(currentValue.subtract(latestAggData.getFullValue()));
                currentMinuteAggregateDataDO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
                currentDataList.add(currentMinuteAggregateDataDO);
                return;
            }
            // 如果时间段缺失
            // 需要从上一个聚合时间 latestAggTime 的下一分钟 开始，补到 targetTime 为止

            // 上次实时推送数据早于 与 最新的聚合时间差距一分钟以上，需要田中，最新聚合时间与上次实时推送数据之间的分钟数据。
            // 需要补充聚合数据最新时间和上次实时数据时间点时间的数据。
            MinuteAggregateDataDO endDO = BeanUtils.toBean(latestAggData, MinuteAggregateDataDO.class);
            endDO.setAggregateTime(targetTime);
            endDO.setFullValue(currentValue);
            endDO.setIncrementalValue(null);
            endDO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
            splitData(currentDataList, latestAggData, latestAggData, endDO);
            return;
        }

        if (Objects.isNull(prev)) {
            // ❌ 没有前的实时数据，说明无法继续推算
            log.info("台账id {} 当前分钟的聚合数据 无前实时数据，无法生成当前值", standingbookId);
            return;
        }
        // 实时数据
        LocalDateTime prevTime = prev.getSyncTime();//前一时间点 一定不是准分钟点，一定早于targetTime
        BigDecimal prevValue = new BigDecimal(prev.getCalcValue());//前一数值

        // 最新聚合数据，
        MinuteAggregateDataDO latestAggData = latestTimeDataMap.get(standingbookId);
        // 聚合数据表没有最新数据，则第一条的增量为0；

        // 聚合数据表有最新数据，
        MinuteAggregateDataDO startDO = new MinuteAggregateDataDO();
        startDO.setAggregateTime(prevTime);
        startDO.setFullValue(prevValue);
        if (Objects.nonNull(latestAggData)) {
            LocalDateTime latestAggTime = latestAggData.getAggregateTime();
            // 上次实时推送数据早于 < 最新的聚合时间，则以最新的聚合时间开始进行计算
            if (prevTime.isBefore(latestAggTime)) {
                startDO.setAggregateTime(latestAggTime);
                startDO.setFullValue(latestAggData.getFullValue());
                startDO.setIncrementalValue(latestAggData.getIncrementalValue());
                startDO.setAcqFlag(latestAggData.getAcqFlag());
            } else if (prevTime.isAfter(latestAggTime.plusMinutes(1L))) {
                // 上次实时推送数据早于 与 最新的聚合时间差距一分钟以上，需要田中，最新聚合时间与上次实时推送数据之间的分钟数据。
                // 需要补充聚合数据最新时间和上次实时数据时间点时间的数据。
                MinuteAggregateDataDO endDO = BeanUtils.toBean(latestAggData, MinuteAggregateDataDO.class);
                endDO.setAggregateTime(prevTime);
                endDO.setFullValue(prevValue);
                endDO.setIncrementalValue(null);
                endDO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
                splitData(currentDataList, latestAggData, latestAggData, endDO);
            }
        }
        if (Objects.isNull(next)) {
            // ❌ 没有后的实时数据，说明无法继续推算
            log.info("台账id {} 当前分钟的聚合数据 无后实时数据，无法生成当前值", standingbookId);
            return;
        }
        startDO.setStandingbookId(standingbookId);
        startDO.setParamCode(prev.getParamCode());
        startDO.setEnergyFlag(prev.getEnergyFlag());
        startDO.setDataSite(prev.getDataSite());
        startDO.setUsage(prev.getUsage());
        startDO.setDataType(prev.getDataType());
        startDO.setDataFeature(prev.getDataFeature());
        startDO.setFullIncrement(prev.getFullIncrement());
        MinuteAggregateDataDO endDO = BeanUtils.toBean(startDO, MinuteAggregateDataDO.class);

        endDO.setAggregateTime(targetTime);
        long totalSeconds = Duration.between(prevTime, next.getSyncTime()).getSeconds();
        BigDecimal rate = new BigDecimal(next.getCalcValue()).subtract(prevValue)
                .divide(BigDecimal.valueOf(totalSeconds), 10, RoundingMode.HALF_UP);
        long elapsedSeconds = Duration.between(prevTime, targetTime).getSeconds();
        endDO.setFullValue(prevValue.add(rate.multiply(BigDecimal.valueOf(elapsedSeconds))));
        if(prevTime.truncatedTo(ChronoUnit.MINUTES).equals(targetTime.truncatedTo(ChronoUnit.MINUTES)) || next.getSyncTime().truncatedTo(ChronoUnit.MINUTES).equals(targetTime.truncatedTo(ChronoUnit.MINUTES))){
            endDO.setAcqFlag(AcqFlagEnum.ACQ.getCode());
        }else{
            endDO.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());
        }
        splitData(currentDataList, latestAggData, startDO, endDO);

    }

    /**
     * 实时数据拆分！
     * 根据开始时间和结束时间，将数据拆分到分钟粒度
     */
    private static void splitData(List<MinuteAggregateDataDO> minuteAggregateDataDOList, MinuteAggregateDataDO lastData, MinuteAggregateDataDO startData, MinuteAggregateDataDO endData) {

        LocalDateTime prevTime = startData.getAggregateTime();
        LocalDateTime nextTime = endData.getAggregateTime();
        BigDecimal prevValue = startData.getFullValue();
        BigDecimal nextValue = endData.getFullValue();

        long totalSeconds = Duration.between(prevTime, nextTime).getSeconds();
        if (totalSeconds <= 0) {
            return; // 时间非法或越界
        }
        BigDecimal rate = nextValue.subtract(prevValue)
                .divide(BigDecimal.valueOf(totalSeconds), 10, RoundingMode.HALF_UP);

        // 从 prevTime 的下一个准分钟点开始
        LocalDateTime minutePoint = prevTime.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1L);

        while (!minutePoint.isAfter(nextTime)) {
            // 差的秒数
            long elapsedSeconds = Duration.between(prevTime, minutePoint).getSeconds();
            // 计算与开始的值的增量，
            BigDecimal interpolatedValue = prevValue.add(rate.multiply(BigDecimal.valueOf(elapsedSeconds)));

            MinuteAggregateDataDO data = BeanUtils.toBean(startData, MinuteAggregateDataDO.class);
            data.setAggregateTime(minutePoint);
            data.setFullValue(interpolatedValue);
            data.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());

            if(minutePoint.equals(nextTime)){
                if (endData.getAcqFlag() != null) {
                    data.setAcqFlag(endData.getAcqFlag());
                } else {
                    data.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());
                }
            }
            if (lastData == null) {
                data.setIncrementalValue(BigDecimal.ZERO);
            } else {
                // 计算增量：当前值 - 上一条 fullValue
                data.setIncrementalValue(interpolatedValue.subtract(lastData.getFullValue()));
            }

            minuteAggregateDataDOList.add(data);
            lastData = data;
            minutePoint = minutePoint.plusMinutes(1);
        }
    }
}