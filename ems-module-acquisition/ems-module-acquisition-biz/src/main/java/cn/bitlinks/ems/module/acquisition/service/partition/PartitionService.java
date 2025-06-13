package cn.bitlinks.ems.module.acquisition.service.partition;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.*;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.REDIS_MAX_PARTITION_NOT_EXIST;

@Service
@DS("starrocks")
@Slf4j
public class PartitionService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String partitionPrefix = "p";
    private static final DateTimeFormatter dateTimeFormatter = DatePattern.NORM_DATETIME_FORMATTER;
    private static final DateTimeFormatter pureDateFormatter = DatePattern.PURE_DATE_FORMATTER;
    @Value("${spring.profiles.active}")
    private String env;
    @Value("${ems.max-partition.minutes-agg}")
    private String maxPartitionMinutesAgg;
    @Value("${ems.max-partition.usage-cost}")
    private String maxPartitionUsageCost;
    @Resource
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initMaxPartitions(){
        // redis存储的最大的分区时间
        String maxPartitionMinutesAggKey = String.format(REDIS_KEY_MAX_PARTITION_TIME, env, MINUTE_AGGREGATE_DATA_TB_NAME);
        stringRedisTemplate.opsForValue().set(maxPartitionMinutesAggKey, maxPartitionMinutesAgg);
        String maxPartitionUsageCostKey = String.format(REDIS_KEY_MAX_PARTITION_TIME, env, USAGE_COST_TB_NAME);
        stringRedisTemplate.opsForValue().set(maxPartitionUsageCostKey, maxPartitionUsageCost);
    }

    /**
     * 创建分区
     *
     * @param tableName
     * @param startDateTime
     * @param endDateTime
     */
    public void createPartitions(String tableName, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        // redis存储的最大的分区时间
        String maxPartitionTimeKey = String.format(REDIS_KEY_MAX_PARTITION_TIME, env, tableName);

        String maxPartitionTimeStr = stringRedisTemplate.opsForValue().get(maxPartitionTimeKey);
        if (StringUtils.isEmpty(maxPartitionTimeStr)) {
            throw exception(REDIS_MAX_PARTITION_NOT_EXIST);
        }
        LocalDateTime maxPartitionTime = LocalDateTime.parse(maxPartitionTimeStr, dateTimeFormatter);

        String hisPartitionListKey = String.format(REDIS_KEY_HIS_PARTITION_LIST, env, tableName);
        // 获取需要插入的批量分区
        List<String> partitionNames = new ArrayList<>();
        List<PartitionDayRange> partitionDayRanges = splitDayRangeWithPartition(startDateTime, endDateTime, maxPartitionTime, hisPartitionListKey, partitionNames);

        // 不需要创建历史分区
        if (CollUtil.isEmpty(partitionDayRanges)) {
            return;
        }

        String disableDynamicPartitionSql = "ALTER TABLE " + tableName + " SET (\"dynamic_partition.enable\" = \"false\")";
        String addPartitionsSql = buildAddPartitionsSQL(tableName, partitionDayRanges); // 构建 ADD PARTITIONS 语句
        String enableDynamicPartitionSql = "ALTER TABLE " + tableName + " SET (\"dynamic_partition.enable\" = \"true\")";
        try {

            // 分开执行
            jdbcTemplate.execute(disableDynamicPartitionSql);
            jdbcTemplate.execute(addPartitionsSql);
            jdbcTemplate.execute(enableDynamicPartitionSql);
            // 把手动维护的分区维护到redis之中
            savePartitions(tableName, partitionNames);
        } catch (Exception ex) {
            log.error("[StarRocksDDL] 创建分区执行 SQL 失败", ex);
            jdbcTemplate.execute(enableDynamicPartitionSql);
            throw new RuntimeException("执行 StarRocks DDL 失败: " + ex.getMessage(), ex);
        }


    }

    public String buildAddPartitionsSQL(String tableName, List<PartitionDayRange> partitionDayRanges) {
        StringBuilder sqlBuilder = new StringBuilder();
        for (PartitionDayRange p : partitionDayRanges) {
            String partitionSql = String.format(
                    "ALTER TABLE %s ADD PARTITION %s VALUES [(\"%s\"), (\"%s\"));",
                    tableName,
                    p.getPartitionName(),
                    p.getStartTime(),
                    p.getEndTime()
            );
            sqlBuilder.append(partitionSql).append("\n"); // 每条 SQL 之后加换行方便日志查看
        }

        return sqlBuilder.toString();

    }


    /**
     * 批量插入redis 手动维护的历史分区
     *
     * @param tableName
     * @param partitionNames
     */
    private void savePartitions(String tableName, List<String> partitionNames) {
        String key = String.format(REDIS_KEY_HIS_PARTITION_LIST, env, tableName);

        Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
        for (String partitionName : partitionNames) {
            String datePart = partitionName.substring(1); // 去掉 'p'
            LocalDate date = LocalDate.parse(datePart, pureDateFormatter);
            double score = date.toEpochDay(); // 用 epochDay 排序

            ZSetOperations.TypedTuple<String> tuple = new DefaultTypedTuple<>(partitionName, score);
            tuples.add(tuple);
        }

        stringRedisTemplate.opsForZSet().add(key, tuples);
    }


    /**
     * 按照天拆分时间段 包含两段时间
     *
     * @param startDateTime
     * @param endDateTime
     * @param maxPartitionTime
     * @return
     */
    private List<PartitionDayRange> splitDayRangeWithPartition(LocalDateTime startDateTime, LocalDateTime endDateTime, LocalDateTime maxPartitionTime, String hisPartitionListKey, List<String> partitionNames) {
        List<PartitionDayRange> result = new ArrayList<>();

        DateTime start = DateTime.of(Date.from(startDateTime.truncatedTo(ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toInstant()));
        DateTime end = DateTime.of(Date.from(endDateTime.plusDays(1L).truncatedTo(ChronoUnit.DAYS).atZone(ZoneId.systemDefault()).toInstant()));

        DateRange range = DateUtil.range(start, end, DateField.DAY_OF_MONTH);


        for (DateTime day : range) {
            LocalDateTime startDay = day.toLocalDateTime();
            LocalDateTime endDay = startDay.plusDays(1);
            String partitionName = partitionPrefix + startDay.format(pureDateFormatter);
            // 判断在redis中最大分区之前，则手动维护到redis中，而且分区名称不在redis中维护
            // 分区不在动态分区之前，不需要维护创建
            if (!startDay.isBefore(maxPartitionTime)) {
                continue;
            }
            // 判断分区在历史分区中存在不存在
            boolean exists = stringRedisTemplate.opsForZSet().score(hisPartitionListKey, partitionName) != null;
            if (exists) {
                continue;
            }
            result.add(new PartitionDayRange(partitionName, startDay.format(dateTimeFormatter), endDay.format(dateTimeFormatter)));
            partitionNames.add(partitionName);
        }

        return result;
    }
}

