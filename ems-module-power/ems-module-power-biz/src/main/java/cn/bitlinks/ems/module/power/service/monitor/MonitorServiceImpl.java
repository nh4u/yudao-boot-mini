package cn.bitlinks.ems.module.power.service.monitor;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.pojo.StatsResult;
import cn.bitlinks.ems.framework.common.util.calc.CalculateUtil;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorDetailData;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorDetailRespVO;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.monitor.vo.MonitorRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.tmpl.vo.StandingbookTmplDaqAttrRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateData;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.tmpl.StandingbookTmplDaqAttrDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookLabelInfoMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.templ.StandingbookTmplDaqAttrMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.minuteagg.MinuteAggDataService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.bitlinks.ems.module.power.service.standingbook.tmpl.StandingbookTmplDaqAttrService;
import cn.bitlinks.ems.module.power.service.warninginfo.WarningInfoService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ApiConstants.*;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.DEFAULT_SCALE;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.utils.CommonUtil.dealBigDecimalScale;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class MonitorServiceImpl implements MonitorService {

    @Resource
    private StandingbookMapper standingbookMapper;
    @Resource
    private StandingbookLabelInfoMapper standingbookLabelInfoMapper;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;
    @Resource
    private StandingbookTmplDaqAttrMapper standingbookTmplDaqAttrMapper;

    @Resource
    private StandingbookTmplDaqAttrService standingbookTemplAttrService;
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;

    @Resource
    @Lazy
    private StandingbookService standingbookService;

    @Resource
    @Lazy
    private WarningInfoService warningInfoService;

    @Resource
    private MinuteAggDataService minuteAggDataService;

    @Override
    public MonitorRespVO getMinitorList(Map<String, String> pageReqVO) {
        //过滤空条件
        pageReqVO.entrySet().removeIf(entry -> StringUtils.isEmpty(entry.getValue()));

        // 能耗状态
        String standingbookStatus = pageReqVO.get(SB_STATUS);
        if (CharSequenceUtil.isBlank(standingbookStatus)) {
            standingbookStatus = "-1";
        }

        MonitorRespVO minitorRespVO = new MonitorRespVO();

        // 取出查询条件
        // 能源条件（可能为空）
        String energy = pageReqVO.get(ATTR_ENERGY);
        List<Long> energyTypeIds = new ArrayList<>();
        if (StringUtils.isNotEmpty(energy)) {
            //逗号分割的数据 转为Long类型列表
            List<Long> energyIds = Arrays.stream(energy.split(StringPool.COMMA))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            energyTypeIds = standingbookTmplDaqAttrMapper.selectSbTypeIdsByEnergyIds(energyIds);
            if (CollUtil.isEmpty(energyTypeIds)) {
                return minitorRespVO;
            }
        }

        // 分类多选条件(可能为空)
        List<String> sbTypeIdList = new ArrayList<>();
        String sbTypeIds = pageReqVO.get(ATTR_SB_TYPE_ID);
        if (StringUtils.isNotEmpty(sbTypeIds)) {
            sbTypeIdList = Arrays.stream(sbTypeIds.split(StringPool.HASH))
                    .map(s -> s.split(StringPool.COMMA))
                    .map(Arrays::stream)
                    .map(stream -> stream.reduce((first, second) -> second).orElse(""))
                    .collect(Collectors.toList());
        }

        // 根据分类和topType查询台账
        List<StandingbookTypeDO> sbTypeDOS = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                .inIfPresent(StandingbookTypeDO::getId, sbTypeIdList)
                .inIfPresent(StandingbookTypeDO::getId, energyTypeIds)
                .eqIfPresent(StandingbookTypeDO::getTopType, pageReqVO.get(SB_TYPE_ATTR_TOP_TYPE)));
        if (CollUtil.isEmpty(sbTypeDOS)) {
            return minitorRespVO;
        }
        List<Long> sbTypeIdLongList = sbTypeDOS.stream().map(StandingbookTypeDO::getId).collect(Collectors.toList());
        // 分类单选条件(可能为空)
        Long typeId = pageReqVO.get(ATTR_TYPE_ID) != null ? Long.valueOf(pageReqVO.get(ATTR_TYPE_ID)) : null;


        // 环节单选条件(可能为空)
        Integer stage = pageReqVO.get(ATTR_STAGE) != null ? Integer.valueOf(pageReqVO.get(ATTR_STAGE)) : null;
        // 创建时间条件(可能为空)
        String createTimes = pageReqVO.get(ATTR_CREATE_TIME);
        List<String> createTimeArr = new ArrayList<>();
        if (StringUtils.isNotEmpty(createTimes)) {
            createTimeArr = Arrays.asList(createTimes.split(StringPool.COMMA));
        }
        pageReqVO.remove(ATTR_ENERGY);
        pageReqVO.remove(SB_TYPE_ATTR_TOP_TYPE);
        pageReqVO.remove(ATTR_SB_TYPE_ID);
        pageReqVO.remove(ATTR_STAGE);
        pageReqVO.remove(ATTR_TYPE_ID);
        pageReqVO.remove(ATTR_CREATE_TIME);
        pageReqVO.remove(SB_STATUS);
        Map<String, List<String>> childrenConditions = new HashMap<>();
        Map<String, List<String>> labelInfoConditions = new HashMap<>();
        // 构造标签数组 和 属性表code条件数组
        pageReqVO.forEach((k, v) -> {
            if (k.startsWith(ATTR_LABEL_INFO_PREFIX)) {
                if (v.contains(StringPool.HASH)) {
                    labelInfoConditions.put(k, Arrays.asList(v.split(StringPool.HASH)));
                } else {
                    labelInfoConditions.put(k, Collections.singletonList(v));
                }
            } else {
                if (v.contains(StringPool.COMMA)) {
                    childrenConditions.put(k, Arrays.asList(v.split(StringPool.COMMA)));
                } else {
                    childrenConditions.put(k, Collections.singletonList(v));
                }
            }
        });
        // 根据台账属性查询台账id
        List<Long> sbIds = standingbookMapper.selectStandingbookIdByCondition(typeId, sbTypeIdLongList, stage, createTimeArr);
        if (CollUtil.isEmpty(sbIds)) {
            return minitorRespVO;
        }
        if (CollUtil.isNotEmpty(labelInfoConditions)) {
            // 根据标签属性查询台账id
            List<Long> labelSbIds = standingbookLabelInfoMapper.selectStandingbookIdByLabelCondition(labelInfoConditions, sbIds);
            sbIds.retainAll(labelSbIds);
            if (CollUtil.isEmpty(sbIds)) {
                return minitorRespVO;
            }
        }
        // 根据台账id、台账属性条件查询台账属性
        List<Long> attrSbIds = standingbookAttributeService.getStandingbookIdByCondition(childrenConditions, sbIds);

        sbIds.retainAll(attrSbIds);
        if (CollUtil.isEmpty(sbIds)) {
            return minitorRespVO;
        }
        // 组装每个台账节点结构，可与上合起来优化，暂不敢动
        List<StandingbookDO> result = standingbookService.getByIds(sbIds);
        Integer total = result.size();
        Integer warning = 0;
        if (CollUtil.isNotEmpty(result)) {
            result = dealWarningStatus(result, standingbookStatus);
        }

        //补充能源信息
        List<StandingbookRespVO> respVOS = BeanUtils.toBean(result, StandingbookRespVO.class);
        standingbookService.sbOtherField(respVOS);

        if (CollUtil.isNotEmpty(respVOS)) {

            // 异常的在最前面
            List<StandingbookRespVO> collect = respVOS
                    .stream()
                    .sorted(Comparator.comparing(StandingbookRespVO::getStandingbookStatus).reversed())
                    .collect(Collectors.toList());

            // 数量处理
            warning = (int) respVOS.stream().filter(r -> r.getStandingbookStatus() == 1).count();

            minitorRespVO.setWarning("0".equals(standingbookStatus) ? total - respVOS.size() : warning);
            minitorRespVO.setStandingbookRespVOList(collect);
        }
        minitorRespVO.setTotal(total);
        minitorRespVO.setNormal("0".equals(standingbookStatus) ? respVOS.size() : total - warning);

        return minitorRespVO;
    }

    @Override
    public MonitorDetailRespVO deviceDetail(MonitorParamReqVO paramVO) {

        // 返回结果
        MonitorDetailRespVO resultVO = new MonitorDetailRespVO();

        // 1.校验时间范围
        LocalDateTime[] rangeOrigin = validateRange(paramVO.getRange());
        // 2.2.校验时间类型
        DataTypeEnum dataTypeEnum = validateDateType(paramVO.getDateType());

        Integer flag = paramVO.getFlag();

        Integer dataFeature = paramVO.getDataFeature();

        List<MinuteAggregateData> list = null;

        if (flag == 0 || dataFeature == 2) {
            // 实时值 or 稳态值
            list = minuteAggDataService.getRealTimeList(
                    paramVO.getStandingbookId(),
                    paramVO.getParamCode(),
                    paramVO.getEnergyFlag(),
                    dataFeature,
                    rangeOrigin[0],
                    rangeOrigin[1]);

        } else if (flag == 1) {
            // 累计值
            list = minuteAggDataService.getList(
                    paramVO.getStandingbookId(),
                    paramVO.getParamCode(),
                    paramVO.getDateType(),
                    paramVO.getEnergyFlag(),
                    dataFeature,
                    rangeOrigin[0],
                    rangeOrigin[1]);
        } else {
            return resultVO;
        }

        if (CollUtil.isEmpty(list)) {
            return resultVO;
        }

        Map<String, BigDecimal> dataMap = list
                .stream()
                .collect(Collectors.toMap(MinuteAggregateData::getTime, MinuteAggregateData::getValue));

        // 处理 图-x轴
        List<String> x = LocalDateTimeUtils.getTimeRangeList(rangeOrigin[0], rangeOrigin[1], dataTypeEnum);
        resultVO.setChartX(x);

        // 处理 表
        List<MonitorDetailData> table = x.stream().map(time -> {
            MonitorDetailData minitorDetailData = new MonitorDetailData();
            minitorDetailData.setTime(time);
            minitorDetailData.setValue(dealBigDecimalScale(dataMap.get(time), DEFAULT_SCALE));
            return minitorDetailData;
        }).collect(Collectors.toList());
        resultVO.setTable(table);

        // 处理 图-数据
        List<BigDecimal> chartY = x.stream().map(dataMap::get).collect(Collectors.toList());
        resultVO.setChartData(chartY);

        StatsResult statsResult = CalculateUtil.calculateStats(
                list,
                MinuteAggregateData::getValue);
        // 平均值
        resultVO.setAvg(dealBigDecimalScale(statsResult.getAvg(), DEFAULT_SCALE));
        // 最大值
        resultVO.setMax(dealBigDecimalScale(statsResult.getMax(), DEFAULT_SCALE));
        // 最小值
        resultVO.setMin(dealBigDecimalScale(statsResult.getMin(), DEFAULT_SCALE));

        return resultVO;
    }

    @Override
    public List<StandingbookTmplDaqAttrRespVO> getDaqAttrs(Long standingbookId) {
        List<StandingbookTmplDaqAttrDO> standingbookTmplDaqAttrDOS = standingbookTemplAttrService.getDaqAttrsByStandingbookId(standingbookId);
        return BeanUtils.toBean(standingbookTmplDaqAttrDOS, StandingbookTmplDaqAttrRespVO.class);
    }

    @Override
    public List<MonitorDetailData> getDetailTable(MonitorParamReqVO paramVO) {
        MonitorDetailRespVO minitorDetailRespVO = deviceDetail(paramVO);
        return minitorDetailRespVO.getTable();
    }

    private List<StandingbookDO> dealWarningStatus(List<StandingbookDO> result, String standingbookStatus) {
        try {
            int status = Integer.parseInt(standingbookStatus);

            List<Long> sbIds = null;

            // 1.获取所有告警信息  warning的服务warning是根据加量器具编号处理的所以需要用编号做对应
            List<WarningInfoDO> warningList = warningInfoService.getWarningList();

            if (CollUtil.isNotEmpty(warningList)) {
                // 定义正则表达式：匹配括号及其中内容
                Pattern pattern = Pattern.compile("\\((.*?)\\)");
                List<String> warningCodes = warningList
                        .stream()
                        .map(WarningInfoDO::getDeviceRel)
                        .filter(Objects::nonNull)
                        .map(w -> {
                            Matcher matcher = pattern.matcher(w);
                            List<String> codes = new ArrayList<>();
                            while (matcher.find()) {
                                codes.add(matcher.group(1)); // 括号内的内容，如 Low-m5
                            }
                            return codes;
                        })
                        .flatMap(List::stream)
                        .distinct()
                        .collect(Collectors.toList());

                // 2.获取所有台账
                List<StandingbookDTO> list = standingbookAttributeMapper.getStandingbookDTO();
                // 2.1 告警的台账id
                sbIds = list
                        .stream()
                        .filter(l -> warningCodes.contains(l.getCode()))
                        .map(StandingbookDTO::getStandingbookId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            List<Long> finalSbIds = sbIds;
            switch (status) {
                case 0:
                    // 正常
                    if (CollUtil.isEmpty(finalSbIds)) {
                        return result;
                    } else {
                        return result
                                .stream()
                                .filter(r -> !finalSbIds.contains(r.getId()))
                                .collect(Collectors.toList());
                    }

                case 1:
                    // 异常
                    if (CollUtil.isEmpty(finalSbIds)) {
                        return Collections.emptyList();
                    } else {
                        return result
                                .stream()
                                .filter(r -> finalSbIds.contains(r.getId()))
                                .map(r -> r.setStandingbookStatus(1))
                                .collect(Collectors.toList());
                    }
                default:
                    if (CollUtil.isEmpty(finalSbIds)) {
                        return result;
                    } else {
                        result.forEach(r -> {
                            if (finalSbIds.contains(r.getId())) {
                                r.setStandingbookStatus(1);
                            }
                        });
                        return result;
                    }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
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
