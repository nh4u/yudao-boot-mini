package cn.bitlinks.ems.module.power.service.cophouraggdata;

import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.COP_RECALCULATE_HOUR_QUEUE;

@DS("starrocks")
@Slf4j
@Service
@Validated
public class RedisCopQueueService {
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public void pushHourForCopRecalc(LocalDateTime hourTime) {
        ZonedDateTime zoned = LocalDateTime.now().atZone(ZoneId.systemDefault());
        double score = zoned.toEpochSecond();
        redisTemplate.opsForZSet().add(COP_RECALCULATE_HOUR_QUEUE, hourTime.toString(), score);
    }
}
