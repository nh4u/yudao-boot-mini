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
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
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

    private LinkedHashMap<String, String> getItemMapping() {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        List<String> gasSbLabels = DictFrameworkUtils.getDictDataLabelList(REPORT_NATURAL_GAS);
        for (String label : gasSbLabels) {
            String sbCode = DictFrameworkUtils.parseDictDataValue(REPORT_NATURAL_GAS, label);
            result.put(label, sbCode);
        }
        return result;
    }

    private BaseReportResultVO<NaturalGasInfo> defaultNullData(LinkedHashMap<String, String> itemMapping, List<String> tableHeader) {
        BaseReportResultVO<NaturalGasInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
        resultVO.setDataTime(LocalDateTime.now());
        List<NaturalGasInfo> infoList = new ArrayList<>();
        itemMapping.forEach((itemName, sbCode) -> {
            NaturalGasInfo info = new NaturalGasInfo();
            info.setItemName(itemName);
            info.setNaturalGasInfoDataList(Collections.emptyList());
            infoList.add(info);
        });
        resultVO.setReportDataList(infoList);
        return resultVO;
    }

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


        // 查询字典统计项
        LinkedHashMap<String, String> itemMapping = getItemMapping();

        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();


        Map<String, Long> sbMapping = allStandingbookDTOList.stream()
                .filter(dto -> itemMapping.containsValue(dto.getCode()))
                .collect(Collectors.toMap(
                        StandingbookDTO::getCode,
                        StandingbookDTO::getStandingbookId
                ));
        // 查询不到台账信息,返回空
        if (CollUtil.isEmpty(sbMapping)) {
            return defaultNullData(itemMapping, tableHeader);
        }


        // 查询 热力计量器具对应的用量使用情况；
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], new ArrayList<>(sbMapping.values()));

        List<NaturalGasInfo> NaturalGasInfoList = queryDefaultData(usageCostDataList, sbMapping, itemMapping);

        //返回结果
        BaseReportResultVO<NaturalGasInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
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

        resultVO.setDataTime(getLastTime(paramVO.getRange()[0], paramVO.getRange()[1], new ArrayList<>(sbMapping.values())));
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
    public BaseReportMultiChartResultVO<Map<String,List<BigDecimal>>> getChart(BaseTimeDateParamVO paramVO) {
        // 校验参数
        validCondition(paramVO);

        String cacheKey = NATURAL_GAS_CHART + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportMultiChartResultVO<Map<String,List<BigDecimal>>>>() {
            });
        }
        BaseReportMultiChartResultVO<Map<String,List<BigDecimal>>> resultVO = new BaseReportMultiChartResultVO<>();
        // x轴
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        resultVO.setXdata(xdata);


        LinkedHashMap<String, String> itemMapping = getItemMapping();

        List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();

        Map<String, Long> sbMapping = allStandingbookDTOList.stream()
                .filter(dto -> itemMapping.containsValue(dto.getCode()))
                .collect(Collectors.toMap(
                        StandingbookDTO::getCode,
                        StandingbookDTO::getStandingbookId
                ));


        if (CollUtil.isEmpty(sbMapping)) {
            resultVO.setDataTime(LocalDateTime.now());
            resultVO.setYdata(Collections.emptyMap());
            return resultVO;
        }

        // 查询 热力计量器具对应的用量使用情况；
        List<UsageCostData> usageCostDataList = usageCostService.getUsageByStandingboookIdGroup(paramVO, paramVO.getRange()[0], paramVO.getRange()[1], new ArrayList<>(sbMapping.values()));

        if (CollUtil.isEmpty(usageCostDataList)) {
            resultVO.setDataTime(LocalDateTime.now());
            resultVO.setYdata(Collections.emptyMap());
            return resultVO;
        }
        Map<Long, Map<String, BigDecimal>> standingbookIdTimeCostMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(
                        UsageCostData::getStandingbookId,
                        Collectors.toMap(
                                UsageCostData::getTime,
                                UsageCostData::getCurrentTotalUsage
                        )
                ));
        Map<String, List<BigDecimal>> ydataListMap = new HashMap<>();
        // 获取每个统计项的数据
        itemMapping.values().forEach(sbCode -> {
            Map<String, BigDecimal> sbCodeMap = standingbookIdTimeCostMap.get(sbMapping.get(sbCode));
            if (CollUtil.isEmpty(sbCodeMap)) {
                sbCodeMap = new HashMap<>();
            }
            Map<String, BigDecimal> finalSbCodeMap = sbCodeMap;
            List<BigDecimal> sbDataList = xdata.stream().map(time -> {
                time = dealStrTime(time);
                return dealBigDecimalScale(finalSbCodeMap.getOrDefault(time, BigDecimal.ZERO), scale);
            }).collect(Collectors.toList());
            ydataListMap.put(sbCode, sbDataList);
        });
        // 初始化汇总列表，长度和 xdata 一样，初始值为 0
        List<BigDecimal> sumList = new ArrayList<>(xdata.size());
        for (int i = 0; i < xdata.size(); i++) {
            sumList.add(BigDecimal.ZERO);
        }

        // 遍历每个 sbCode 的数据列表，逐项累加
        for (List<BigDecimal> sbDataList : ydataListMap.values()) {
            for (int i = 0; i < sbDataList.size(); i++) {
                sumList.set(i, sumList.get(i).add(sbDataList.get(i)));
            }
        }

        List<BigDecimal> scaledSumList = sumList.stream()
                .map(val -> dealBigDecimalScale(val, scale))
                .collect(Collectors.toList());

        // 放入 map 中
        ydataListMap.put("汇总", scaledSumList);

        Map<String,List<BigDecimal>> map = new HashMap<>();
        itemMapping.forEach((k,v)->{
            map.put(k,ydataListMap.get(v));
        });

        map.put("汇总",ydataListMap.get("汇总"));
        resultVO.setYdata(map);

        LocalDateTime lastTime = getLastTime(paramVO.getRange()[0], paramVO.getRange()[1], new ArrayList<>(sbMapping.values()));
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
        list.add(Arrays.asList("表单名称", "统计周期", ""));
        String sheetName = "天然气用量";
        // 统计周期
        String period = getFormatTime(paramVO.getRange()[0]) + "~" + getFormatTime(paramVO.getRange()[1]);

        // 月份处理
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));
        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, period, x));
        });
        list.add(Arrays.asList(sheetName, period, "周期合计"));
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


    private List<NaturalGasInfo> queryDefaultData(List<UsageCostData> usageCostDataList, Map<String, Long> sbMapping, LinkedHashMap<String, String> itemMapping) {
        // 聚合数据按台账id分组
        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        List<NaturalGasInfo> resultList = new ArrayList<>();
        // 循环统计项
        itemMapping.forEach((itemName, sbCode) -> {
            NaturalGasInfo info = new NaturalGasInfo();
            info.setItemName(itemName);
            if (CollUtil.isEmpty(standingBookUsageMap)) {
                info.setNaturalGasInfoDataList(Collections.emptyList());
                resultList.add(info);
                return;
            }
            List<UsageCostData> usageCostList = standingBookUsageMap.get(sbMapping.get(sbCode));
            if (CollUtil.isEmpty(usageCostList)) {
                info.setNaturalGasInfoDataList(Collections.emptyList());
                resultList.add(info);
                return;
            }
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

