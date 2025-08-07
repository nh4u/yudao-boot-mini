package cn.bitlinks.ems.module.power.service.report.supplywatertmp;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.supplyanalysis.vo.SupplyAnalysisStructureInfo;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpReportParamVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultV2VO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StructureInfoData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.SupplyAnalysisPieResultVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.dal.dataobject.report.supplywatertmp.SupplyWaterTmpSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.supplywatertmp.SupplyWaterTmpSettingsMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils.getFormatTime;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ExportConstants.SUPPLY_ANALYSIS;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.*;

/**
 * @author liumingqiang
 */
@Slf4j
@Service
@Validated
public class SupplyWaterTmpSettingsServiceImpl implements SupplyWaterTmpSettingsService {

    @Resource
    private SupplyWaterTmpSettingsMapper supplyWaterTmpSettingsMapper;

    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;

    @Resource
    private UsageCostService usageCostService;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Override
    public void updateBatch(List<SupplyWaterTmpSettingsSaveReqVO> supplyAnalysisSettingsList) {
        // 校验
        if (CollUtil.isEmpty(supplyAnalysisSettingsList)) {
            throw exception(SUPPLY_ANALYSIS_SETTINGS_LIST_NOT_EXISTS);
        }

        // 统一保存
        List<SupplyWaterTmpSettingsDO> list = BeanUtils.toBean(supplyAnalysisSettingsList, SupplyWaterTmpSettingsDO.class);
        list.forEach(l -> {
            Long standingBookId = l.getStandingbookId();
            if (!Objects.isNull(standingBookId)) {
                String paramCode = standingbookTmplDaqAttrService.getParamCode(standingBookId, l.getEnergyParamName());
                l.setEnergyParamCode(paramCode);
            }
        });
        supplyWaterTmpSettingsMapper.updateBatch(list);
    }

    @Override
    public List<SupplyWaterTmpSettingsDO> getSupplyWaterTmpSettingsList(SupplyWaterTmpSettingsPageReqVO pageReqVO) {
        return supplyWaterTmpSettingsMapper.selectList((new LambdaQueryWrapperX<SupplyWaterTmpSettingsDO>()
                .eqIfPresent(SupplyWaterTmpSettingsDO::getSystem, pageReqVO.getSystem())
                .orderByAsc(SupplyWaterTmpSettingsDO::getId)));
    }

    @Override
    public List<String> getSystem() {
        return supplyWaterTmpSettingsMapper.getSystem();
    }

    @Override
    public StatisticsResultV2VO<SupplyAnalysisStructureInfo> supplyAnalysisTable(SupplyWaterTmpReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());

        // 2.校验时间类型
        Integer dateType = paramVO.getDateType();
        DataTypeEnum dataTypeEnum = validateDateType(dateType);

        StatisticsResultV2VO<SupplyAnalysisStructureInfo> resultVO = new StatisticsResultV2VO<>();
        resultVO.setDataTime(LocalDateTime.now());

        // 校验系统 没值就返空
        List<String> system1 = paramVO.getSystem();
        if (CollUtil.isEmpty(system1)) {
            return resultVO;
        }

        // 3.表头处理
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultVO.setHeader(tableHeader);

        // 4.获取所有standingBookids
        List<SupplyWaterTmpSettingsDO> supplyAnalysisSettingsList = supplyWaterTmpSettingsMapper.selectList(paramVO);
        List<Long> standingBookIds = supplyAnalysisSettingsList
                .stream()
                .map(SupplyWaterTmpSettingsDO::getStandingbookId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 4.4.台账id为空直接返回结果
        if (CollUtil.isEmpty(standingBookIds)) {
            return resultVO;
        }

        // 5.根据台账ID查询用量
        List<UsageCostData> usageCostDataList = usageCostService.getList(
                dateType,
                rangeOrigin[0],
                rangeOrigin[1],
                standingBookIds);

        Map<Long, List<UsageCostData>> standingBookUsageMap = usageCostDataList.stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));


        List<SupplyAnalysisStructureInfo> collect = supplyAnalysisSettingsList
                .stream()
                .map(s -> {
                    SupplyAnalysisStructureInfo info = new SupplyAnalysisStructureInfo();
                    info.setSystem(s.getSystem());
                    info.setId(s.getId());

                    List<UsageCostData> usageCostList = standingBookUsageMap.get(s.getStandingbookId());

                    List<StructureInfoData> structureInfoDataList = new ArrayList<>();

                    if (Objects.isNull(usageCostList)) {
                        // 如果为空自动填充/
                        tableHeader.forEach(date -> {
                            StructureInfoData structureInfoData = new StructureInfoData();
                            structureInfoData.setNum(BigDecimal.ZERO);
                            structureInfoData.setProportion(BigDecimal.ZERO);
                            structureInfoData.setDate(date);

                            structureInfoDataList.add(structureInfoData);
                        });
                        info.setStructureInfoDataList(structureInfoDataList);
                        info.setSumNum(BigDecimal.ZERO);
                        info.setSumProportion(BigDecimal.ZERO);

                    } else {
                        // 如何不空 填充对应数据
                        Map<String, UsageCostData> usageCostMap = usageCostList
                                .stream()
                                .collect(Collectors.toMap(UsageCostData::getTime, Function.identity()));
                        tableHeader.forEach(date -> {

                            UsageCostData usageCostData = usageCostMap.get(date);
                            if (usageCostData == null) {
                                StructureInfoData structureInfoData = new StructureInfoData();
                                structureInfoData.setNum(BigDecimal.ZERO);
                                structureInfoData.setProportion(BigDecimal.ZERO);
                                structureInfoData.setDate(date);

                                structureInfoDataList.add(structureInfoData);
                            } else {
                                StructureInfoData structureInfoData = new StructureInfoData();
                                structureInfoData.setNum(usageCostData.getCurrentTotalUsage());
                                structureInfoData.setProportion(null);
                                structureInfoData.setDate(date);

                                structureInfoDataList.add(structureInfoData);

                            }

                        });
                    }

                    // 横向折标煤总和
                    BigDecimal sumNum = structureInfoDataList
                            .stream()
                            .map(StructureInfoData::getNum)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    info.setStructureInfoDataList(structureInfoDataList);

                    info.setSumNum(sumNum);
                    info.setSumProportion(null);
                    return info;
                })
                .collect(Collectors.toList());


        Map<String, List<SupplyAnalysisStructureInfo>> supplyAnalysisStructureInfoMap = collect
                .stream()
                .collect(Collectors.groupingBy(SupplyAnalysisStructureInfo::getSystem));

        List<SupplyAnalysisStructureInfo> list = new ArrayList<>();

        supplyAnalysisStructureInfoMap.forEach((system, supplyAnalysisStructureInfoList) -> {
            List<SupplyAnalysisStructureInfo> structureResultList = getStructureResultList(supplyAnalysisStructureInfoList);
            list.addAll(structureResultList);
        });

        // 排序
        List<SupplyAnalysisStructureInfo> statisticsInfoList = list
                .stream()
                .sorted(Comparator.comparing(SupplyAnalysisStructureInfo::getId))
                .collect(Collectors.toList());

        resultVO.setDataTime(LocalDateTime.now());
        resultVO.setHeader(tableHeader);
        resultVO.setStatisticsInfoList(statisticsInfoList);

        return resultVO;
    }

    @Override
    public SupplyAnalysisPieResultVO supplyAnalysisChart(SupplyWaterTmpReportParamVO paramVO) {
        return null;
    }

    @Override
    public List<List<String>> getExcelHeader(SupplyWaterTmpReportParamVO paramVO) {

        // 1.校验时间范围
        LocalDateTime[] range = validateRange(paramVO.getRange());
        // 2.时间处理
        LocalDateTime startTime = range[0];
        LocalDateTime endTime = range[1];
        // 表头数据
        List<List<String>> list = ListUtils.newArrayList();
        // 表单名称
        String sheetName = SUPPLY_ANALYSIS;
        // 统计周期
        String strTime = getFormatTime(startTime) + "~" + getFormatTime(endTime);
        // 统计系统
        List<String> system = paramVO.getSystem();
        String systemStr = CollUtil.isNotEmpty(system) ? String.join("、", system) : "全";

        list.add(Arrays.asList("表单名称", "统计系统", "统计周期", "系统", "系统"));
        list.add(Arrays.asList(sheetName, systemStr, strTime, "分析项", "分析项"));

        // 月份数据处理
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());
        List<String> xdata = LocalDateTimeUtils.getTimeRangeList(startTime, endTime, dataTypeEnum);

        xdata.forEach(x -> {
            list.add(Arrays.asList(sheetName, systemStr, strTime, x, "用量"));
            list.add(Arrays.asList(sheetName, systemStr, strTime, x, "占比(%)"));
        });

        // 周期合计
        list.add(Arrays.asList(sheetName, systemStr, strTime, "周期合计", "用量"));
        list.add(Arrays.asList(sheetName, systemStr, strTime, "周期合计", "占比(%)"));
        return list;
    }

    @Override
    public List<List<Object>> getExcelData(SupplyWaterTmpReportParamVO paramVO) {

        // 结果list
        List<List<Object>> result = ListUtils.newArrayList();
        StatisticsResultV2VO<SupplyAnalysisStructureInfo> resultVO = supplyAnalysisTable(paramVO);
        List<String> tableHeader = resultVO.getHeader();

        List<SupplyAnalysisStructureInfo> statisticsInfoList = resultVO.getStatisticsInfoList();
        // 底部合计map
        Map<String, BigDecimal> sumStandardCoatMap = new HashMap<>();
        Map<String, BigDecimal> sumProportionMap = new HashMap<>();

        for (SupplyAnalysisStructureInfo s : statisticsInfoList) {

            List<Object> data = ListUtils.newArrayList();
            data.add(s.getSystem());
            data.add(s.getItem());


            // 处理数据
            List<StructureInfoData> standardCoalInfoDataList = s.getStructureInfoDataList();

            Map<String, StructureInfoData> dateMap = standardCoalInfoDataList.stream()
                    .collect(Collectors.toMap(StructureInfoData::getDate, Function.identity()));

            tableHeader.forEach(date -> {
                StructureInfoData structureInfoData = dateMap.get(date);
                if (structureInfoData == null) {
                    data.add("/");
                    data.add("/");
                } else {
                    BigDecimal usage = structureInfoData.getNum();
                    BigDecimal proportion = structureInfoData.getProportion();
                    data.add(getConvertData(usage));
                    data.add(getConvertData(proportion));

                    // 底部合计处理
                    sumStandardCoatMap.put(date, addBigDecimal(sumStandardCoatMap.get(date), usage));
                    sumProportionMap.put(date, addBigDecimal(sumProportionMap.get(date), proportion));
                }

            });

            BigDecimal sumUsage = s.getSumNum();
            BigDecimal sumProportion = s.getSumProportion();
            // 处理周期合计
            data.add(getConvertData(sumUsage));
            data.add(getConvertData(sumProportion));

            // 处理底部合计
            sumStandardCoatMap.put("sumNum", addBigDecimal(sumStandardCoatMap.get("sumNum"), sumUsage));
            sumProportionMap.put("sumNum", addBigDecimal(sumProportionMap.get("sumNum"), sumProportion));
            result.add(data);
        }

        return result;
    }

    /**
     * 处理占比问题
     *
     * @param list 对应list
     * @return
     */
    private List<SupplyAnalysisStructureInfo> getStructureResultList(List<SupplyAnalysisStructureInfo> list) {

        // 获取纵向总和map
        Map<String, BigDecimal> sumMap = getSumMap(list);
        // 获取合计
        for (SupplyAnalysisStructureInfo structureInfo : list) {

            List<StructureInfoData> statisticsStructureDataList = structureInfo.getStructureInfoDataList();
            statisticsStructureDataList.forEach(s -> {
                BigDecimal proportion = getProportion(s.getNum(), sumMap.get(s.getDate()));
                s.setProportion(dealBigDecimalScale(proportion, DEFAULT_SCALE));
            });

            BigDecimal proportion = getProportion(structureInfo.getSumNum(), sumMap.get("sumNum"));
            structureInfo.setSumProportion(dealBigDecimalScale(proportion, DEFAULT_SCALE));

            // 保留有效数字
            structureInfo.setSumNum(dealBigDecimalScale(structureInfo.getSumNum(), DEFAULT_SCALE));

            statisticsStructureDataList = statisticsStructureDataList.stream().peek(s -> {
                s.setProportion(dealBigDecimalScale(s.getProportion(), DEFAULT_SCALE));
                s.setNum(dealBigDecimalScale(s.getNum(), DEFAULT_SCALE));
            }).collect(Collectors.toList());

            structureInfo.setStructureInfoDataList(statisticsStructureDataList);
        }

        return list;
    }

    /**
     * 按时间为key 得到map  纵向综合map
     *
     * @param list 对应list
     * @return
     */
    private Map<String, BigDecimal> getSumMap(List<SupplyAnalysisStructureInfo> list) {

        Map<String, BigDecimal> sumMap = new HashMap<>();

        list.forEach(l -> {
            List<StructureInfoData> structureInfoDataList = l.getStructureInfoDataList();
            structureInfoDataList.forEach(s -> {
                sumMap.put(s.getDate(), addBigDecimal(sumMap.get(s.getDate()), s.getNum()));
            });
            sumMap.put("sumNum", addBigDecimal(sumMap.get("sumNum"), l.getSumNum()));
        });

        return sumMap;
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
