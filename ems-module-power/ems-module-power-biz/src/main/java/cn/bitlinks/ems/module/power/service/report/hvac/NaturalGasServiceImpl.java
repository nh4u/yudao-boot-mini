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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.dealStrTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.REPORT_NATURAL_GAS;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.NATURAL_GAS_CHART;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.NATURAL_GAS_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.getConvertData;
@Service
@Validated
@Slf4j
public class NaturalGasServiceImpl implements NaturalGasService {
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private UsageCostService usageCostService;

    private final Integer scale = DEFAULT_SCALE;


    @Override
    public BaseReportResultVO<NaturalGasInfo> getTable(BaseTimeDateParamVO paramVO) {
        // 校验参数
        validCondition(paramVO);
        String cacheKey = NATURAL_GAS_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportResultVO<NaturalGasInfo>>() {
            });
        }

        // 表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));

        //返回结果
        BaseReportResultVO<NaturalGasInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
        // 查询热力的计量器具
        List<String> gasSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_NATURAL_GAS);
        String gasSbLabel = gasSbLabels.get(0);
        String gasSbCode = DictFrameworkUtils.parseDictDataValue(REPORT_NATURAL_GAS, gasSbLabel);
        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();

        StandingbookDTO targetDTO = allStandingbookDTOList.stream()
                .filter(dto -> gasSbCode.equals(dto.getCode()))
                .findFirst()
                .orElse(null);
        if (Objects.isNull(targetDTO)) {
            resultVO.setDataTime(LocalDateTime.now());
            return resultVO;
        }


        // 查询 热力计量器具对应的用量使用情况；
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], Collections.singletonList(targetDTO.getStandingbookId()));

        List<NaturalGasInfo> NaturalGasInfoList = queryDefaultData(usageCostDataList);


        resultVO.setReportDataList(NaturalGasInfoList);

        // 无数据的填充0
        NaturalGasInfoList.forEach(l -> {

            List<NaturalGasInfoData> newList = new ArrayList<>();
            List<NaturalGasInfoData> oldList = l.getNaturalGasInfoDataList();
            if (tableHeader.size() != oldList.size()) {
                Map<String, List<NaturalGasInfoData>> dateMap = oldList.stream()
                        .collect(Collectors.groupingBy(NaturalGasInfoData::getDate));

                tableHeader.forEach(date -> {
                    List<NaturalGasInfoData> NaturalGasInfoDataList = dateMap.get(date);
                    if (NaturalGasInfoDataList == null) {
                        NaturalGasInfoData NaturalGasInfoData = new NaturalGasInfoData();
                        NaturalGasInfoData.setDate(date);
                        NaturalGasInfoData.setConsumption(BigDecimal.ZERO);
                        newList.add(NaturalGasInfoData);
                    } else {
                        newList.add(NaturalGasInfoDataList.get(0));
                    }
                });
                // 设置新数据list
                l.setNaturalGasInfoDataList(newList);
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

        String cacheKey = NATURAL_GAS_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportChartResultVO<BigDecimal>>() {
            });
        }

        // 查询热力的计量器具
        List<String> gasSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_NATURAL_GAS);
        String gasSbLabel = gasSbLabels.get(0);
        String gasSbCode = DictFrameworkUtils.parseDictDataValue(REPORT_NATURAL_GAS, gasSbLabel);

        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();


        StandingbookDTO targetDTO = allStandingbookDTOList.stream()
                .filter(dto -> gasSbCode.equals(dto.getCode()))
                .findFirst()
                .orElse(null);
        BaseReportChartResultVO<BigDecimal> resultVO = new BaseReportChartResultVO<>();
        // x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        resultVO.setXdata(xdata);
        if (Objects.isNull(targetDTO)) {
            resultVO.setDataTime(LocalDateTime.now());
            resultVO.setYdata(Collections.emptyList());
            return resultVO;
        }

        // 查询 热力计量器具对应的用量使用情况；
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], Collections.singletonList(targetDTO.getStandingbookId()));

        if (CollUtil.isEmpty(usageCostDataList)) {
            resultVO.setDataTime(LocalDateTime.now());
            resultVO.setYdata(Collections.emptyList());
            return resultVO;
        }


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

    @Override
    public List<List<String>> getExcelHeader(BaseTimeDateParamVO paramVO) {

        validCondition(paramVO);

        List<List<String>> list = ListUtils.newArrayList();
        // 第一格处理
        list.add(Arrays.asList(""));

        // 月份处理
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        xdata.forEach(x -> {
            list.add(Arrays.asList(x));
        });
        list.add(Arrays.asList("周期合计"));
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(BaseTimeDateParamVO paramVO) {
        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();

        BaseReportResultVO<NaturalGasInfo> resultVO = getTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<NaturalGasInfo> NaturalGasInfoList = resultVO.getReportDataList();

        for (NaturalGasInfo s : NaturalGasInfoList) {

            List<Object> data = ListUtils.newArrayList();

            data.add(s.getItemName());

            // 处理数据
            List<NaturalGasInfoData> NaturalGasInfoDataList = s.getNaturalGasInfoDataList();

            Map<String, NaturalGasInfoData> dateMap = NaturalGasInfoDataList.stream()
                    .collect(Collectors.toMap(NaturalGasInfoData::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                NaturalGasInfoData NaturalGasInfoData = dateMap.get(date);
                if (NaturalGasInfoData == null) {
                    data.add("/");
                } else {
                    BigDecimal consumption = NaturalGasInfoData.getConsumption();
                    data.add(getConvertData(consumption));
                }
            });

            BigDecimal periodSum = s.getPeriodSum();
            // 处理周期合计
            data.add(getConvertData(periodSum));

            result.add(data);
        }

        return result;
    }


    private LocalDateTime getLastTime(LocalDateTime start, LocalDateTime end, List<Long> standingbookIds) {
        return usageCostService.getLastTimeNoParam(start, end, standingbookIds);
    }


    private List<NaturalGasInfo> queryDefaultData(List<UsageCostData> usageCostDataList) {
        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        List<NaturalGasInfo> resultList = new ArrayList<>();
        List<String> gasSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_NATURAL_GAS);
        standingBookUsageMap.forEach((standingbookId, usageCostList) -> {
            // 获取热力台账的名称
            String gasSbLabel = gasSbLabels.get(0);
            // 聚合数据 转换成 NaturalGasInfoData
            List<NaturalGasInfoData> dataList = new ArrayList<>(usageCostList.stream().collect(Collectors.groupingBy(
                    UsageCostData::getTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                BigDecimal totalConsumption = list.stream()
                                        .map(UsageCostData::getCurrentTotalUsage)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                                return new NaturalGasInfoData(list.get(0).getTime(), totalConsumption);
                            }
                    )
            )).values());

            BigDecimal totalConsumption = dataList.stream()
                    .map(NaturalGasInfoData::getConsumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            NaturalGasInfo info = new NaturalGasInfo();
            info.setItemName(gasSbLabel);

            dataList = dataList.stream().peek(i -> {
                i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
            }).collect(Collectors.toList());
            info.setNaturalGasInfoDataList(dataList);
            info.setPeriodSum(dealBigDecimalScale(totalConsumption, scale));

            resultList.add(info);
        });

        return resultList;
    }
}

