package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.utils.CommonUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.QUERY_TYPE_NOT_EXISTS;

/**
 * * 用能结构分析、价格结构分析  Service 实现类
 *
 * @author hero
 */
@Service
@Validated
public class StatisticsStructureServiceImpl implements StatisticsStructureService {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private StatisticsService statisticsService;


    @Override
    public Map<String, Object> standardCoalStructureAnalysisTable(StatisticsParamVO paramVO) {
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
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        Integer queryType = paramVO.getQueryType();
        if (queryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }
        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        // 统计结果list
        List<StatisticsStructureResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = CommonUtil.getTableHeader(rangeOrigin, dateType);

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsStructureResultVO> statisticsResultVOList = getEnergyList(new StatisticsStructureResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            Map<String, BigDecimal> sumMap = new HashMap<>();
            List<StatisticsStructureResultVO> tempList = new ArrayList<>();
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsStructureResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);
                tempList.addAll(statisticsResultVOList);
            }

            list = getStructureResultList(sumMap, tempList);


            // TODO: 2025/1/10   标签统计时： 如果只展示一级标签的时候，那表数据，只可能展示一级标签数据 大约3~6条数据，是否数据少，
            //  如果每一级都展示的话，那如何计算占比的问题，怎么划分占比，即怎么归类的问题。如何进行划分归类，目前原型暂时未体现。





        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);

            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            // TODO: 2024/12/11 多线程处理 labelTree for循环
            for (Tree<Long> tree : labelTree) {
                List<StatisticsStructureResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
    }

    @Override
    public Object standardCoalStructureAnalysisChart(StatisticsParamVO paramVO) {
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
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        // 保存原始查询类型
        Integer originalQueryType = paramVO.getQueryType();
        if (originalQueryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }
        paramVO.setQueryType(0);
        // 复用表方法的核心逻辑
        Map<String, Object> tableResult = standardCoalStructureAnalysisTable(paramVO);

        // 获取原始数据列表
        List<StatisticsStructureResultVO> dataList = (List<StatisticsStructureResultVO>) tableResult.get("data");

        // 构建饼图结果
        Map<String, Object> result = new HashMap<>();

        switch (originalQueryType) {
            case 0:
                result.put("energyPie", buildEnergyPie(dataList, paramVO));
                result.put("labelPie", buildLabelPie(dataList, paramVO));
                break;
            case 1:
                result.put("energyPies", buildEnergyDimensionPies(dataList, paramVO));
                break;
            case 2:
                result.put("labelPies", buildLabelDimensionPies(dataList, paramVO));
                break;
        }

        return result;
    }

    @Override
    public Map<String, Object> standardMoneyStructureAnalysisTable(StatisticsParamVO paramVO) {
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
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        Integer queryType = paramVO.getQueryType();
        if (queryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }
        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        // 统计结果list
        List<StatisticsStructureResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = CommonUtil.getTableHeader(rangeOrigin, dateType);

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsStructureResultVO> statisticsResultVOList = getEnergyList(new StatisticsStructureResultVO(), energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            Map<String, BigDecimal> sumMap = new HashMap<>();
            List<StatisticsStructureResultVO> tempList = new ArrayList<>();
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsStructureResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType);
                tempList.addAll(statisticsResultVOList);
            }

            list = getStructureResultList(sumMap, tempList);


            // TODO: 2025/1/10   标签统计时： 如果只展示一级标签的时候，那表数据，只可能展示一级标签数据 大约3~6条数据，是否数据少，
            //  如果每一级都展示的话，那如何计算占比的问题，怎么划分占比，即怎么归类的问题。如何进行划分归类，目前原型暂时未体现。





        } else {
            // 0、综合查看（默认）
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);

            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);

            // TODO: 2024/12/11 多线程处理 labelTree for循环
            for (Tree<Long> tree : labelTree) {
                List<StatisticsStructureResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
    }

    @Override
    public Object standardMoneyStructureAnalysisChart(StatisticsParamVO paramVO) {
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
        if (dateType == null) {
            throw exception(DATE_TYPE_NOT_EXISTS);
        }

        // 保存原始查询类型
        Integer originalQueryType = paramVO.getQueryType();
        if (originalQueryType == null) {
            throw exception(QUERY_TYPE_NOT_EXISTS);
        }
        paramVO.setQueryType(0);
        // 复用表方法的核心逻辑
        Map<String, Object> tableResult = standardCoalStructureAnalysisTable(paramVO);

        // 获取原始数据列表
        List<StatisticsStructureResultVO> dataList = (List<StatisticsStructureResultVO>) tableResult.get("data");

        // 构建饼图结果
        Map<String, Object> result = new HashMap<>();

        switch (originalQueryType) {
            case 0:
                result.put("energyPie", buildEnergyPie(dataList, paramVO));
                result.put("labelPie", buildLabelPie(dataList, paramVO));
                break;
            case 1:
                result.put("energyPies", buildEnergyDimensionPies(dataList, paramVO));
                break;
            case 2:
                result.put("labelPies", buildLabelDimensionPies(dataList, paramVO));
                break;
        }

        return result;
    }

    // 构建能源维度饼图（综合查看）
    private PieChartVO buildEnergyPie(List<StatisticsStructureResultVO> dataList, StatisticsParamVO paramVO) {
        // 过滤出选中的能源
        Set<Long> selectedEnergyIds = new HashSet<>(paramVO.getEnergyIds());
        Map<String, BigDecimal> energyMap = dataList.stream()
                .filter(vo -> selectedEnergyIds.contains(vo.getEnergyId()))
                .collect(Collectors.groupingBy(
                        vo -> vo.getEnergyId() + "|" + vo.getEnergyName(),
                        Collectors.reducing(BigDecimal.ZERO, StatisticsStructureResultVO::getSumNum, BigDecimal::add)
                ));

        return createPieChart("能源用能结构", energyMap);
    }

    // 构建标签维度饼图（综合查看）
    private PieChartVO buildLabelPie(List<StatisticsStructureResultVO> dataList, StatisticsParamVO paramVO) {
        // 过滤出选中的标签
        Set<Long> selectedLabelIds = new HashSet<>(paramVO.getLabelIds());
        Map<String, BigDecimal> labelMap = dataList.stream()
                .filter(vo -> selectedLabelIds.contains(vo.getLabelId()))
                .collect(Collectors.groupingBy(
                        vo -> getFullLabelPath(vo),
                        Collectors.reducing(BigDecimal.ZERO, StatisticsStructureResultVO::getSumNum, BigDecimal::add)
                ));

        return createPieChart("标签用能结构", labelMap);
    }

    // 构建能源维度饼图集合（按能源查看）
    private List<PieChartVO> buildEnergyDimensionPies(List<StatisticsStructureResultVO> dataList, StatisticsParamVO paramVO) {
        return paramVO.getEnergyIds().stream().map(energyId -> {
            // 按一级标签聚合数据
            Map<String, BigDecimal> labelMap = dataList.stream()
                    .filter(vo -> energyId.equals(vo.getEnergyId()))
                    .collect(Collectors.groupingBy(
                            StatisticsStructureResultVO::getLabel1, // 关键修改：使用一级标签分组
                            Collectors.reducing(BigDecimal.ZERO, StatisticsStructureResultVO::getSumNum, BigDecimal::add)
                    ));

            String energyName = dataList.stream()
                    .filter(vo -> energyId.equals(vo.getEnergyId()))
                    .findFirst()
                    .map(StatisticsStructureResultVO::getEnergyName)
                    .orElse("未知能源");

            return createPieChart(energyName, labelMap);
        }).collect(Collectors.toList());
    }

    // 构建标签维度饼图集合（按标签查看）
    private List<PieChartVO> buildLabelDimensionPies(List<StatisticsStructureResultVO> dataList, StatisticsParamVO paramVO) {
        // 获取所有选中的labelIds
        Set<Long> selectedLabelIds = new HashSet<>(paramVO.getLabelIds());

        // 过滤出选中标签的数据
        List<StatisticsStructureResultVO> filteredData = dataList.stream()
                .collect(Collectors.toList());

        // 按label1分组，每个分组生成一个饼图
        Map<String, List<StatisticsStructureResultVO>> groupedByLabel1 = filteredData.stream()
                .collect(Collectors.groupingBy(StatisticsStructureResultVO::getLabel1));

        // 对每个label1生成饼图
        return groupedByLabel1.entrySet().stream().map(entry -> {
            String label1 = entry.getKey();
            List<StatisticsStructureResultVO> labelData = entry.getValue();

            // 按能源分组，计算总用量
            Map<String, BigDecimal> energyMap = labelData.stream()
                    .collect(Collectors.groupingBy(
                            vo -> vo.getEnergyId() + "|" + vo.getEnergyName(),
                            Collectors.reducing(BigDecimal.ZERO, StatisticsStructureResultVO::getSumNum, BigDecimal::add)
                    ));

            return createPieChart(label1, energyMap);
        }).collect(Collectors.toList());
    }

    // 获取完整标签路径
    private String getFullLabelPath(StatisticsStructureResultVO vo) {
        return Stream.of(vo.getLabel1(), vo.getLabel2(), vo.getLabel3())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("/"));
    }

    // 安全创建饼图
    private PieChartVO createPieChart(String title, Map<String, BigDecimal> dataMap) {
        BigDecimal total = dataMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PieItemVO> items = dataMap.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    String name = parts.length > 1 ? parts[1] : entry.getKey();

                    return new PieItemVO(
                            name,
                            entry.getValue(),
                            calculateProportion(entry.getValue(), total)
                    );
                })
                .collect(Collectors.toList());

        return new PieChartVO(title, items,total);
    }

    // 保持与表格相同的占比计算
    private BigDecimal calculateProportion(BigDecimal value, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.divide(total, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP);
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


    private List<StatisticsStructureResultVO> getStatisticsResultVOHaveEnergy(Tree<Long> labelTree, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsStructureResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsStructureResultVO statisticsStructureResultVO = new StatisticsStructureResultVO();
            statisticsStructureResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsStructureResultVO.setLabel1(parentsNameList.get(0));
                    statisticsStructureResultVO.setLabel2(parentsNameList.get(0));
                    statisticsStructureResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsStructureResultVO.setLabel1(parentsNameList.get(1));
                    statisticsStructureResultVO.setLabel2(parentsNameList.get(0));
                    statisticsStructureResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsStructureResultVO.setLabel1(parentsNameList.get(2));
                    statisticsStructureResultVO.setLabel2(parentsNameList.get(1));
                    statisticsStructureResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsStructureResultVO> statisticsResultVOList = getEnergyList(statisticsStructureResultVO, energyList, range, dateType, queryType);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsStructureResultVO> statisticsResultList = getStatisticsResultVOHaveEnergy(longTree, energyList, range, dateType, queryType);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsStructureResultVO> getStatisticsResultVONotHaveEnergy(Tree<Long> labelTree, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsStructureResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsStructureResultVO statisticsStructureResultVO = new StatisticsStructureResultVO();
            statisticsStructureResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsStructureResultVO.setLabel1(parentsNameList.get(0));
                    statisticsStructureResultVO.setLabel2(parentsNameList.get(0));
                    statisticsStructureResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsStructureResultVO.setLabel1(parentsNameList.get(1));
                    statisticsStructureResultVO.setLabel2(parentsNameList.get(0));
                    statisticsStructureResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsStructureResultVO.setLabel1(parentsNameList.get(2));
                    statisticsStructureResultVO.setLabel2(parentsNameList.get(1));
                    statisticsStructureResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsStructureResultVO> statisticsResultVOList = getLabelList(statisticsStructureResultVO, range, dateType, queryType);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsStructureResultVO> statisticsResultList = getStatisticsResultVONotHaveEnergy(longTree, range, dateType, queryType);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsStructureResultVO> getEnergyList(StatisticsStructureResultVO statisticsStructureResultVO, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType) {


        Map<String, BigDecimal> sumMap = new HashMap<>();

        List<StatisticsStructureResultVO> list = new ArrayList<>();

        for (EnergyConfigurationDO energy : energyList) {
            StatisticsStructureResultVO vo = BeanUtils.toBean(statisticsStructureResultVO, StatisticsStructureResultVO.class);
            vo.setEnergyName(energy.getEnergyName());
            vo.setEnergyId(energy.getId());

            // 获取对应标签下对应能源的每日用能数据和折价数据
            StatisticsStructureResultVO dateData = getDateData(vo, range, dateType, queryType);
            List<StatisticsStructureData> statisticsStructureDataList = dateData.getStatisticsStructureDataList();
            statisticsStructureDataList.forEach(s -> {
                sumMap.put(s.getDate(), sumBigDecimal(sumMap.get(s.getDate()), s.getNum()));
            });
            sumMap.put("sumNum", sumBigDecimal(sumMap.get("sumNum"), dateData.getSumNum()));
            list.add(dateData);
        }

        return getStructureResultList(sumMap, list);
    }

    /**
     * 处理比例问题
     *
     * @param sumMap 每天总和map
     * @param list   对应list
     * @return
     */
    private List<StatisticsStructureResultVO> getStructureResultList(Map<String, BigDecimal> sumMap, List<StatisticsStructureResultVO> list) {

        // 获取合计
        for (StatisticsStructureResultVO resultVO : list) {

            List<StatisticsStructureData> statisticsStructureDataList = resultVO.getStatisticsStructureDataList();
            statisticsStructureDataList.forEach(s -> {
                s.setProportion(getProportion(s.getNum(), sumMap.get(s.getDate())));
            });

            resultVO.setSumProportion(getProportion(resultVO.getSumNum(), sumMap.get("sumNum")));
        }

        return list;
    }


    private List<StatisticsStructureResultVO> getLabelList(StatisticsStructureResultVO statisticsStructureResultVO, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsStructureResultVO> list = new ArrayList<>();
        StatisticsStructureResultVO vo = BeanUtils.toBean(statisticsStructureResultVO, StatisticsStructureResultVO.class);

        // 获取对应标签下对应能源的每日用能数据和折价数据
        StatisticsStructureResultVO dateData = getDateData(vo, range, dateType, queryType);

        dateData.setSumNum(dateData.getSumNum());
        dateData.setSumProportion(dateData.getSumProportion());
        list.add(dateData);
        return list;
    }

    /**
     * @param range 时间范围
     * @return
     */
    private StatisticsStructureResultVO getDateData(StatisticsStructureResultVO vo, LocalDate[] range, Integer dateType, Integer queryType) {

        List<StatisticsStructureData> statisticsStructureDataList = new ArrayList<>();

        //时间预处理
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        // 标签、能源id预处理
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();


        BigDecimal sumNum = BigDecimal.ZERO;


        if (1 == dateType) {
            // 月
            LocalDate tempStartDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
            LocalDate tempEndDate = LocalDate.of(endDate.getYear(), endDate.getMonth(), 1);

            while (tempStartDate.isBefore(tempEndDate) || tempStartDate.isEqual(tempEndDate)) {

                int year = tempStartDate.getYear();
                int month = tempStartDate.getMonthValue();

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsStructureData statisticsStructureData = getMonthData(labelId, energyId, range, year, month, queryType);

                String monthSuffix = (month < 10 ? "-0" : "-") + month;
                statisticsStructureData.setDate(year + monthSuffix);
                statisticsStructureDataList.add(statisticsStructureData);

                sumNum = sumNum.add(statisticsStructureData.getNum());

                tempStartDate = tempStartDate.plusMonths(1);
            }

        } else if (2 == dateType) {
            // 年
            while (startDate.getYear() <= endDate.getYear()) {

                int year = startDate.getYear();
                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsStructureData statisticsStructureData = getYearData(labelId, energyId, range, year, queryType);
                statisticsStructureData.setDate(String.valueOf(year));
                statisticsStructureDataList.add(statisticsStructureData);

                sumNum = sumNum.add(statisticsStructureData.getNum());

                startDate = startDate.plusYears(1);
            }

        } else if (3 == dateType) {
            // 时

            LocalDateTime startDateTime = range[0].atStartOfDay();
            LocalDateTime endDateTime = range[1].atStartOfDay();

            while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
                String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
                StatisticsStructureData statisticsStructureData = getHourData(labelId, energyId, startDateTime, queryType);
                statisticsStructureData.setDate(formattedDate);
                statisticsStructureDataList.add(statisticsStructureData);

                sumNum = sumNum.add(statisticsStructureData.getNum());

                startDateTime = startDateTime.plusHours(1);
            }

        } else {
            // 日
            while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

                // 时间处理
                String formattedDate = LocalDateTimeUtil.formatNormal(startDate);

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsStructureData statisticsStructureData = getData(labelId, energyId, startDate, queryType);

                statisticsStructureData.setDate(formattedDate);
                statisticsStructureDataList.add(statisticsStructureData);

                sumNum = sumNum.add(statisticsStructureData.getNum());

                startDate = startDate.plusDays(1);
            }
        }

        vo.setStatisticsStructureDataList(statisticsStructureDataList);
        // 横向合计：所有日期的一个合计
        vo.setSumNum(sumNum);

        return vo;
    }

    private List<StatisticsStructureResultVO> getHourlyStatisticsData(StatisticsParamVO paramVO) {
        List<StatisticsStructureResultVO> list = new ArrayList<>();
        List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsStructureResultVO vo = new StatisticsStructureResultVO();
            vo.setEnergyId(energy.getId());
            StatisticsStructureResultVO dateData = getHourlyDateData(vo, paramVO.getRange(), paramVO.getQueryType());
            list.add(dateData);
        }
        return list;
    }

    private List<String> getHourlyTableHeader(LocalDateTime[] range) {
        List<String> headerList = new ArrayList<>();
        LocalDateTime startDateTime = range[0];
        LocalDateTime endDateTime = range[1];

        while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
            String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
            headerList.add(formattedDate);
            startDateTime = startDateTime.plusHours(1);
        }

        return headerList;
    }

    private StatisticsStructureResultVO getHourlyDateData(StatisticsStructureResultVO vo, LocalDateTime[] range, Integer queryType) {
        List<StatisticsStructureData> statisticsStructureDataList = new ArrayList<>();
        LocalDateTime startDateTime = range[0];
        LocalDateTime endDateTime = range[1];
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();
        while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
            String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
            StatisticsStructureData statisticsStructureData = getHourData(labelId, energyId, startDateTime, queryType);
            statisticsStructureData.setDate(formattedDate);
            statisticsStructureDataList.add(statisticsStructureData);
            startDateTime = startDateTime.plusHours(1);
        }
        vo.setStatisticsStructureDataList(statisticsStructureDataList);
        // 计算总折标煤/折价
        BigDecimal sumNum = statisticsStructureDataList.stream()
                .map(StatisticsStructureData::getNum)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setSumNum(sumNum);

        // 计算总 占比
        BigDecimal sumProportion = statisticsStructureDataList.stream()
                .map(StatisticsStructureData::getProportion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setSumProportion(sumProportion);

        return vo;
    }

    private StatisticsStructureData getHourData(Long labelId, Long energyId, LocalDateTime dateTime, Integer queryType) {
        StatisticsStructureData statisticsStructureData = new StatisticsStructureData();

        statisticsStructureData.setNum(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));

        return statisticsStructureData;
    }

    private StatisticsStructureData getData(Long labelId, Long energyId, LocalDate date, Integer queryType) {
        StatisticsStructureData statisticsStructureData = new StatisticsStructureData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）
        statisticsStructureData.setNum(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));

        return statisticsStructureData;
    }


    private StatisticsStructureData getMonthData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer month, Integer queryType) {
        StatisticsStructureData statisticsStructureData = new StatisticsStructureData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024 and month = 12
        statisticsStructureData.setNum(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));

        return statisticsStructureData;
    }

    private StatisticsStructureData getYearData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer queryType) {
        StatisticsStructureData statisticsStructureData = new StatisticsStructureData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善） 获取对应折标煤 或者 折价
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024
        statisticsStructureData.setNum(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));

        return statisticsStructureData;
    }

    /**
     * 计算占比
     *
     * @param now   当前
     * @param total 总计
     * @return
     */
    private BigDecimal getProportion(BigDecimal now, BigDecimal total) {

        if (now == null || total == null) {
            return null;
        }
        BigDecimal proportion = BigDecimal.ZERO;
        if (total.compareTo(BigDecimal.ZERO) != 0) {
            proportion = now.divide(total, 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        return proportion;
    }

    private BigDecimal sumBigDecimal(BigDecimal first, BigDecimal second) {

        if (Objects.isNull(first)) {
            return second;
        } else {
            return first.add(second);
        }

    }

}