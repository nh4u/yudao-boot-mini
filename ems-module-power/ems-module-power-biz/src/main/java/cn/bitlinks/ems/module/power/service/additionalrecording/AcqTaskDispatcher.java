package cn.bitlinks.ems.module.power.service.additionalrecording;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.ACQ_TASK_QUEUE_REDIS_KEY;


@Slf4j
@Component
public class AcqTaskDispatcher {

    @Resource
    private RedissonClient redissonClient;

    private static final int BATCH_SIZE = 500; // 每批提交数量

    /**
     * 异步批量处理插入业务点数据
     */
    public void dispatchTaskBatch(List<MinuteAggregateDataDTO> inputList) {
        log.info("批量插入业务点任务，任务数量：{}", inputList.size());

        RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(ACQ_TASK_QUEUE_REDIS_KEY);
        double scoreBase = System.currentTimeMillis();

        Map<String, Double> batch = new LinkedHashMap<>(BATCH_SIZE);

        for (int i = 0; i < inputList.size(); i++) {
            MinuteAggregateDataDTO task = inputList.get(i);
            double score = scoreBase + (i * 0.000001);
            batch.put(JsonUtils.toJsonString(task), score);

            if (batch.size() >= BATCH_SIZE || i == inputList.size() - 1) {
                queue.addAll(batch); // 正确类型
                batch.clear();
            }
        }
    }


}
