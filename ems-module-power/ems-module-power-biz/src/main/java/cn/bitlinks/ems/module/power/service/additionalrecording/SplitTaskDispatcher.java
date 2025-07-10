package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.enums.AcqFlagEnum;
import cn.bitlinks.ems.framework.common.util.calc.AggSplitUtils;
import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.SPLIT_TASK_QUEUE_REDIS_KEY_PREFIX;

// 拆分任务入队调度器
@Component
public class SplitTaskDispatcher {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public void dispatchSplitTask(MinuteAggDataSplitDTO input) {
        List<MinuteAggDataSplitDTO> dailyTasks = splitIntoDailyTasks(input);
        for (MinuteAggDataSplitDTO task : dailyTasks) {
            String key = getQueueKey(task.getStartDataDO().getAggregateTime());
            redisTemplate.opsForList().leftPush(key, JsonUtils.toJsonString(task));
        }
    }
    public void dispatchSplitTaskBatch(List<MinuteAggDataSplitDTO> inputList) {
        // 遍历列表，进行每个数据的异步处理
        for (MinuteAggDataSplitDTO minuteAggDataSplitDTO : inputList) {
            // 执行异步插入拆分操作
            dispatchSplitTask(minuteAggDataSplitDTO);
        }
    }

    /**
     * 拆分业务点中的数据
     *
     * @param input
     * @return
     */
    private List<MinuteAggDataSplitDTO> splitIntoDailyTasks(MinuteAggDataSplitDTO input) {
        List<MinuteAggDataSplitDTO> result = new ArrayList<>();
        MinuteAggregateDataDTO startDataDO = input.getStartDataDO();
        MinuteAggregateDataDTO endDataDO = input.getEndDataDO();


        // 计算每分钟的增量
        BigDecimal increment = AggSplitUtils.calculatePerMinuteIncrement(startDataDO.getAggregateTime(), endDataDO.getAggregateTime(), startDataDO.getFullValue(), endDataDO.getFullValue());
        // 首尾排除
        LocalDateTime start = input.getStartDataDO().getAggregateTime().plusMinutes(1);
        LocalDateTime end = input.getEndDataDO().getAggregateTime().minusMinutes(1);

        // 如果 start > end，说明之间没有间隔，直接返回空
        if (start.isAfter(end)) {
            return result;
        }

        while (!start.toLocalDate().isAfter(end.toLocalDate())) {
            LocalDateTime segStart = start;
            LocalDateTime segEnd = segStart.toLocalDate().atTime(23, 59);
            if (segEnd.isAfter(end)) segEnd = end;

            MinuteAggregateDataDTO toAddStartDTO = BeanUtil.copyProperties(input.getStartDataDO(), MinuteAggregateDataDTO.class);
            MinuteAggregateDataDTO toAddEndDTO = BeanUtil.copyProperties(input.getEndDataDO(), MinuteAggregateDataDTO.class);

            toAddStartDTO.setAggregateTime(segStart);
            toAddStartDTO.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());
            toAddEndDTO.setAggregateTime(segEnd);
            toAddEndDTO.setAcqFlag(AcqFlagEnum.NOT_ACQ.getCode());

            // 计算每个时间段的
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

    private String getQueueKey(LocalDateTime dateTime) {
        return SPLIT_TASK_QUEUE_REDIS_KEY_PREFIX + dateTime.toLocalDate();
    }
}