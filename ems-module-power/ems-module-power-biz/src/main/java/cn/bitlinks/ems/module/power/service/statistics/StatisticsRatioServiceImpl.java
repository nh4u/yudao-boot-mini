package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsRatioData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsRatioResultVO;
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
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.QUERY_TYPE_NOT_EXISTS;

/**
 * * 比值（同比、环比、定基比） Service 实现类
 *
 * @author hero
 */
@Service
@Validated
public class StatisticsRatioServiceImpl implements StatisticsRatioService {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private StatisticsService statisticsService;


    /**
     * 折标煤用量环比分析
     *
     * @param paramVO
     * @return
     */
    @Override
    public Map<String, Object> standardCoalMomAnalysisTable(StatisticsParamVO paramVO) {

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
        List<StatisticsRatioResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = CommonUtil.getTableHeader(rangeOrigin, dateType);

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsRatioResultVO> statisticsResultVOList = getEnergyList(new StatisticsRatioResultVO(), energyList, range, dateType, queryType, 1);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsRatioResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType, 1);
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
                List<StatisticsRatioResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType, 1);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
    }

    /**
     * 折价环比分析
     *
     * @param paramVO
     * @return
     */
    @Override
    public Map<String, Object> moneyMomAnalysisTable(StatisticsParamVO paramVO) {
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
        List<StatisticsRatioResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = CommonUtil.getTableHeader(rangeOrigin, dateType);

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsRatioResultVO> statisticsResultVOList = getEnergyList(new StatisticsRatioResultVO(), energyList, range, dateType, queryType, 2);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsRatioResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType, 2);
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
                List<StatisticsRatioResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType, 2);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
    }

    /**
     * 利用率环比分析
     *
     * @param paramVO
     * @return
     */
    @Override
    public Map<String, Object> utilizationRatioMomAnalysisTable(StatisticsParamVO paramVO) {
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
        List<StatisticsRatioResultVO> list = new ArrayList<>();

        // 表头处理
        List<String> tableHeader = CommonUtil.getTableHeader(rangeOrigin, dateType);

        if (1 == queryType) {
            // 1、按能源查看
            // 能源查询条件处理
            List<EnergyConfigurationDO> energyList = dealEnergyQueryData(paramVO);
            // 能源结果list
            List<StatisticsRatioResultVO> statisticsResultVOList = getEnergyList(new StatisticsRatioResultVO(), energyList, range, dateType, queryType, 3);
            list.addAll(statisticsResultVOList);

        } else if (2 == queryType) {
            // 2、按标签查看
            // 标签查询条件处理
            List<Tree<Long>> labelTree = dealLabelQueryData(paramVO);
            for (Tree<Long> tree : labelTree) {
                List<StatisticsRatioResultVO> statisticsResultVOList = getStatisticsResultVONotHaveEnergy(tree, range, dateType, queryType, 3);
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
                List<StatisticsRatioResultVO> statisticsResultVOList = getStatisticsResultVOHaveEnergy(tree, energyList, range, dateType, queryType, 3);
                list.addAll(statisticsResultVOList);
            }
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
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

    private List<StatisticsRatioResultVO> getEnergyList(StatisticsRatioResultVO statisticsRatioResultVO, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType, Integer filed) {

        List<StatisticsRatioResultVO> list = new ArrayList<>();
        BigDecimal sumNow = BigDecimal.ZERO;
        BigDecimal sumPrevious = BigDecimal.ZERO;
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsRatioResultVO vo = BeanUtils.toBean(statisticsRatioResultVO, StatisticsRatioResultVO.class);
            vo.setEnergyName(energy.getEnergyName());
            vo.setEnergyId(energy.getId());

            // 获取对应标签下对应能源的每日用能数据和折价数据
            StatisticsRatioResultVO dateData = getDateData(vo, range, dateType, queryType, filed);
            list.add(dateData);
        }

        // 获取合计
        for (StatisticsRatioResultVO resultVO : list) {
            sumNow = sumNow.add(resultVO.getSumNow());
            sumPrevious = sumPrevious.add(resultVO.getSumPrevious());
        }

        // 赋值合计
        for (StatisticsRatioResultVO resultVO : list) {
            resultVO.setSumNow(sumNow);
            resultVO.setSumPrevious(sumPrevious);
            resultVO.setSumRatio(getMOMOrYOY(sumNow, sumPrevious));
        }
        return list;
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

    private List<StatisticsRatioResultVO> getLabelList(StatisticsRatioResultVO statisticsRatioResultVO, LocalDate[] range, Integer dateType, Integer queryType, Integer filed) {

        List<StatisticsRatioResultVO> list = new ArrayList<>();
        StatisticsRatioResultVO vo = BeanUtils.toBean(statisticsRatioResultVO, StatisticsRatioResultVO.class);

        // 获取对应标签下对应能源的每日用能数据和折价数据
        StatisticsRatioResultVO dateData = getDateData(vo, range, dateType, queryType, filed);
        list.add(dateData);
        return list;
    }

    private List<StatisticsRatioResultVO> getStatisticsResultVOHaveEnergy(Tree<Long> labelTree, List<EnergyConfigurationDO> energyList, LocalDate[] range, Integer dateType, Integer queryType, Integer filed) {

        List<StatisticsRatioResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsRatioResultVO statisticsRatioResultVO = new StatisticsRatioResultVO();
            statisticsRatioResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsRatioResultVO.setLabel1(parentsNameList.get(0));
                    statisticsRatioResultVO.setLabel2(parentsNameList.get(0));
                    statisticsRatioResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsRatioResultVO.setLabel1(parentsNameList.get(1));
                    statisticsRatioResultVO.setLabel2(parentsNameList.get(0));
                    statisticsRatioResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsRatioResultVO.setLabel1(parentsNameList.get(2));
                    statisticsRatioResultVO.setLabel2(parentsNameList.get(1));
                    statisticsRatioResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsRatioResultVO> statisticsResultVOList = getEnergyList(statisticsRatioResultVO, energyList, range, dateType, queryType, filed);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsRatioResultVO> statisticsResultList = getStatisticsResultVOHaveEnergy(longTree, energyList, range, dateType, queryType, filed);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsRatioResultVO> getStatisticsResultVONotHaveEnergy(Tree<Long> labelTree, LocalDate[] range, Integer dateType, Integer queryType, Integer filed) {

        List<StatisticsRatioResultVO> list = new ArrayList<>();

        List<Tree<Long>> labelTreeList = labelTree.getChildren();

        // 没有孩子的节点处理
        if (CollectionUtil.isEmpty(labelTreeList)) {

            StatisticsRatioResultVO statisticsRatioResultVO = new StatisticsRatioResultVO();
            statisticsRatioResultVO.setLabelId(labelTree.getId());
            // 获取父节点名称包含本节点名称
            List<String> parentsNameList = labelTree.getParentsName(true).stream()
                    .map(name -> (String) name)
                    .collect(Collectors.toList());

            switch (parentsNameList.size()) {
                case 1:
                    statisticsRatioResultVO.setLabel1(parentsNameList.get(0));
                    statisticsRatioResultVO.setLabel2(parentsNameList.get(0));
                    statisticsRatioResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 2:
                    statisticsRatioResultVO.setLabel1(parentsNameList.get(1));
                    statisticsRatioResultVO.setLabel2(parentsNameList.get(0));
                    statisticsRatioResultVO.setLabel3(parentsNameList.get(0));
                    break;
                case 3:
                    statisticsRatioResultVO.setLabel1(parentsNameList.get(2));
                    statisticsRatioResultVO.setLabel2(parentsNameList.get(1));
                    statisticsRatioResultVO.setLabel3(parentsNameList.get(0));
                    break;
                default:

            }

            // 包含 能源类型的结果list
            List<StatisticsRatioResultVO> statisticsResultVOList = getLabelList(statisticsRatioResultVO, range, dateType, queryType, filed);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsRatioResultVO> statisticsResultList = getStatisticsResultVONotHaveEnergy(longTree, range, dateType, queryType, filed);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    /**
     * @param range 时间范围
     * @return
     */
    private StatisticsRatioResultVO getDateData(StatisticsRatioResultVO vo, LocalDate[] range, Integer dateType, Integer queryType, Integer filed) {

        List<StatisticsRatioData> statisticsRatioDataList = new ArrayList<>();

        //时间预处理
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];

        // 标签、能源id预处理
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();

        BigDecimal sumNow = BigDecimal.ZERO;
        BigDecimal sumPrevious = BigDecimal.ZERO;


        if (1 == dateType) {
            // 月
            LocalDate tempStartDate = LocalDate.of(startDate.getYear(), startDate.getMonth(), 1);
            LocalDate tempEndDate = LocalDate.of(endDate.getYear(), endDate.getMonth(), 1);

            while (tempStartDate.isBefore(tempEndDate) || tempStartDate.isEqual(tempEndDate)) {

                int year = tempStartDate.getYear();
                int month = tempStartDate.getMonthValue();

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsRatioData statisticsRatioData = getMonthData(labelId, energyId, range, year, month, queryType, filed);

                String monthSuffix = (month < 10 ? "-0" : "-") + month;
                statisticsRatioData.setDate(year + monthSuffix);
                statisticsRatioDataList.add(statisticsRatioData);

                sumNow = sumNow.add(statisticsRatioData.getNow());
                sumPrevious = sumPrevious.add(statisticsRatioData.getPrevious());

                tempStartDate = tempStartDate.plusMonths(1);
            }

        } else if (2 == dateType) {
            // 年
            while (startDate.getYear() <= endDate.getYear()) {

                int year = startDate.getYear();
                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsRatioData statisticsRatioData = getYearData(labelId, energyId, range, year, queryType, filed);
                statisticsRatioData.setDate(String.valueOf(year));
                statisticsRatioDataList.add(statisticsRatioData);

                sumNow = sumNow.add(statisticsRatioData.getNow());
                sumPrevious = sumPrevious.add(statisticsRatioData.getPrevious());

                startDate = startDate.plusYears(1);
            }

        } else if (3 == dateType) {
            // 时

            LocalDateTime startDateTime = range[0].atStartOfDay();
            LocalDateTime endDateTime = range[1].atStartOfDay();

            while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
                String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
                StatisticsRatioData statisticsRatioData = getHourData(labelId, energyId, startDateTime, queryType, filed);
                statisticsRatioData.setDate(formattedDate);
                statisticsRatioDataList.add(statisticsRatioData);

                sumNow = sumNow.add(statisticsRatioData.getNow());
                sumPrevious = sumPrevious.add(statisticsRatioData.getPrevious());

                startDateTime = startDateTime.plusHours(1);
            }

        } else {
            // 日
            while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

                // 时间处理
                String formattedDate = LocalDateTimeUtil.formatNormal(startDate);

                // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
                StatisticsRatioData statisticsRatioData = getData(labelId, energyId, startDate, queryType, filed);

                statisticsRatioData.setDate(formattedDate);
                statisticsRatioDataList.add(statisticsRatioData);

                sumNow = sumNow.add(statisticsRatioData.getNow());
                sumPrevious = sumPrevious.add(statisticsRatioData.getPrevious());


                startDate = startDate.plusDays(1);
            }
        }

        vo.setStatisticsRatioDataList(statisticsRatioDataList);
        // 横向合计：所有日期的一个合计
        vo.setSumNow(sumNow);
        vo.setSumPrevious(sumPrevious);
        vo.setSumRatio(getMOMOrYOY(sumNow, sumPrevious));


        return vo;
    }

    private StatisticsRatioResultVO getHourlyDateData(StatisticsRatioResultVO vo, LocalDateTime[] range, Integer queryType, Integer filed) {
        List<StatisticsRatioData> statisticsRatioDataList = new ArrayList<>();
        LocalDateTime startDateTime = range[0];
        LocalDateTime endDateTime = range[1];
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();
        while (startDateTime.isBefore(endDateTime) || startDateTime.isEqual(endDateTime)) {
            String formattedDate = LocalDateTimeUtil.format(startDateTime, "yyyy-MM-dd:HH");
            StatisticsRatioData statisticsRatioData = getHourData(labelId, energyId, startDateTime, queryType, filed);
            statisticsRatioData.setDate(formattedDate);
            statisticsRatioDataList.add(statisticsRatioData);
            startDateTime = startDateTime.plusHours(1);
        }
        vo.setStatisticsRatioDataList(statisticsRatioDataList);
        // 计算总当期
        BigDecimal sumNow = statisticsRatioDataList.stream()
                .map(StatisticsRatioData::getNow)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setSumNow(sumNow);
        // 计算总同期/上期
        BigDecimal sumPrevious = statisticsRatioDataList.stream()
                .map(StatisticsRatioData::getPrevious)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setSumPrevious(sumPrevious);
        // 计算总同比/环比/定基比
        vo.setSumRatio(getMOMOrYOY(sumNow, sumPrevious));
        return vo;
    }

    private StatisticsRatioData getHourData(Long labelId, Long energyId, LocalDateTime dateTime, Integer queryType, Integer filed) {

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）折标煤、折价、利用率
        BigDecimal now = BigDecimal.ZERO;
        BigDecimal previous = BigDecimal.ZERO;
        switch (filed) {
            case 1:
                // 折标煤
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 2:
                // 折价
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 3:
                // 利用率
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            default:

        }
        return StatisticsRatioData.builder()
                .now(now)
                .previous(previous)
                .ratio(getMOMOrYOY(now, previous))
                .build();
    }

    private StatisticsRatioData getData(Long labelId, Long energyId, LocalDate date, Integer queryType, Integer filed) {

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）折标煤、折价、利用率
        BigDecimal now = BigDecimal.ZERO;
        BigDecimal previous = BigDecimal.ZERO;
        switch (filed) {
            case 1:
                // 折标煤
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 2:
                // 折价
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 3:
                // 利用率
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            default:

        }
        return StatisticsRatioData.builder()
                .now(now)
                .previous(previous)
                .ratio(getMOMOrYOY(now, previous))
                .build();
    }


    private StatisticsRatioData getMonthData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer month, Integer queryType, Integer filed) {

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）折标煤、折价、利用率
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024 and month = 12

        BigDecimal now = BigDecimal.ZERO;
        BigDecimal previous = BigDecimal.ZERO;
        switch (filed) {
            case 1:
                // 折标煤
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 2:
                // 折价
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 3:
                // 利用率
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            default:

        }
        return StatisticsRatioData.builder()
                .now(now)
                .previous(previous)
                .ratio(getMOMOrYOY(now, previous))
                .build();
    }

    private StatisticsRatioData getYearData(Long labelId, Long energyId, LocalDate[] range, Integer year, Integer queryType, Integer filed) {

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。  可能没标签或者没能源类型（待完善）折标煤、折价、利用率
        //  在范围内拿年月的数据，where time between 2024-05-5 and 2024-08-05 and year = 2024

        BigDecimal now = BigDecimal.ZERO;
        BigDecimal previous = BigDecimal.ZERO;
        switch (filed) {
            case 1:
                // 折标煤
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 2:
                // 折价
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            case 3:
                // 利用率
                now = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                previous = RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP);
                break;
            default:

        }

        return StatisticsRatioData.builder()
                .now(now)
                .previous(previous)
                .ratio(getMOMOrYOY(now, previous))
                .build();
    }


    /**
     * 计算环比 环比计算公式：环比增长率=(本期数-上期数)/上期数×100%
     * 同比计算公式：同比增长率=(本期数-上年同期数)/上年同期数×100%
     *
     * @param now      本期
     * @param previous 上一期
     * @return
     */
    private BigDecimal getMOMOrYOY(BigDecimal now, BigDecimal previous) {

        if (now == null || previous == null) {
            return null;
        }
        BigDecimal MOM = BigDecimal.ZERO;
        if (previous.compareTo(BigDecimal.ZERO) != 0) {
            MOM = now.subtract(previous)
                    .divide(previous, 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(2, RoundingMode.HALF_UP);

        }
        return MOM;
    }
}