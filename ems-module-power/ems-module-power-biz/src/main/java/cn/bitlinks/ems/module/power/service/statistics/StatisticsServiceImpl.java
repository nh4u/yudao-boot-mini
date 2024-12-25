package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.DeviceAssociationConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration.DeviceAssociationConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.deviceassociationconfiguration.DeviceAssociationConfigurationService;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_RANGE_EXCEED_LIMIT;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.DATE_RANGE_NOT_EXISTS;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private DeviceAssociationConfigurationService deviceAssociationConfigurationService;

    @Override
    public Map<String, Object> energyFlowAnalysis(StatisticsParamVO paramVO) {


        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        Map<String, Object> result = new HashMap<>(2);

        List<Map<String, String>> data = new ArrayList<>();

        List<Map<String, Object>> links = new ArrayList<>();

        // 能源数据
        List<EnergyConfigurationDO> energyList = dealEnergyQueryDataForEnergyFlow(paramVO);
        energyList.forEach(e -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", e.getEnergyName());
            data.add(map);
        });

        // 标签数据
        ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> labelPair = dealLabelQueryDataForEnergyFlow(paramVO);

        List<LabelConfigDO> list = labelPair.getLeft();
        list.forEach(l -> {
            Map<String, String> map = new HashMap<>();
            map.put("name", l.getLabelName());
            data.add(map);
        });


        Map<Integer, List<EnergyConfigurationDO>> collect = energyList.stream().collect(Collectors.groupingBy(EnergyConfigurationDO::getEnergyClassify));
        List<EnergyConfigurationDO> energy1 = collect.get(1);
        List<EnergyConfigurationDO> energy2 = collect.get(2);

        energy1.forEach(e -> {

            energy2.forEach(e2 -> {
                Map<String, Object> map = new HashMap<>();
                map.put("source", e.getEnergyName());
                map.put("target", e2.getEnergyName());
                map.put("value", RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
                links.add(map);
            });
        });

        List<Tree<Long>> labelTree = labelPair.getRight();

        energy2.forEach(e -> {
            for (Tree<Long> tree : labelTree) {
                Map<String, Object> map = new HashMap<>();
                map.put("source", e.getEnergyName());
                map.put("target", tree.getName());
                map.put("value", RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
                links.add(map);
            }
        });


        for (Tree<Long> tree : labelTree) {
            List<Map<String, Object>> mapList = getMapList(energyList, tree, range);
            links.addAll(mapList);
        }

        result.put("data", data);
        result.put("links", links);
        return result;
    }

    private List<Map<String, Object>> getMapList(List<EnergyConfigurationDO> energyList, Tree<Long> labelTree, LocalDate[] range) {

        List<Map<String, Object>> links = new ArrayList<>();


        List<Tree<Long>> labelTreeList = labelTree.getChildren();
        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {
            Map<String, Object> map = new HashMap<>();

            List<CharSequence> parentsNames = labelTree.getParentsName(true);
            map.put("source", parentsNames.get(parentsNames.size() - 1));
            map.put("target", labelTree.getName());
            map.put("value", RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
            links.add(map);
            return links;
        }

        // TODO: 2024/12/18 能流数据处理  关系 以及 标签，处理

        // 能源关系处理
//        energyList.forEach(e -> {
//
//            if (e.getEnergyClassify() == 1){
//                // 外购能源
//                DeviceAssociationConfigurationPageReqVO deviceAssociationVo = new DeviceAssociationConfigurationPageReqVO();
//                deviceAssociationVo.setEnergyId(e.getId());
//                List<DeviceAssociationConfigurationDO> list = deviceAssociationConfigurationService.getDeviceAssociationConfigurationList(deviceAssociationVo);
//                list.forEach(l->{
//                    String preMeasurement = l.getPreMeasurement();
//                });
//            }
//        });

        for (Tree<Long> longTree : labelTreeList) {
            List<Map<String, Object>> mapList = getMapList(energyList, longTree, range);
            links.addAll(mapList);
        }

        return links;
    }


    @Override
    public Map<String, Object> standardCoalAnalysisTable(StatisticsParamVO paramVO) {

        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        Integer dateType = paramVO.getDateType();

        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = getTableHeader(rangeOrigin, dateType);


        Integer queryType = paramVO.getQueryType();

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);

            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            // TODO: 2024/12/11 多线程处理 labelTree for循环
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
    }

    @Override
    public Object standardCoalAnalysisChart(StatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }
        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        Integer queryType = paramVO.getQueryType();
        Integer dateType = paramVO.getDateType();

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

            return getStackVO(rangeOrigin, dateType, list, queryType);

        } else if (2 == queryType) {
            // 2、按标签查看
            //X轴数据
            List<String> XData = getTableHeader(rangeOrigin, dateType);
            //Y轴数据list
            List<StackDataVO> YData = new ArrayList<>();

            // 标签查询条件处理 只需要第一级别就可以
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);

                List<BigDecimal> collect = getYData(XData, statisticsResultVOList);
                YData.add(StackDataVO.builder()
                        .name(tree.getName().toString())
                        .data(collect).build());

            }
            return StatisticsStackVO.builder()
                    .XData(XData)
                    .YData(YData).build();

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            //X轴数据
            List<String> XData = getTableHeader(rangeOrigin, dateType);

            // 统计结果list
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }

            //Y轴数据
            List<BigDecimal> YData = getYData(XData, list);

            return StatisticsBarVO.builder()
                    .XData(XData)
                    .YData(YData).build();
        }

    }

    private List<BigDecimal> getYData(List<String> XData, List<StatisticsResultVO> statisticsResultVOList) {
        // 初始化一个Map来存储每个时间点的总金额
        Map<String, BigDecimal> totalMoneyByDate = new HashMap<>();
        for (String date : XData) {
            totalMoneyByDate.put(date, BigDecimal.ZERO);
        }
        // 填充Y轴数据
        for (StatisticsResultVO vo : statisticsResultVOList) {
            for (StatisticsDateData dateData : vo.getStatisticsDateDataList()) {
                String date = dateData.getDate();
                BigDecimal money = dateData.getMoney();
                totalMoneyByDate.merge(date, money, BigDecimal::add);
            }
        }
        // 生成YData列表
        return XData.stream().map(totalMoneyByDate::get).collect(Collectors.toList());

    }

    /**
     * 堆叠图
     *
     * @param rangeOrigin
     * @param dateType
     * @param statisticsResultVOList
     * @return
     */
    private StatisticsStackVO getStackVO(LocalDateTime[] rangeOrigin, Integer dateType, List<StatisticsResultVO> statisticsResultVOList, Integer queryType) {

        //X轴数据
        List<String> XData = getTableHeader(rangeOrigin, dateType);

        //Y轴数据list
        List<StackDataVO> YData = new ArrayList<>();

        for (StatisticsResultVO statisticsResultVO : statisticsResultVOList) {
            String name;
            if (1 == queryType) {
                name = statisticsResultVO.getEnergyName();
            } else if (2 == queryType) {
                name = statisticsResultVO.getLabel1();
            } else {
                name = "";
            }

            List<StatisticsDateData> statisticsDateDataList = statisticsResultVO.getStatisticsDateDataList();
            List<BigDecimal> collect = statisticsDateDataList.stream().map(StatisticsDateData::getMoney).collect(Collectors.toList());
            YData.add(StackDataVO.builder()
                    .name(name)
                    .data(collect).build());
        }

        return StatisticsStackVO.builder()
                .XData(XData)
                .YData(YData).build();
    }

    @Override
    public Map<String, Object> moneyAnalysisTable(StatisticsParamVO paramVO) {

        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        Integer dateType = paramVO.getDateType();

        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = getTableHeader(rangeOrigin, dateType);


        Integer queryType = paramVO.getQueryType();

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);

            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            // TODO: 2024/12/11 多线程处理 labelTree for循环
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
    }

    @Override
    public Object moneyAnalysisChart(StatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }
        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }

        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();

        Integer queryType = paramVO.getQueryType();
        Integer dateType = paramVO.getDateType();

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(new StatisticsResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

            return getStackVO(rangeOrigin, dateType, list, queryType);

        } else if (2 == queryType) {
            // 2、按标签查看
            //X轴数据
            List<String> XData = getTableHeader(rangeOrigin, dateType);
            //Y轴数据list
            List<StackDataVO> YData = new ArrayList<>();

            // 标签查询条件处理 只需要第一级别就可以
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);

                List<BigDecimal> collect = getYData(XData, statisticsResultVOList);
                YData.add(StackDataVO.builder()
                        .name(tree.getName().toString())
                        .data(collect).build());

            }
            return StatisticsStackVO.builder()
                    .XData(XData)
                    .YData(YData).build();

        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            //X轴数据
            List<String> XData = getTableHeader(rangeOrigin, dateType);

            // 统计结果list
            for (Tree<Long> tree : labelTree) {
                List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }

            //Y轴数据
            List<BigDecimal> YData = getYData(XData, list);

            return StatisticsBarVO.builder()
                    .XData(XData)
                    .YData(YData).build();
        }

    }

    private List<StatisticsResultVO> getStatisticsData(StatisticsParamVO paramVO) {
        // 校验时间范围是否存在
        LocalDateTime[] rangeOrigin = paramVO.getRange();

        if (ArrayUtil.isEmpty(rangeOrigin)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};

        long between = LocalDateTimeUtil.between(range[0].atStartOfDay(), range[1].atStartOfDay(), ChronoUnit.DAYS);
        if (CommonConstants.YEAR < between) {
            throw exception(DATE_RANGE_EXCEED_LIMIT);
        }
        List<StatisticsResultVO> list = new ArrayList<>();
        List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsResultVO vo = new StatisticsResultVO();
            vo.setEnergyId(energy.getId());
            StatisticsResultVO dateData = getDateData(vo, range, paramVO.getDateType(), paramVO.getQueryType());
            list.add(dateData);
        }
        return list;
    }

    /**
     * 格式 时间list ['2024/5/5','2024/5/5','2024/5/5'];
     *
     * @param rangeOrigin 时间范围
     * @return ['2024/5/5','2024/5/5','2024/5/5']
     */
    private List<String> getTableHeader(LocalDateTime[] rangeOrigin, Integer dateType) {

        List<String> headerList = new ArrayList<>();

        LocalDate[] range = new LocalDate[]{rangeOrigin[0].toLocalDate(), rangeOrigin[1].toLocalDate()};
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        if (1 == dateType) {
            // 月
            LocalDate tempStartDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
            LocalDate tempEndDate = LocalDate.of(endDate.getYear(), endDate.getMonth(), 1);

            while (tempStartDate.isBefore(tempEndDate) || tempStartDate.isEqual(tempEndDate)) {

                int year = tempStartDate.getYear();
                int month = tempStartDate.getMonthValue();
                String monthSuffix = (month < 10 ? "-0" : "-") + month;
                headerList.add(year + monthSuffix);

                tempStartDate = tempStartDate.plusMonths(1);
            }

        } else if (2 == dateType) {
            // 年
            while (startDate.getYear() <= endDate.getYear()) {

                headerList.add(String.valueOf(startDate.getYear()));

                startDate = startDate.plusYears(1);
            }
        } else if (3 == dateType) {
            // 时
            LocalDateTime startDateTime = rangeOrigin[0];
            LocalDateTime endDateTime = rangeOrigin[1];

            while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
                String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd-HH");
                headerList.add(formattedDate);
                startDateTime = startDateTime.plusHours(1);
            }

        } else {
            // 日
            while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

                String formattedDate = LocalDateTimeUtil.formatNormal(startDate);
                headerList.add(formattedDate);

                startDate = startDate.plusDays(1);
            }
        }

        return headerList;
    }

    /**
     * 标签查询条件处理 能流图
     *
     * @param paramVO
     * @return
     */
    private ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> dealLabelQueryDataForEnergyFlow(StatisticsParamVO paramVO) {
        ImmutablePair<List<LabelConfigDO>, List<Tree<Long>>> labelPair;
        List<Long> labelIds = paramVO.getLabelIds();
        if (CollectionUtil.isNotEmpty(labelIds)) {
            labelPair = labelConfigService.getLabelPairByParam(labelIds);
        } else {
            labelPair = labelConfigService.getLabelPair(false, null, null);
        }
        return labelPair;
    }

    /**
     * 标签查询条件处理
     *
     * @param paramVO
     * @return
     */
    private List<Tree<Long>> dealLabelQueryData(StatisticsParamVO paramVO) {
        List<Tree<Long>> labelTree;
        List<Long> labelIds = paramVO.getLabelIds();
        if (CollectionUtil.isNotEmpty(labelIds)) {
            labelTree = labelConfigService.getLabelTreeByParam(labelIds);
        } else {
            labelTree = labelConfigService.getLabelTree(false, null, null);
        }
        return labelTree;
    }

    /**
     * 能源查询条件处理
     *
     * @param paramVO
     * @return
     */
    private List<EnergyConfigurationDO> dealEnergyQueryDataForEnergyFlow(StatisticsParamVO paramVO) {
        // 能源查询条件处理
        EnergyConfigurationPageReqVO queryVO = new EnergyConfigurationPageReqVO();

        List<Long> energyIds = paramVO.getEnergyIds();
        if (CollectionUtil.isNotEmpty(energyIds)) {
            queryVO.setEnergyIds(energyIds);
        }
        // 能源list
        return energyConfigurationService.getEnergyConfigurationList(queryVO);
    }

    /**
     * 能源查询条件处理
     *
     * @param paramVO
     * @return
     */
    private List<EnergyConfigurationDO> dealEnergyQueryData(StatisticsParamVO paramVO) {
        // 能源查询条件处理
        EnergyConfigurationPageReqVO queryVO = new EnergyConfigurationPageReqVO();

        List<Long> energyIds = paramVO.getEnergyIds();
        if (CollectionUtil.isNotEmpty(energyIds)) {
            queryVO.setEnergyIds(energyIds);
        } else {
            // 默认 外购能源全部
            queryVO.setEnergyClassify(1);
        }
        // 能源list
        return energyConfigurationService.getEnergyConfigurationList(queryVO);
    }


    private List<StatisticsResultVO> getStatisticsResultVOHaveEnergy(Tree<Long> labelTree, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsResultVO statisticsResultVO = new StatisticsResultVO();
            statisticsResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsResultVO.setLabel1(parentsNameList.get(0));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsResultVO.setLabel1(parentsNameList.get(1));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsResultVO.setLabel1(parentsNameList.get(2));
                    statisticsResultVO.setLabel2(parentsNameList.get(1));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(statisticsResultVO, energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsResultVO> statisticsResultList = getStatisticsResultVOHaveEnergy(longTree, energyList, range, dateType, queryType);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsResultVO> getStatisticsResultVONotHaveEnergy(Tree<Long> labelTree, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsResultVO statisticsResultVO = new StatisticsResultVO();
            statisticsResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsResultVO.setLabel1(parentsNameList.get(0));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsResultVO.setLabel1(parentsNameList.get(1));
                    statisticsResultVO.setLabel2(parentsNameList.get(0));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsResultVO.setLabel1(parentsNameList.get(2));
                    statisticsResultVO.setLabel2(parentsNameList.get(1));
                    statisticsResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsResultVO> statisticsResultVOList = getLabelList(statisticsResultVO, range, dateType, queryType);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsResultVO> statisticsResultList = getStatisticsResultVONotHaveEnergy(longTree, range, dateType, queryType);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsResultVO> getEnergyList(StatisticsResultVO statisticsResultVO, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();
        BigDecimal sumLabelConsumption = BigDecimal.ZERO;
        BigDecimal sumLabelMoney = BigDecimal.ZERO;
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsResultVO vo = BeanUtils.toBean(statisticsResultVO, StatisticsResultVO.class);
            vo.setEnergyName(energy.getEnergyName());
            vo.setEnergyId(energy.getId());

            // 获取对应标签下对应能源的每日用能数据和折价数据
            StatisticsResultVO dateData = getDateData(vo, range, dateType, queryType);
            list.add(dateData);
        }

        // 获取合计
        for (StatisticsResultVO resultVO : list) {
            sumLabelConsumption = sumLabelConsumption.add(resultVO.getSumEnergyConsumption());
            sumLabelMoney = sumLabelMoney.add(resultVO.getSumEnergyMoney());
        }

        // 赋值合计
        for (StatisticsResultVO resultVO : list) {
            resultVO.setSumLabelConsumption(sumLabelConsumption);
            resultVO.setSumLabelMoney(sumLabelMoney);
        }
        return list;
    }

    private List<StatisticsResultVO> getLabelList(StatisticsResultVO statisticsResultVO, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsResultVO> list = new ArrayList<>();
        StatisticsResultVO vo = BeanUtils.toBean(statisticsResultVO, StatisticsResultVO.class);

        // 获取对应标签下对应能源的每日用能数据和折价数据
        StatisticsResultVO dateData = getDateData(vo, range, dateType, queryType);

        dateData.setSumLabelConsumption(dateData.getSumEnergyConsumption());
        dateData.setSumLabelMoney(dateData.getSumEnergyMoney());
        list.add(dateData);
        return list;
    }

    /**
     * @param range 时间范围
     * @return
     */
    private StatisticsResultVO getDateData(StatisticsResultVO vo, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsDateData> statisticsDateDataList = new ArrayList<>();

        //时间预处理
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        // 标签、能源id预处理
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();


        BigDecimal sumEnergyConsumption = BigDecimal.ZERO;
        BigDecimal sumEnergyMoney = BigDecimal.ZERO;

        if (1 == dateType) {
            // 月
            LocalDate tempStartDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
            LocalDate tempEndDate = LocalDate.of(endDate.getYear(), endDate.getMonth(), 1);

            while (tempStartDate.isBefore(tempEndDate) || tempStartDate.isEqual(tempEndDate)) {

                int year = tempStartDate.getYear();
                int month = tempStartDate.getMonthValue();

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsDateData statisticsDateData = getMonthData(labelId, energyId, range, year, month, queryType);

                String monthSuffix = (month < 10 ? "-0" : "-") + month;
                statisticsDateData.setDate(year + monthSuffix);
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                tempStartDate = tempStartDate.plusMonths(1);
            }

        } else if (2 == dateType) {
            // 年
            while (startDate.getYear() <= endDate.getYear()) {

                int year = startDate.getYear();
                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsDateData statisticsDateData = getYearData(labelId, energyId, range, year, queryType);
                statisticsDateData.setDate(String.valueOf(year));
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                startDate = startDate.plusYears(1);
            }

        } else if (3 == dateType) {
            // 时

            LocalDateTime startDateTime = range[0].atStartOfDay();
            LocalDateTime endDateTime = range[1].atStartOfDay();

            while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
                String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
                StatisticsDateData statisticsDateData = getHourData(labelId, energyId, startDateTime, queryType);
                statisticsDateData.setDate(formattedDate);
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }
                startDateTime = startDateTime.plusHours(1);
            }

        } else {
            // 日
            while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

                // 时间处理
                String formattedDate = LocalDateTimeUtil.formatNormal(startDate);

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsDateData statisticsDateData = getData(labelId, energyId, startDate, queryType);

                statisticsDateData.setDate(formattedDate);
                statisticsDateDataList.add(statisticsDateData);

                if (statisticsDateData.getConsumption() != null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                } else {
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                startDate = startDate.plusDays(1);
            }
        }

        vo.setStatisticsDateDataList(statisticsDateDataList);
        // 横向合计：所有日期的一个合计
        vo.setSumEnergyConsumption(sumEnergyConsumption);
        vo.setSumEnergyMoney(sumEnergyMoney);


        return vo;
    }

    private List<StatisticsResultVO> getHourlyStatisticsData(StatisticsParamVO paramVO) {
        List<StatisticsResultVO> list = new ArrayList<>();
        List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsResultVO vo = new StatisticsResultVO();
            vo.setEnergyId(energy.getId());
            StatisticsResultVO dateData = getHourlyDateData(vo, paramVO.getRange(), paramVO.getQueryType());
            list.add(dateData);
        }
        return list;
    }

    private List<String> getHourlyTableHeader(LocalDateTime[] range) {
        List<String> headerList = new ArrayList<>();
        LocalDateTime startDateTime = range[0];
        LocalDateTime endDateTime = range[1];

        while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
            String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd-HH");
            headerList.add(formattedDate);
            startDateTime = startDateTime.plusHours(1);
        }

        return headerList;
    }

    private StatisticsResultVO getHourlyDateData(StatisticsResultVO vo, LocalDateTime[] range, Integer queryType) {
        List<StatisticsDateData> statisticsDateDataList = new ArrayList<>();
        LocalDateTime startDateTime = range[0];
        LocalDateTime endDateTime = range[1];
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();
        while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
            String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd HH");
            StatisticsDateData statisticsDateData = getHourData(labelId, energyId, startDateTime, queryType);
            statisticsDateData.setDate(formattedDate);
            statisticsDateDataList.add(statisticsDateData);
            startDateTime = startDateTime.plusHours(1);
        }
        vo.setStatisticsDateDataList(statisticsDateDataList);
        // 计算总金额
        BigDecimal sumEnergyMoney = statisticsDateDataList.stream()
                .map(StatisticsDateData::getMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setSumEnergyMoney(sumEnergyMoney);
        return vo;
    }

    private StatisticsDateData getHourData(Long labelId, Long energyId, LocalDateTime dateTime, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();
        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            // 如果没有找到数据，你可以设置默认值或空值。
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(BigDecimal.ZERO);
        }
        return statisticsDateData;
    }

    private StatisticsDateData getData(Long labelId, Long energyId, LocalDate date, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）

        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        }
        return statisticsDateData;
    }


    private StatisticsDateData getMonthData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer month, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024 and month = 12

        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        }
        return statisticsDateData;
    }

    private StatisticsDateData getYearData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer queryType) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024

        if (queryType != 2) {
            statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        } else {
            statisticsDateData.setConsumption(null);
            statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        }
        return statisticsDateData;
    }
}