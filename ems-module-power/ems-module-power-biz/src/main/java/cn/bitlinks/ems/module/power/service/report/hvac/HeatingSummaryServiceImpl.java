package cn.bitlinks.ems.module.power.service.report.hvac;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.dealStrTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.REPORT_HVAC_HEAT;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.HVAC_HEATING_SUMMARY_CHART;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.HVAC_HEATING_SUMMARY_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

@Service
@Validated
@Slf4j
public class HeatingSummaryServiceImpl implements HeatingSummaryService {
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private UsageCostService usageCostService;

    private final Integer scale = DEFAULT_SCALE;


    @Override
    public BaseReportResultVO<HeatingSummaryInfo> getTable(BaseTimeDateParamVO paramVO) {
        // 校验参数
        validCondition(paramVO);
        String cacheKey = HVAC_HEATING_SUMMARY_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportResultVO<HeatingSummaryInfo>>() {
            });
        }

        // 表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));

        //返回结果
        BaseReportResultVO<HeatingSummaryInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
        // 查询热力的计量器具
        List<String> heatingSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_HVAC_HEAT);
        String heatingSbLabel = heatingSbLabels.get(0);
        String heatingSbCode = DictFrameworkUtils.getDictDataLabel(REPORT_HVAC_HEAT, heatingSbLabel);
        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();

        StandingbookDTO targetDTO = allStandingbookDTOList.stream()
                .filter(dto -> heatingSbCode.equals(dto.getCode()))
                .findFirst()
                .orElse(null);
        if (Objects.isNull(targetDTO)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }


        // 查询 热力计量器具对应的用量使用情况；
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], Collections.singletonList(targetDTO.getStandingbookId()));

        List<HeatingSummaryInfo> heatingSummaryInfoList = queryDefaultData(usageCostDataList);


        resultVO.setReportDataList(heatingSummaryInfoList);

        // 无数据的填充0
        heatingSummaryInfoList.forEach(l -> {

            List<HeatingSummaryInfoData> newList = new ArrayList<>();
            List<HeatingSummaryInfoData> oldList = l.getHeatingSummaryInfoDataList();
            if (tableHeader.size() != oldList.size()) {
                Map<String, List<HeatingSummaryInfoData>> dateMap = oldList.stream()
                        .collect(Collectors.groupingBy(HeatingSummaryInfoData::getDate));

                tableHeader.forEach(date -> {
                    List<HeatingSummaryInfoData> heatingSummaryInfoDataList = dateMap.get(date);
                    if (heatingSummaryInfoDataList == null) {
                        HeatingSummaryInfoData heatingSummaryInfoData = new HeatingSummaryInfoData();
                        heatingSummaryInfoData.setDate(date);
                        heatingSummaryInfoData.setConsumption(BigDecimal.ZERO);
                        newList.add(heatingSummaryInfoData);
                    } else {
                        newList.add(heatingSummaryInfoDataList.get(0));
                    }
                });
                // 设置新数据list
                l.setHeatingSummaryInfoDataList(newList);
            }
        });

        resultVO.setDataTime(getLastTime(paramVO.getRange()[0], paramVO.getRange()[1], Collections.singletonList(targetDTO.getStandingbookId())));
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }

    private void validCondition(BaseTimeDateParamVO paramVO) {
        LocalDateTime[] rangeOrigin = paramVO.getRange();
        LocalDateTime startTime = rangeOrigin[0];
        LocalDateTime endTime = rangeOrigin[1];
        if (!startTime.isBefore(endTime)) {
            throw exception(END_TIME_MUST_AFTER_START_TIME);
        }
        //时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(paramVO.getDateType());
        //时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }
    }

    @Override
    public BaseReportChartResultVO<BigDecimal> getChart(BaseTimeDateParamVO paramVO) {
        // 校验参数
        validCondition(paramVO);

        String cacheKey = HVAC_HEATING_SUMMARY_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportChartResultVO<BigDecimal>>() {
            });
        }

        // 查询热力的计量器具
        List<String> heatingSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_HVAC_HEAT);
        String heatingSbLabel = heatingSbLabels.get(0);
        String heatingSbCode = DictFrameworkUtils.getDictDataLabel(REPORT_HVAC_HEAT, heatingSbLabel);

        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();


        StandingbookDTO targetDTO = allStandingbookDTOList.stream()
                .filter(dto -> heatingSbCode.equals(dto.getCode()))
                .findFirst()
                .orElse(null);
        BaseReportChartResultVO<BigDecimal> resultVO = new BaseReportChartResultVO<>();
        if (Objects.isNull(targetDTO)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }

        // 查询 热力计量器具对应的用量使用情况；
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], Collections.singletonList(targetDTO.getStandingbookId()));

        if (CollUtil.isEmpty(usageCostDataList)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }

        // x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        resultVO.setXdata(xdata);

        // 按能源查看
        Map<Long, Map<String, BigDecimal>> standingbookIdTimeCostMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getStandingbookId,
                        Collectors.toMap(
                                UsageCostData::getTime,
                                UsageCostData::getCurrentTotalUsage
                        )
                ));
        Map<String, BigDecimal> timeCostMap = standingbookIdTimeCostMap.get(targetDTO.getStandingbookId());
        List<BigDecimal> ydataList = xdata.stream().map(time -> {
            time = dealStrTime(time);
            return dealBigDecimalScale(timeCostMap.getOrDefault(time, BigDecimal.ZERO), scale);
        }).collect(Collectors.toList());
        resultVO.setYdata(ydataList);

        LocalDateTime lastTime = getLastTime(paramVO.getRange()[0], paramVO.getRange()[1], Collections.singletonList(targetDTO.getStandingbookId()));
        resultVO.setDataTime(lastTime);
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }

    private LocalDateTime getLastTime(LocalDateTime start, LocalDateTime end, List<Long> standingbookIds) {
        return usageCostService.getLastTimeNoParam(start, end, standingbookIds);
    }


    private List<HeatingSummaryInfo> queryDefaultData(List<UsageCostData> usageCostDataList) {
        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        List<HeatingSummaryInfo> resultList = new ArrayList<>();
        List<String> heatingSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_HVAC_HEAT);
        standingBookUsageMap.forEach((standingbookId, usageCostList) -> {
            // 获取热力台账的名称
            String heatingSbLabel = heatingSbLabels.get(0);
            // 聚合数据 转换成 HeatingSummaryInfoData
            List<HeatingSummaryInfoData> dataList = new ArrayList<>(usageCostList.stream().collect(Collectors.groupingBy(
                    UsageCostData::getTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                BigDecimal totalConsumption = list.stream()
                                        .map(UsageCostData::getCurrentTotalUsage)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new HeatingSummaryInfoData(list.get(0).getTime(), totalConsumption);
                            }
                    )
            )).values());

            BigDecimal totalConsumption = dataList.stream()
                    .map(HeatingSummaryInfoData::getConsumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            HeatingSummaryInfo info = new HeatingSummaryInfo();
            info.setItemName(heatingSbLabel);

            dataList = dataList.stream().peek(i -> {
                i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
            }).collect(Collectors.toList());
            info.setHeatingSummaryInfoDataList(dataList);
            info.setPeriodSum(dealBigDecimalScale(totalConsumption, scale));

            resultList.add(info);
        });

        return resultList;
    }
}
