package cn.bitlinks.ems.module.power.service.statistics.deviation;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.enums.QueryDimensionEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationChartYInfo;
import cn.bitlinks.ems.module.power.controller.admin.statistics.deviation.vo.DeviationStatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.statistics.StatisticsCommonService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.bitlinks.ems.module.power.service.voucher.VoucherService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.END_TIME_MUST_AFTER_START_TIME;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.QUERY_TYPE_NOT_EXISTS;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.DEVIATION_CHART;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
@Slf4j
public class DeviationServiceImpl implements DeviationService {

    @Resource
    private VoucherService voucherService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StatisticsCommonService statisticsCommonService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;


    @Override
    public DeviationChartResultVO<DeviationChartYInfo> deviationChart(DeviationStatisticsParamVO paramVO) {
        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());

        // 2.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = DEVIATION_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
//        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
//            log.info("缓存结果");
//            // 泛型放缓存避免强转问题
//            return JSON.parseObject(cacheRes, new TypeReference<DeviationChartResultVO<DeviationChartYInfo>>() {
//            });
//
//        }

        // 3.如果没有则去数据库查询
        // 3.1. 构建返回体
        DeviationChartResultVO<DeviationChartYInfo> resultVO = new DeviationChartResultVO<>();
        List<DeviationChartYInfo> yInfoList = ListUtils.newArrayList();

        // 3.2.x轴处理
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], DataTypeEnum.MONTH);
        resultVO.setXdata(xdata);

        // 3.3.1.根据能源id查询台账
        Long energyId = paramVO.getEnergyId();
        List<Long> energyIds = ListUtils.newArrayList(energyId);
        List<StandingbookDO> standingBookIdsByEnergy = statisticsCommonService.getStandingbookIdsByEnergy(energyIds);

        List<Long> standingBookIds = standingBookIdsByEnergy
                .stream()
                .map(StandingbookDO::getId)
                .collect(Collectors.toList());

        // 3.3.2.台账id为空直接返回结果
        if (CollUtil.isEmpty(standingBookIds)) {

            DeviationChartYInfo system = new DeviationChartYInfo();
            system.setName(SYSTEM);
            system.setData(Collections.emptyList());

            yInfoList.add(system);

            DeviationChartYInfo voucher = new DeviationChartYInfo();
            voucher.setName(VOUCHER);
            voucher.setData(Collections.emptyList());
            resultVO.setYdata(yInfoList);

            return resultVO;
        }

        // 3.4. 获取系统数据
        // 根据台账ID查询用量
        List<UsageCostData> usageCostDataList = usageCostService.getEnergyUsage(
                1,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);

        Map<String, BigDecimal> timeUsageMap = usageCostDataList.stream()
                .collect(Collectors.toMap(UsageCostData::getTime, UsageCostData::getCurrentTotalUsage));

        List<BigDecimal> ydata1 = xdata
                .stream()
                .map(time -> {
                    BigDecimal standardCoal = timeUsageMap.get(time);
                    return dealBigDecimalScale(standardCoal, DEFAULT_SCALE);
                })
                .collect(Collectors.toList());

        DeviationChartYInfo system = new DeviationChartYInfo();
        system.setName(SYSTEM);
        system.setData(ydata1);

        yInfoList.add(system);


        // 3.5. 获取凭证数据
        List<VoucherDO> voucherList = voucherService.getVoucherByEnergy(energyId, rangeOrigin);
        Map<String, BigDecimal> voucherUsageMap = voucherList.stream()
                .collect(Collectors.toMap(
                        key -> LocalDateTimeUtils.getFormatTime(key.getPurchaseTime(), DataTypeEnum.MONTH),
                        VoucherDO::getUsage));

        List<BigDecimal> ydata2 = xdata
                .stream()
                .map(time -> {
                    BigDecimal standardCoal = voucherUsageMap.get(time);
                    return dealBigDecimalScale(standardCoal, DEFAULT_SCALE);
                })
                .collect(Collectors.toList());

        DeviationChartYInfo voucher = new DeviationChartYInfo();
        voucher.setName(VOUCHER);
        voucher.setData(ydata2);
        yInfoList.add(voucher);

        // 保存y数据
        resultVO.setYdata(yInfoList);

        // 4.能源单位
        EnergyConfigurationDO energy = energyConfigurationService.getEnergyAndUnit(energyId);

        if (Objects.nonNull(energy)) {
            resultVO.setUnit(energy.getUnit());
        }

        // 5.获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTime(
                1,
                paramVO.getRange()[0],
                paramVO.getRange()[1],
                standingBookIds);
        resultVO.setDataTime(lastTime);

        // 6.结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }

    /**
     * 校验时间范围
     *
     * @param rangeOrigin
     * @return
     */
    private LocalDateTime[] validateRange(LocalDateTime[] rangeOrigin) {
        // 1.校验时间范围
        // 1.1.校验结束时间必须大于开始时间
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        return rangeOrigin;
    }

    /**
     * 校验查看类型
     *
     * @param queryType
     */
    private Integer validateQueryType(Integer queryType) {

        QueryDimensionEnum queryDimensionEnum = QueryDimensionEnum.codeOf(queryType);
        // 查看类型不存在
        if (Objects.isNull(queryDimensionEnum)) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }

        return queryType;
    }

}
