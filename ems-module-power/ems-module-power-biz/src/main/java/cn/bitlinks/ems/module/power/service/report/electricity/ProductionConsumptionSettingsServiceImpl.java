package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.report.electricity.ProductionConsumptionSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.electricity.ProductionConsumptionSettingsMapper;
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
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.PRODUCTION_CONSUMPTION_STATISTICS;
import static cn.bitlinks.ems.module.power.enums.StatisticsCacheConstants.USAGE_PRODUCTION_CONSUMPTION_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * @author liumingqiang
 */
@Slf4j
@Service
@Validated
public class ProductionConsumptionSettingsServiceImpl implements ProductionConsumptionSettingsService {

    @Resource
    private ProductionConsumptionSettingsMapper productionConsumptionSettingsMapper;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    private Integer scale = DEFAULT_SCALE;

    @Override
    public void updateBatch(List<ProductionConsumptionSettingsSaveReqVO> productionConsumptionList) {
        // 校验
        if (CollUtil.isEmpty(productionConsumptionList)) {
            throw exception(SUPPLY_ANALYSIS_SETTINGS_LIST_NOT_EXISTS);
        }

        // 统一保存
        List<ProductionConsumptionSettingsDO> list = BeanUtils.toBean(productionConsumptionList, ProductionConsumptionSettingsDO.class);
        list.forEach(l -> {
            if (Objects.isNull(l.getId())) {
                productionConsumptionSettingsMapper.insert(l);
            } else {
                productionConsumptionSettingsMapper.updateById(l);
            }
        });

    }

    @Override
    public List<ProductionConsumptionSettingsRespVO> getProductionConsumptionSettingsList(ProductionConsumptionSettingsPageReqVO pageReqVO) {


        List<ProductionConsumptionSettingsDO> productionConsumptionSettingsList = productionConsumptionSettingsMapper.selectList((new LambdaQueryWrapperX<ProductionConsumptionSettingsDO>()
                .eqIfPresent(ProductionConsumptionSettingsDO::getName, pageReqVO.getName())
                .orderByAsc(ProductionConsumptionSettingsDO::getId)));

        List<ProductionConsumptionSettingsRespVO> list = BeanUtils.toBean(productionConsumptionSettingsList, ProductionConsumptionSettingsRespVO.class);
        List<StandingbookDTO> standingbookDTOList = standingbookService.getStandingbookDTOList();

        Map<Long, StandingbookDTO> standingbookMap = standingbookDTOList
                .stream()
                .collect(Collectors.toMap(StandingbookDTO::getStandingbookId, Function.identity()));

        list.forEach(l -> {
            Long standingbookId = l.getStandingbookId();
            String name = "";
            StandingbookDTO standingbook = standingbookMap.get(standingbookId);
            if (!Objects.isNull(standingbook)) {
                name = standingbook.getName();
            }
            l.setStandingbookName(name);
        });
        return list;
    }

    @Override
    public List<String> getName() {
        return productionConsumptionSettingsMapper.getName();
    }

    @Override
    public StatisticsResultV2VO<ProductionConsumptionStatisticsInfo> productionConsumptionTable(ProductionConsumptionReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());

        // 2.2.校验时间类型
        Integer dateType = paramVO.getDateType();
        DataTypeEnum dataTypeEnum = validateDateType(dateType);

        // 3.查询对应缓存是否已经存在，如果存在这直接返回（如果查最新的，最新的在实时更新，所以缓存的是不对的）
        String cacheKey = USAGE_PRODUCTION_CONSUMPTION_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            log.info("缓存结果");
            // 泛型放缓存避免强转问题
            return JSON.parseObject(cacheRes, new TypeReference<StatisticsResultV2VO<ProductionConsumptionStatisticsInfo>>() {
            });
        }

        // 4.如果没有则去数据库查询
        StatisticsResultV2VO<ProductionConsumptionStatisticsInfo> resultVO = new StatisticsResultV2VO<>();
        resultVO.setDataTime(LocalDateTime.now());

        // 4.1.表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(range[0], range[1], dataTypeEnum);
        resultVO.setHeader(tableHeader);

        // 4.获取所有standingBookids
        List<ProductionConsumptionSettingsDO> productionConsumptionList = productionConsumptionSettingsMapper.selectList(paramVO);
        // 4.4.设置为空直接返回结果
        if (CollUtil.isEmpty(productionConsumptionList)) {
            return defaultNullData(productionConsumptionList, tableHeader);
        }
        List<Long> standingBookIds = productionConsumptionList
                .stream()
                .map(ProductionConsumptionSettingsDO::getStandingbookId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 4.4.台账id为空直接返回结果
        if (CollUtil.isEmpty(standingBookIds)) {
            return defaultNullData(productionConsumptionList, tableHeader);
        }
        // 4.如果没有则去数据库查询
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                dateType,
                range[0],
                range[1],
                standingBookIds);

        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));


        List<ProductionConsumptionStatisticsInfo> collect = productionConsumptionList
                .stream()
                .map(s -> {
                    ProductionConsumptionStatisticsInfo info = new ProductionConsumptionStatisticsInfo();
                    info.setName(s.getName());
                    info.setId(s.getId());

                    List<UsageCostData> usageCostList = standingBookUsageMap.get(s.getStandingbookId());

                    List<ProductionConsumptionStatisticInfoData> statisticInfoDataList = new ArrayList<>();

                    if (Objects.isNull(usageCostList)) {
                        // 如果为空自动填充/
                        tableHeader.forEach(date -> {
                            ProductionConsumptionStatisticInfoData infoData = new ProductionConsumptionStatisticInfoData();
                            infoData.setConsumption(BigDecimal.ZERO);
                            infoData.setDate(date);
                            statisticInfoDataList.add(infoData);
                        });
                        info.setStatisticsDateDataList(statisticInfoDataList);

                    } else {
                        // 如何不空 填充对应数据
                        Map<String, UsageCostData> usageCostMap = usageCostList
                                .stream()
                                .collect(Collectors.toMap(UsageCostData::getTime, Function.identity()));
                        tableHeader.forEach(date -> {

                            UsageCostData usageCostData = usageCostMap.get(date);
                            if (usageCostData == null) {
                                ProductionConsumptionStatisticInfoData infoData = new ProductionConsumptionStatisticInfoData();
                                infoData.setConsumption(BigDecimal.ZERO);
                                infoData.setDate(date);

                                statisticInfoDataList.add(infoData);
                            } else {
                                ProductionConsumptionStatisticInfoData infoData = new ProductionConsumptionStatisticInfoData();
                                infoData.setConsumption(usageCostData.getCurrentTotalUsage());
                                infoData.setDate(date);

                                statisticInfoDataList.add(infoData);
                            }
                        });
                    }

                    // 横向折标煤总和
                    BigDecimal sumConsumption = statisticInfoDataList
                            .stream()
                            .map(ProductionConsumptionStatisticInfoData::getConsumption)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    List<ProductionConsumptionStatisticInfoData> dataList = statisticInfoDataList.stream().peek(i -> {
                        i.setConsumption(dealBigDecimalScale(i.getConsumption(), scale));
                    }).collect(Collectors.toList());

                    info.setStatisticsDateDataList(dataList);
                    info.setSumConsumption(dealBigDecimalScale(sumConsumption, scale));

                    return info;
                })
                .collect(Collectors.toList());

        // 排序
        List<ProductionConsumptionStatisticsInfo> statisticsInfoList = collect
                .stream()
                .sorted(Comparator.comparing(ProductionConsumptionStatisticsInfo::getId))
                .collect(Collectors.toList());

        resultVO.setStatisticsInfoList(statisticsInfoList);

        // 获取数据更新时间
        LocalDateTime lastTime = usageCostService.getLastTime(
                dateType,
                range[0],
                range[1],
                standingBookIds);

        resultVO.setDataTime(lastTime);

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);

        return resultVO;
    }


    @Override
    public List<List<String>> getExcelHeader(ProductionConsumptionReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];
        // 表头数据
        List<List<String>> list = ListUtils.newArrayList();
        // 表单名称
        String sheetName = PRODUCTION_CONSUMPTION_STATISTICS;
        // 统计周期
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);
        // 统计系统
        List<String> system = paramVO.getNameList();
        String systemStr = CollUtil.isNotEmpty(system) ? String.join("、", system) : "全";

        list.add(Arrays.asList("表单名称", "统计系统", "统计周期", "统计项"));

        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> list.add(Arrays.asList(sheetName, systemStr, strTime, x)));

        // 周期合计
        list.add(Arrays.asList(sheetName, systemStr, strTime, "周期合计"));
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(ProductionConsumptionReportParamVO paramVO) {

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        StatisticsResultV2VO<ProductionConsumptionStatisticsInfo> resultVO = productionConsumptionTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();
        List<ProductionConsumptionStatisticsInfo> statisticsInfoList = resultVO.getStatisticsInfoList();
        // 底部合计map
        Map<String, BigDecimal> bottomSumMap = new HashMap<>();

        for (ProductionConsumptionStatisticsInfo p : statisticsInfoList) {
            List<Object> data = ListUtils.newArrayList();
            data.add(p.getName());
            List<ProductionConsumptionStatisticInfoData> statisticsDateDataList = p.getStatisticsDateDataList();

            Map<String, ProductionConsumptionStatisticInfoData> dateMap = statisticsDateDataList.stream()
                    .collect(Collectors.toMap(ProductionConsumptionStatisticInfoData::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                ProductionConsumptionStatisticInfoData infoData = dateMap.get(date);
                if (infoData == null) {
                    data.add("/");
                } else {
                    BigDecimal consumption = infoData.getConsumption();
                    data.add(getConvertData(consumption));
                    // 底部合计
                    bottomSumMap.put(date, addBigDecimal(bottomSumMap.get(date), consumption));
                }
            });

            BigDecimal sumConsumption = p.getSumConsumption();
            // 处理周期合计
            data.add(getConvertData(sumConsumption));
            // 处理底部周期合计
            bottomSumMap.put("sumConsumption", addBigDecimal(bottomSumMap.get("sumConsumption"), sumConsumption));
            result.add(data);
        }

        // 添加底部合计数据
        List<Object> bottom = ListUtils.newArrayList();
        // 每日合计、每月合计，每年合计
        bottom.add(DataTypeEnum.getBottomSumCell(DataTypeEnum.codeOf(paramVO.getDateType())));
        // 底部数据位
        tableHeader.forEach(date -> bottom.add(getConvertData(bottomSumMap.get(date))));
        // 底部周期合计
        bottom.add(getConvertData(bottomSumMap.get("sumConsumption")));
        result.add(bottom);
        return result;
    }


    private StatisticsResultV2VO<ProductionConsumptionStatisticsInfo> defaultNullData(List<ProductionConsumptionSettingsDO> list, List<String> tableHeader) {
        StatisticsResultV2VO<ProductionConsumptionStatisticsInfo> resultVO = new StatisticsResultV2VO<>();
        resultVO.setHeader(tableHeader);
        resultVO.setDataTime(LocalDateTime.now());
        List<ProductionConsumptionStatisticsInfo> infoList = new ArrayList<>();
        list.forEach(l -> {
            ProductionConsumptionStatisticsInfo info = new ProductionConsumptionStatisticsInfo();
            info.setName(l.getName());
            info.setStatisticsDateDataList(Collections.emptyList());
            infoList.add(info);
        });
        resultVO.setStatisticsInfoList(infoList);
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
        // 时间不能相差1年
        if (!LocalDateTimeUtils.isWithinDays(startTime, endTime, CommonConstants.YEAR)) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        return rangeOrigin;
    }

    /**
     * 校验时间类型
     *
     * @param dateType
     */
    private DataTypeEnum validateDateType(Integer dateType) {
        DataTypeEnum dataTypeEnum = DataTypeEnum.codeOf(dateType);
        // 时间类型不存在
        if (Objects.isNull(dataTypeEnum)) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        return dataTypeEnum;
    }

}
