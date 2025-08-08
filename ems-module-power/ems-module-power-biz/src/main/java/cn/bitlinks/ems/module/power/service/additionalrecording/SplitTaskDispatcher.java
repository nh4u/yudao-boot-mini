package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.util.calc.AggSplitUtils;
import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.SPLIT_TASK_QUEUE_REDIS_KEY;

@Slf4j
@Component
public class SplitTaskDispatcher {
    @Resource
    private RedisTemplate<String, String> redisTemplate;


    @Resource(name = "splitTaskExecutor")
    private Executor splitExecutor;

    /**
     * 拆分一个业务点，写入 redis 队列
     */
    public void dispatchSplitTask(MinuteAggDataSplitDTO input) {
        List<MinuteAggDataSplitDTO> dailyTasks = splitIntoDailyTasks(input);
        for (MinuteAggDataSplitDTO task : dailyTasks) {
            ZonedDateTime zoned = LocalDateTime.now().atZone(ZoneId.systemDefault());
            double score = zoned.toEpochSecond();
            redisTemplate.opsForZSet().add(SPLIT_TASK_QUEUE_REDIS_KEY, JsonUtils.toJsonString(task), score);
        }
    }

    /**
     * 异步批量处理多个拆分任务
     */
    public void dispatchSplitTaskBatch(List<MinuteAggDataSplitDTO> inputList) {
        log.info("批量拆分任务开始，任务数量：{}", inputList.size());
        for (MinuteAggDataSplitDTO dto : inputList) {
            splitExecutor.execute(() -> {
                try {
                    dispatchSplitTask(dto);
                    log.info("任务拆分成功：台账={}, 时间段=[{}, {}]",
                            dto.getStartDataDO().getStandingbookId(),
                            dto.getStartDataDO().getAggregateTime(),
                            dto.getEndDataDO().getAggregateTime());
                } catch (Exception e) {
                    log.error("任务拆分失败：{}", JsonUtils.toJsonString(dto), e);
                }
            });
        }
    }

    private List<MinuteAggDataSplitDTO> splitIntoDailyTasks(MinuteAggDataSplitDTO input) {
        List<MinuteAggDataSplitDTO> result = new ArrayList<>();
        MinuteAggregateDataDTO startDataDO = input.getStartDataDO();
        MinuteAggregateDataDTO endDataDO = input.getEndDataDO();

        BigDecimal increment = AggSplitUtils.calculatePerMinuteIncrement(
                startDataDO.getAggregateTime(), endDataDO.getAggregateTime(),
                startDataDO.getFullValue(), endDataDO.getFullValue());

        LocalDateTime start = startDataDO.getAggregateTime().plusMinutes(1);
        LocalDateTime end = endDataDO.getAggregateTime().minusMinutes(1);

        if (start.isAfter(end)) return result;

        while (!start.isAfter(end)) {
            LocalDateTime segStart = start;
            LocalDateTime segEnd = segStart.toLocalDate().atTime(23, 59);
            if (segEnd.isAfter(end)) segEnd = end;

            MinuteAggregateDataDTO toAddStartDTO = BeanUtil.copyProperties(startDataDO, MinuteAggregateDataDTO.class);
            MinuteAggregateDataDTO toAddEndDTO = BeanUtil.copyProperties(endDataDO, MinuteAggregateDataDTO.class);

            toAddStartDTO.setAggregateTime(segStart);
            toAddStartDTO.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());
            toAddEndDTO.setAggregateTime(segEnd);
            toAddEndDTO.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());

            long startMinutes = Duration.between(startDataDO.getAggregateTime(), segStart).toMinutes();
            BigDecimal startIncr = increment.multiply(new BigDecimal(startMinutes));
            toAddStartDTO.setFullValue(startDataDO.getFullValue().add(startIncr));
            toAddStartDTO.setIncrementalValue(startIncr);

            long endMinutes = Duration.between(startDataDO.getAggregateTime(), segEnd).toMinutes();
            BigDecimal endIncr = increment.multiply(new BigDecimal(endMinutes));
            toAddEndDTO.setFullValue(startDataDO.getFullValue().add(endIncr));
            toAddEndDTO.setIncrementalValue(increment);

            result.add(new MinuteAggDataSplitDTO(toAddStartDTO, toAddEndDTO));
            start = segEnd.plusMinutes(1);
        }

        return result;
    }


}
