package cn.bitlinks.ems.module.acquisition.service.minuteaggregatedata;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggDataSplitDTO;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.MinuteAggregateDataDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.minuteaggregatedata.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.acquisition.dal.mysql.minuteaggregatedata.MinuteAggregateDataMapper;
import cn.bitlinks.ems.module.acquisition.starrocks.StarRocksStreamLoadService;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.STREAM_LOAD_PREFIX;
import static cn.bitlinks.ems.module.acquisition.enums.ErrorCodeConstants.*;

/**
 * 分钟聚合数据service
 */
@DS("starrocks")
@Service
@Validated
public class MinuteAggregateDataServiceImpl implements MinuteAggregateDataService {

    @Resource
    private MinuteAggregateDataMapper minuteAggregateDataMapper;

    @Resource
    private StarRocksStreamLoadService starRocksStreamLoadService;

    private static final String TB_NAME = "minute_aggregate_data";

    @Override
    public MinuteAggregateDataDTO selectByAggTime(Long standingbookId, LocalDateTime thisCollectTime) {
        MinuteAggregateDataDO minuteAggregateDataDO = minuteAggregateDataMapper.selectExactData(standingbookId,
                thisCollectTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    public MinuteAggregateDataDTO selectLatestByAggTime(Long standingbookId, LocalDateTime currentCollectTime) {
        MinuteAggregateDataDO minuteAggregateDataDO = minuteAggregateDataMapper.selectLatestDataByAggTime(standingbookId,
                currentCollectTime);
        if (Objects.isNull(minuteAggregateDataDO)) {
            return null;
        }
        return BeanUtils.toBean(minuteAggregateDataDO, MinuteAggregateDataDTO.class);
    }

    @Override
    public MinuteAggregateDataDTO selectOldestByStandingBookId(Long standingbookId) {
        return minuteAggregateDataMapper.selectOldestByStandingBookId(standingbookId);
    }

    @Override
    public MinuteAggregateDataDTO selectLatestByStandingBookId(Long standingbookId) {
        return minuteAggregateDataMapper.selectLatestByStandingBookId(standingbookId);
    }

    @Override
    public void insertSingleData(MinuteAggregateDataDTO minuteAggregateDataDTO) {
        try {
            MinuteAggregateDataDO minuteAggregateDataDO = BeanUtils.toBean(minuteAggregateDataDTO,
                    MinuteAggregateDataDO.class);
            String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
            starRocksStreamLoadService.streamLoadData(Collections.singletonList(minuteAggregateDataDO), labelName, TB_NAME);
        } catch (Exception e) {
            throw exception(STREAM_LOAD_INIT_FAIL);
        }
    }

    @Override
    public void insertDelRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        try {
            MinuteAggregateDataDTO endDataDTO = minuteAggDataSplitDTO.getEndDataDO();
            // 按照起始两条数据，进行拆分，然后删除
            minuteAggregateDataMapper.deleteDataByMinute(endDataDTO.getAggregateTime(), endDataDTO.getStandingbookId());

            // 数据拆分
            List<MinuteAggregateDataDO> minuteAggregateDataDOS = splitData(minuteAggDataSplitDTO.getStartDataDO(), endDataDTO);

            String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
            starRocksStreamLoadService.streamLoadData(minuteAggregateDataDOS, labelName, TB_NAME);
        } catch (Exception e) {
            throw exception(STREAM_LOAD_DEL_RANGE_FAIL);
        }
    }

    @Override
    public void insertRangeData(MinuteAggDataSplitDTO minuteAggDataSplitDTO) {
        try {
            List<MinuteAggregateDataDO> minuteAggregateDataDOS = splitData(minuteAggDataSplitDTO.getStartDataDO(),
                    minuteAggDataSplitDTO.getEndDataDO());
            String labelName = System.currentTimeMillis() + STREAM_LOAD_PREFIX + RandomUtil.randomNumbers(6);
            starRocksStreamLoadService.streamLoadData(minuteAggregateDataDOS, labelName, TB_NAME);
        } catch (Exception e) {
            throw exception(STREAM_LOAD_RANGE_FAIL);
        }
    }

    /**
     * 根据分钟级的数据进行数据拆分，填充两端时间之间的分钟级别数据，计算出全量和增量值，塞到MinuteAggregateDataDO中
     *
     * @param startData  开始数据
     * @param endData    结束数据
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

        for (int i = 0; i < minutes; i++) {
            MinuteAggregateDataDO data = new MinuteAggregateDataDO();
            data.setStandingbookId(startData.getStandingbookId());
            data.setParamCode(startData.getParamCode());
            data.setEnergyFlag(startData.getEnergyFlag());
            data.setDataSite(startData.getDataSite());
            data.setAggregateTime(currentTime);
            data.setFullValue(currentFullValue);
            data.setIncrementalValue(perMinuteIncrement);
            if(i==0) {
                if (Objects.isNull(endData.getIncrementalValue())){
                    //这个是历史时间段之后添加的连续数据，两个时间点全量都有，需要计算出最后一个时间点的增量和，时间范围之间的分钟级数据的增量
                    //第一条数据的值，还是第一条数据的值，不需要加入新增的队列中
                    data.setIncrementalValue(startData.getIncrementalValue());
                    continue;
                }
                if (endData.getIncrementalValue().equals(BigDecimal.ZERO)) {
                    //这个是历史时间段之前添加的连续数据，都是全量，第一个时间点的增量为0不需要动，需要计算出最后一个时间点的增量和时间范围之间的分钟级别数据的增量
                    data.setIncrementalValue(BigDecimal.ZERO);
                }
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
