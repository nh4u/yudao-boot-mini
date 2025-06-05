package cn.bitlinks.ems.module.acquisition.task;


import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.collectrawdata.CollectRawDataMapper;
import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
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
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_PREFIX;

/**
 * 聚合数据任务
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
    private StarRocksStreamLoadService starRocksStreamLoadService;

    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private RedissonClient redissonClient;
    public static final int batchSize = 2000;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.topic.device-aggregate}")
    private String deviceAggTopic;
    private final static String AGG_TB = "minute_aggregate_data";


    @Scheduled(cron = "0 0/1 * * * ? ") // 每分钟的 0 秒执行一次
//    @Scheduled(cron = "0/10 * * * * ? ") // 每分钟的 0 秒执行一次
    public void execute() {
        String LOCK_KEY = String.format(AGG_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MICROSECONDS)) {
                log.info("聚合任务Task 已由其他节点执行");
            }
            try {
                log.info("聚合任务Task 开始执行");
                insertMinuteData();
                log.info("聚合任务Task 执行完成");
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("聚合任务Task 执行失败", e);
        }

    }

    /**
     * 当前分钟（-10min）的聚合时间的数据计算与插入
     */
    private void insertMinuteData() throws IOException {
        LocalDateTime currentMinute = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusMinutes(10L);
//        LocalDateTime currentMinute = LocalDateTime.of(2025, 5, 26, 11, 21, 0);
        // 1.先获取所有的台账id、能源参数、和id
        List<CollectRawDataDO> collectRawDataDOList = collectRawDataMapper.getGroupedData();
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

        // 按 2000 条分批处理
        List<List<MinuteAggregateDataDO>> batchList = CollUtil.split(currentAggDataList, batchSize);

        for (List<MinuteAggregateDataDO> batch : batchList) {
            // 执行你的批量插入操作，比如：
            String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
            starRocksStreamLoadService.streamLoadData(batch, labelName, AGG_TB);
            // 发送mq消息
            String topicName = deviceAggTopic;
            // 发送消息
            Message<List<MinuteAggregateDataDTO>> msg =
                    MessageBuilder.withPayload(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class)).build();
            rocketMQTemplate.send(topicName, msg);
        }




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
                currentDataList.add(currentMinuteAggregateDataDO);
                return;
            }
            // 如果时间段缺失
            // 需要从上一个聚合时间 latestAggTime 的下一分钟 开始，补到 targetTime 为止
            LocalDateTime fillTime = latestAggTime.plusMinutes(1);
            while (!fillTime.isAfter(targetTime)) {
                // 秒级对齐，插值中已经做了 align
                long secondsBetween = Duration.between(latestAggTime, fillTime).getSeconds();
                long totalSeconds = Duration.between(latestAggTime, exact.getSyncTime()).getSeconds();

                if (totalSeconds == 0) break; // 防止除以 0

                // 插值计算当前 fillTime 的 fullValue
                BigDecimal rate = currentValue.subtract(latestAggData.getFullValue())
                        .divide(BigDecimal.valueOf(totalSeconds), 10, RoundingMode.HALF_UP);

                BigDecimal interpolatedValue = latestAggData.getFullValue()
                        .add(rate.multiply(BigDecimal.valueOf(secondsBetween)));

                // 计算增量
                BigDecimal incremental = interpolatedValue.subtract(latestAggData.getFullValue());

                MinuteAggregateDataDO interpolatedData = BeanUtils.toBean(exact, MinuteAggregateDataDO.class);
                interpolatedData.setAggregateTime(fillTime);
                interpolatedData.setFullValue(interpolatedValue);
                interpolatedData.setIncrementalValue(incremental);

                currentDataList.add(interpolatedData);

                // 模拟进入下一分钟
                latestAggData = interpolatedData;
                fillTime = fillTime.plusMinutes(1);
            }
            return;
        }

        if (Objects.isNull(next) || Objects.isNull(prev)) {
            // ❌ 没有前后的实时数据，说明无法继续推算
            log.info("台账id {} 当前分钟的聚合数据 无前后实时数据，无法生成当前值", standingbookId);
            return;
        }
        // 实时数据
        LocalDateTime prevTime = prev.getSyncTime();//前一时间点 一定不是准分钟点，一定早于targetTime
        BigDecimal prevValue = new BigDecimal(prev.getCalcValue());//前一数值
        LocalDateTime nextTime = next.getSyncTime();//后一时间点 一定不是准分钟点
        BigDecimal nextValue = new BigDecimal(next.getCalcValue());//后一数值
        // 最新聚合数据，
        MinuteAggregateDataDO latestAggData = latestTimeDataMap.get(standingbookId);
        // 聚合数据表没有最新数据，则第一条的增量为0；

        // 聚合数据表有最新数据，
        if (Objects.nonNull(latestAggData)) {
            LocalDateTime latestAggTime = latestAggData.getAggregateTime();
            // 上次实时推送数据早于 最新的聚合时间，则以最新的聚合时间开始进行计算，，永远不会比prevTime晚
            if (latestAggTime.isAfter(prevTime)) {
                prevTime = latestAggTime;
                prevValue = latestAggData.getFullValue();
            }
        }

        long totalSeconds = Duration.between(prevTime, nextTime).getSeconds();
        if (totalSeconds <= 0 || targetTime.isBefore(prevTime) || targetTime.isAfter(nextTime)) {
            return; // 时间非法或越界
        }

        BigDecimal rate = nextValue.subtract(prevValue)
                .divide(BigDecimal.valueOf(totalSeconds), 10, RoundingMode.HALF_UP);

        // 从 prevTime 的下一个准分钟点开始
        LocalDateTime minutePoint = prevTime.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1L);
        MinuteAggregateDataDO lastData = latestAggData;


        while (!minutePoint.isAfter(targetTime)) {
            long elapsedSeconds = Duration.between(prevTime, minutePoint).getSeconds();
            BigDecimal interpolatedValue = prevValue.add(rate.multiply(BigDecimal.valueOf(elapsedSeconds)));

            MinuteAggregateDataDO data = BeanUtils.toBean(prev, MinuteAggregateDataDO.class);
            data.setAggregateTime(minutePoint);
            data.setFullValue(interpolatedValue);

            // 第一条聚合数据增量为0

            // 计算增量：当前值 - 上一条 fullValue
            BigDecimal lastFullValue = lastData != null ? lastData.getFullValue() : BigDecimal.ZERO;
            data.setIncrementalValue(interpolatedValue.subtract(lastFullValue));


            currentDataList.add(data);
            lastData = data;
            minutePoint = minutePoint.plusMinutes(1);
        }




    }
}