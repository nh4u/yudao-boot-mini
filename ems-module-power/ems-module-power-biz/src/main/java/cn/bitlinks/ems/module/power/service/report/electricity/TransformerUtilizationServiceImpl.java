package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.TransformerUtilizationSettingsDTO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.TransformerUtilizationSettingsOptionsVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.TransformerUtilizationSettingsVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.*;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.electricity.TransformerUtilizationSettingsDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.electricity.TransformerUtilizationSettingsMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.minuteagg.MinuteAggDataService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.standingbook.type.StandingbookTypeService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ReportCacheConstants.TRANSFORMER_UTILIZATION_TABLE;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

@Service
@Validated
@Slf4j
public class TransformerUtilizationServiceImpl implements TransformerUtilizationService {
    @Resource
    private TransformerUtilizationSettingsMapper transformerUtilizationSettingsMapper;
    @Resource
    private StandingbookTmplDaqAttrService standingbookTmplDaqAttrService;
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookTypeService standingbookTypeService;

    private final String namePattern = "%s(%s)";
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    @Resource
    private MinuteAggDataService minuteAggDataService;
    private final Integer scale = DEFAULT_SCALE;

    @Override
    @Transactional
    public void updSettings(List<TransformerUtilizationSettingsVO> settings) {
        // 校验
        if (CollUtil.isEmpty(settings)) {
            return;
        }
        // 提取id到Set
        Set<Long> transformerIdSet = new HashSet<>();
        boolean hasDuplicate = settings.stream()
                .map(TransformerUtilizationSettingsVO::getTransformerId)
                .anyMatch(item -> !transformerIdSet.add(item));
        if (hasDuplicate) {
            throw exception(DUPLICATE_TRANSFORMER_ID);
        }
        List<TransformerUtilizationSettingsDO> list = BeanUtils.toBean(settings, TransformerUtilizationSettingsDO.class);
        Map<Boolean, List<TransformerUtilizationSettingsDO>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getId() != null,
                        Collectors.toList()
                ));

        List<TransformerUtilizationSettingsDO> updateList = grouped.get(true);
        List<TransformerUtilizationSettingsDO> insertList = grouped.get(false);

        if (CollUtil.isNotEmpty(updateList)) {
            transformerUtilizationSettingsMapper.updateBatch(updateList);
        }

        if (CollUtil.isNotEmpty(insertList)) {
            transformerUtilizationSettingsMapper.insertBatch(insertList);
        }
    }

    @Override
    public List<TransformerUtilizationSettingsVO> getSettings() {

        List<TransformerUtilizationSettingsDO> transformerUtilizationSettingsDOList = transformerUtilizationSettingsMapper
                .selectList();
        if (CollUtil.isEmpty(transformerUtilizationSettingsDOList)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(transformerUtilizationSettingsDOList, TransformerUtilizationSettingsVO.class);
    }


    @Override
    public List<TransformerUtilizationSettingsOptionsVO> transformerOptions() {

        // 已按 sort 排序
        List<TransformerUtilizationSettingsDO> transformerUtilizationSettingsDOList =
                transformerUtilizationSettingsMapper.selectList();
        if (CollUtil.isEmpty(transformerUtilizationSettingsDOList)) {
            return Collections.emptyList();
        }
        // （key -> StandingbookDTO）
        List<StandingbookDTO> list = standingbookService.getStandingbookDTOList();
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        Map<Long, StandingbookDTO> standingbookDTOMap = list.stream().collect(Collectors.toMap(StandingbookDTO::getStandingbookId, Function.identity()));
        if (standingbookDTOMap == null || standingbookDTOMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<TransformerUtilizationSettingsOptionsVO> result = new ArrayList<>();

        for (TransformerUtilizationSettingsDO settingsDO : transformerUtilizationSettingsDOList) {
            Long targetKeyFromMap = standingbookDTOMap.keySet().iterator().next(); // 拿一个 Map 里的 key 测试
            Long keyFromDO = 1937074460814827521L;
            System.out.println("DO key: " + keyFromDO + " long值=" + keyFromDO.longValue() + " hash=" + keyFromDO.hashCode());
            System.out.println("Map key: " + targetKeyFromMap + " long值=" + targetKeyFromMap.longValue() + " hash=" + targetKeyFromMap.hashCode());
            System.out.println("equals? " + keyFromDO.equals(targetKeyFromMap));
            StandingbookDTO dto = standingbookDTOMap.get(settingsDO.getTransformerId());
            if (Objects.isNull(dto)) {
                continue;
            }
            TransformerUtilizationSettingsOptionsVO vo = new TransformerUtilizationSettingsOptionsVO();
            vo.setTransformerId(settingsDO.getTransformerId());
            vo.setTransformerName(dto.getName());
            vo.setTransformerLabel(String.format(namePattern, dto.getName(), dto.getCode()));
            result.add(vo);
        }

        return result;
    }


    /**
     * 获取变压器利用率设置+能源参数编码
     *
     * @return list 配置
     */
    public List<TransformerUtilizationSettingsDTO> getSettingsDTO() {

        try {
            List<TransformerUtilizationSettingsDO> transformerUtilizationSettingsDOList = transformerUtilizationSettingsMapper
                    .selectList(new LambdaQueryWrapper<TransformerUtilizationSettingsDO>()
                            .orderByAsc(TransformerUtilizationSettingsDO::getCreateTime));
            if (CollUtil.isEmpty(transformerUtilizationSettingsDOList)) {
                return Collections.emptyList();
            }
            List<TransformerUtilizationSettingsDTO> transformerUtilizationSettingsDTOS = BeanUtils.toBean(transformerUtilizationSettingsDOList, TransformerUtilizationSettingsDTO.class);
            // 2.依赖的所有台账id
            List<Long> standingbookIds = transformerUtilizationSettingsDTOS.stream()
                    .map(TransformerUtilizationSettingsDTO::getLoadCurrentId)
                    .collect(Collectors.toList());

            // 3.查询依赖关系、分类与参数编码
            Map<Long, List<StandingbookTmplDaqAttrDO>> energyDaqAttrsBySbIdsMap = standingbookTmplDaqAttrService.getEnergyDaqAttrsBySbIds(standingbookIds);

            List<StandingbookDTO> list = standingbookService.getStandingbookDTOList();
            if (CollUtil.isEmpty(list)) {
                return Collections.emptyList();
            }
            Map<Long, StandingbookDTO> standingbookDTOMap = list.stream().collect(Collectors.toMap(StandingbookDTO::getStandingbookId, Function.identity()));
            Map<Long, StandingbookTypeDO> typeMap = standingbookTypeService.getStandingbookTypeIdMap(null);

            for (TransformerUtilizationSettingsDTO settingsDTO : transformerUtilizationSettingsDTOS) {
                Long sbId = settingsDTO.getLoadCurrentId();

                List<StandingbookTmplDaqAttrDO> attrList = energyDaqAttrsBySbIdsMap.getOrDefault(sbId, Collections.emptyList());

                // 匹配参数中文名，获取对应编码
                Optional<StandingbookTmplDaqAttrDO> matched = attrList.stream()
                        .filter(attr -> "电流".equals(attr.getParameter()))
                        .findFirst();

                if (!matched.isPresent()) {
                    log.info("未找到该台账id【{}】匹配的【电流】能源参数编码，跳过该COP", sbId);
                    continue;
                }

                String paramCode = matched.get().getCode();
                settingsDTO.setLoadCurrentParamCode(paramCode);
                StandingbookDTO sbDO = standingbookDTOMap.get(settingsDTO.getTransformerId());
                settingsDTO.setTransformerName(sbDO.getName());

                StandingbookTypeDO standingbookTypeDO = typeMap.get(sbDO.getTypeId());
                StandingbookTypeDO superTypeDO = typeMap.get(standingbookTypeDO.getSuperId());
                if (Objects.isNull(superTypeDO)) {
                    settingsDTO.setType(standingbookTypeDO.getName());
                    continue;
                }
                settingsDTO.setType(superTypeDO.getName());
                settingsDTO.setChildType(standingbookTypeDO.getName());
            }
            return transformerUtilizationSettingsDTOS;
        } catch (Exception e) {
            log.error("获取变压器利用率设备发生异常异常：{}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public BaseReportResultVO<TransformerUtilizationInfo> getTable(TransformerUtilizationParamVO paramVO) {

        // 校验参数
        validCondition(paramVO);
        String cacheKey = TRANSFORMER_UTILIZATION_TABLE + SecureUtil.md5(paramVO.toString());
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(cacheKey);
        String cacheRes = StrUtils.decompressGzip(compressed);
        if (CharSequenceUtil.isNotEmpty(cacheRes)) {
            return JSON.parseObject(cacheRes, new TypeReference<BaseReportResultVO<TransformerUtilizationInfo>>() {
            });
        }
        // 表头日期
        List<String> tableHeader = LocalDateTimeUtils.getTimeRangeList(paramVO.getRange()[0], paramVO.getRange()[1], DataTypeEnum.codeOf(paramVO.getDateType()));

        // 获取变压器利用率设置
        List<TransformerUtilizationSettingsDTO> transformerUtilizationSettingsDTOList = getSettingsDTO();
        if (CollUtil.isEmpty(transformerUtilizationSettingsDTOList)) {
            return defaultNullData(tableHeader);
        }

        // 获取所有的负载电流和能源参数
        List<Long> standingbookIds = transformerUtilizationSettingsDTOList.stream()
                .map(TransformerUtilizationSettingsDTO::getLoadCurrentId)
                .collect(Collectors.toList());
        List<String> paramCodes = transformerUtilizationSettingsDTOList.stream()
                .map(TransformerUtilizationSettingsDTO::getLoadCurrentParamCode)
                .collect(Collectors.toList());
        // 查询统计周期内最大值
        List<MinuteAggregateDataDO> minuteAggDataList = minuteAggDataService.getMaxDataGpByDateType(
                standingbookIds,
                paramCodes,
                paramVO.getDateType(),
                paramVO.getRange()[0],
                paramVO.getRange()[1]);


        List<TransformerUtilizationInfo> transformerUtilizationInfos = queryByDefault(transformerUtilizationSettingsDTOList, minuteAggDataList, DataTypeEnum.codeOf(paramVO.getDateType()));

        BaseReportResultVO<TransformerUtilizationInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
        // 设置最终返回值
        resultVO.setReportDataList(transformerUtilizationInfos);
        LocalDateTime lastTime = minuteAggDataService.getLastTime(
                standingbookIds,
                paramCodes,
                paramVO.getRange()[0],
                paramVO.getRange()[1]
        );
        resultVO.setDataTime(lastTime);

        // 结果保存在缓存中
        String jsonStr = JSONUtil.toJsonStr(resultVO);
        byte[] bytes = StrUtils.compressGzip(jsonStr);
        byteArrayRedisTemplate.opsForValue().set(cacheKey, bytes, 1, TimeUnit.MINUTES);
        return resultVO;
    }

    /**
     * 按标签维度统计：以 standingbookId 和标签结构为基础构建同比对比数据
     */
    private List<TransformerUtilizationInfo> queryByDefault(List<TransformerUtilizationSettingsDTO> settingsDTOList,
                                                            List<MinuteAggregateDataDO> minuteAggDataList,
                                                            DataTypeEnum dataTypeEnum
    ) {
        Map<String, List<MinuteAggregateDataDO>> gpSbParamMap =
                minuteAggDataList.stream()
                        .collect(Collectors.groupingBy(
                                d -> d.getStandingbookId() + "_" + d.getParamCode(),
                                Collectors.toList()
                        ));
        List<TransformerUtilizationInfo> resultList = new ArrayList<>();
        for (TransformerUtilizationSettingsDTO settingsDTO : settingsDTOList) {
            TransformerUtilizationInfo info = new TransformerUtilizationInfo();
            info.setChildType(settingsDTO.getChildType());
            info.setType(settingsDTO.getType());
            info.setTransformerId(settingsDTO.getTransformerId());
            info.setTransformerName(settingsDTO.getTransformerName());
            if (CollUtil.isEmpty(gpSbParamMap)) {
                info.setTransformerUtilizationInfoData(Collections.emptyList());
                resultList.add(info);
                continue;
            }
            List<MinuteAggregateDataDO> minuteAggregateDataDOS = gpSbParamMap.get(settingsDTO.getLoadCurrentId() + "_" + settingsDTO.getLoadCurrentParamCode());
            if (CollUtil.isEmpty(minuteAggregateDataDOS)) {
                info.setTransformerUtilizationInfoData(Collections.emptyList());
                resultList.add(info);
                continue;
            }
            // 电压等级
            String level = settingsDTO.getVoltageLevel();
            // 额定容量
            BigDecimal ratedCapacity = settingsDTO.getRatedCapacity();
            if (level == null || ratedCapacity == null) {
                info.setTransformerUtilizationInfoData(Collections.emptyList());
                resultList.add(info);
                continue;
            }
            // 聚合数据 转换成 NaturalGasInfoData
            List<TransformerUtilizationInfoData> dataList = new ArrayList<>(minuteAggregateDataDOS.stream().collect(Collectors.groupingBy(
                    MinuteAggregateDataDO::getAggregateTime,
                    Collectors.collectingAndThen(
                            Collectors.toList(),
                            list -> {
                                Optional<BigDecimal> max = list.stream()
                                        .map(MinuteAggregateDataDO::getFullValue)
                                        .max(Comparator.naturalOrder());
                                String time = LocalDateTimeUtil.format(list.get(0).getAggregateTime(), "yyyy-MM-dd HH:00:00");
                                // 计算利用率
                                BigDecimal result = max.get().multiply(new BigDecimal(level)).multiply(new BigDecimal(NumberUtil.sqrt(3L)))
                                        .multiply(new BigDecimal(100000))
                                        .divide(ratedCapacity, 2, RoundingMode.HALF_UP);
                                return new TransformerUtilizationInfoData(time, max.get(), result);
                            }
                    )
            )).values());

            // 周期合计，
            Optional<BigDecimal> maxPeriodLoad = dataList.stream()
                    .map(TransformerUtilizationInfoData::getActualLoad)
                    .max(Comparator.naturalOrder());
            dataList = dataList.stream().peek(i -> {
                i.setActualLoad(dealBigDecimalScale(i.getActualLoad(), scale));
                i.setUtilization(dealBigDecimalScale(i.getUtilization(), scale));
            }).collect(Collectors.toList());

            info.setTransformerUtilizationInfoData(dataList);

            if (maxPeriodLoad.isPresent()) {
                info.setPeriodActualLoad(dealBigDecimalScale(maxPeriodLoad.get(), scale));
                // 计算利用率
                BigDecimal result = maxPeriodLoad.get().multiply(new BigDecimal(level)).multiply(new BigDecimal(NumberUtil.sqrt(3L)))
                        .multiply(new BigDecimal(100000))
                        .divide(ratedCapacity, 2, RoundingMode.HALF_UP);
                info.setPeriodUtilization(dealBigDecimalScale(result, scale));
            }

            resultList.add(info);
        }

        return resultList;
    }

    /**
     * 表格返回空处理
     *
     * @param tableHeader
     * @return
     */
    private BaseReportResultVO<TransformerUtilizationInfo> defaultNullData(List<String> tableHeader) {
        BaseReportResultVO<TransformerUtilizationInfo> resultVO = new BaseReportResultVO<>();
        resultVO.setHeader(tableHeader);
        resultVO.setReportDataList(Collections.emptyList());
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
}
