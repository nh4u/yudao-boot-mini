package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.tenant.core.aop.TenantIgnore;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import cn.bitlinks.ems.module.acquisition.service.partition.PartitionService;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.STREAM_LOAD_RANGE_FAIL;

/**
 * 分钟聚合数据service
 */
@DS("starrocks")
@Service
@Validated
@Slf4j
public class MinuteAggregateDataServiceImpl implements MinuteAggregateDataService {

    @Resource
    private MinuteAggregateDataMapper minuteAggregateDataMapper;

    @Resource
    private StarRocksStreamLoadService starRocksStreamLoadService;


    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Value("${rocketmq.topic.device-aggregate}")
    private String deviceAggTopic;

    @Resource
    private PartitionService partitionService;
    @Override
    @TenantIgnore
    public MinuteAggregateDataDTO selectByAggTime(Long standingbookId, LocalDateTime thisCollectTime) {
        MinuteAggregateDataDO minuteAggregateDataDO = minuteAggregateDataMapper.selectExactData(standingbookId,
                thisCollectTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    @TenantIgnore
    public MinuteAggregateDataDTO selectLatestByAggTime(Long standingbookId, LocalDateTime currentCollectTime) {
        MinuteAggregateDataDO minuteAggregateDataDO = minuteAggregateDataMapper.selectLatestDataByAggTime(standingbookId,
                currentCollectTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    @TenantIgnore
    public MinuteAggregateDataDTO selectOldestByStandingBookId(Long standingbookId) {
        MinuteAggregateDataDO minuteAggregateDataDO =
                minuteAggregateDataMapper.selectOldestByStandingBookId(standingbookId);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    @TenantIgnore
    public MinuteAggregateDataDTO selectLatestByStandingBookId(Long standingbookId) {
        MinuteAggregateDataDO minuteAggregateDataDO =
                minuteAggregateDataMapper.selectLatestByStandingBookId(standingbookId);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    public void sendMsgToUsageCostBatch(List<MinuteAggregateDataDO> aggDataList) throws IOException {
        if (CollUtil.isEmpty(aggDataList)) {
            return;
        }
        List<List<MinuteAggregateDataDO>> batchList = CollUtil.split(aggDataList, batchSize);
        for (List<MinuteAggregateDataDO> batch : batchList) {
            // 执行你的批量插入操作，比如：
            String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
            starRocksStreamLoadService.streamLoadData(batch, labelName, MINUTE_AGGREGATE_DATA_TB_NAME);
            // 发送mq消息
            String topicName = deviceAggTopic;
            // 发送消息
            Message<List<MinuteAggregateDataDTO>> msg =
                    MessageBuilder.withPayload(BeanUtils.toBean(batch, MinuteAggregateDataDTO.class)).build();
            rocketMQTemplate.send(topicName, msg);
        }

    }


    @Override
    @TenantIgnore
    @Transactional
    public void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        try {

            List<MinuteAggregateDataDO> minuteAggregateDataDOS = splitData(minuteAggDataSplitDTO.getStartDataDO(),
                    minuteAggDataSplitDTO.getEndDataDO());
            // 创建分区
            partitionService.createPartitions(MINUTE_AGGREGATE_DATA_TB_NAME, minuteAggregateDataDOS.get(0).getAggregateTime(),minuteAggregateDataDOS.get(minuteAggregateDataDOS.size()-1).getAggregateTime());
            // 发送给usageCost进行计算
            sendMsgToUsageCostBatch(minuteAggregateDataDOS);
        } catch (Exception e) {
            log.error("insertRangeData失败：{}", e.getMessage(), e);
            throw exception(STREAM_LOAD_RANGE_FAIL);
        }
    }

    /**
     * 根据分钟级的数据进行数据拆分，填充两端时间之间的分钟级别数据，计算出全量和增量值，塞到MinuteAggregateDataDO中
     *
     * @param startData 开始数据
     * @param endData   结束数据
     */
    private List<MinuteAggregateDataDO> splitData(MinuteAggregateDataDTO startData, MinuteAggregateDataDTO endData) {

        LocalDateTime startTime = startData.getAggregateTime();
        LocalDateTime endTime = endData.getAggregateTime();
        BigDecimal startValue = startData.getFullValue();
        BigDecimal endValue = endData.getFullValue();

        List<MinuteAggregateDataDO> result = new ArrayList<>();

        // 计算时间差（分钟）
        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return result; // 无需处理
        }

        // 计算每分钟的增量值
        BigDecimal totalIncrement = endValue.subtract(startValue);
        BigDecimal perMinuteIncrement = totalIncrement.divide(BigDecimal.valueOf(minutes), 10, RoundingMode.HALF_UP);

        // 初始化当前时间和当前全量值
        LocalDateTime currentTime = startTime;
        BigDecimal currentFullValue = startValue;

        for (int i = 0; i <= minutes; i++) {
            MinuteAggregateDataDO data = new MinuteAggregateDataDO();
            data.setStandingbookId(startData.getStandingbookId());
            data.setParamCode(startData.getParamCode());
            data.setEnergyFlag(startData.getEnergyFlag());
            data.setDataSite(startData.getDataSite());
            data.setAggregateTime(currentTime);
            data.setFullValue(currentFullValue);
            data.setIncrementalValue(perMinuteIncrement);
            if (i == 0) {
                if (Objects.isNull(endData.getIncrementalValue())) {
                    //这个是历史时间段之后添加的连续数据，两个时间点全量都有，需要计算出最后一个时间点的增量和，时间范围之间的分钟级数据的增量
                    //第一条数据的值，还是第一条数据的值，不需要加入新增的队列中
                    data.setIncrementalValue(startData.getIncrementalValue());
                    // 更新当前时间和全量值
                    currentTime = currentTime.plusMinutes(1);
                    currentFullValue = currentFullValue.add(perMinuteIncrement);
                    continue;
                }
                if (endData.getIncrementalValue().equals(BigDecimal.ZERO)) {
                    //这个是历史时间段之前添加的连续数据，都是全量，第一个时间点的增量为0不需要动，需要计算出最后一个时间点的增量和时间范围之间的分钟级别数据的增量
                    data.setIncrementalValue(BigDecimal.ZERO);
                }
            }
            if (i == minutes) {
                data.setFullValue(endValue);
            }

            // 如果开始时间为空，按
            result.add(data);
            // 更新当前时间和全量值
            currentTime = currentTime.plusMinutes(1);
            currentFullValue = currentFullValue.add(perMinuteIncrement);
        }

        return result;
    }
}
