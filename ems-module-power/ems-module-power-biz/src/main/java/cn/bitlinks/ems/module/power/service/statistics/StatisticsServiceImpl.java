package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsBarVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsDateData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
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


    @Override
    public JSONObject energyFlowAnalysis(StatisticsParamVO paramVO) {
        return null;
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
        List<String> tableHeader = getTableHeader(range, dateType);


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
    public Map<String, Object> standardCoalAnalysisChart(StatisticsParamVO paramVO) {

        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        result.put("", "");
        result.put("data", "list");
        return result;
    }


    /**
     * 投资指数-成本柱状图
     *
     * @param pageReqVO
     * @return
     */
    private StatisticsBarVO getTotalBarVO(StatisticsParamVO pageReqVO) {
        //获取数据

        //X轴数据
        List<String> XData = new ArrayList<>();
        XData.add("标识设备成本");
        XData.add("标识载体成本");
        XData.add("标识应用成本");
        //X轴数据
        List<BigDecimal> YData = new ArrayList<>();

        //标识设备成本
        BigDecimal identifierEquipmentCosts = BigDecimal.ZERO;
        //标识载体成本
        BigDecimal identifierCarrierCosts = BigDecimal.ZERO;
        //标识应用成本
        BigDecimal identifierApplicationCosts = BigDecimal.ZERO;


        YData.add(identifierEquipmentCosts);
        YData.add(identifierCarrierCosts);
        YData.add(identifierApplicationCosts);


        return StatisticsBarVO.builder()
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
        List<String> tableHeader = getTableHeader(range, dateType);


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
    public Map<String, Object> moneyAnalysisChart(StatisticsParamVO paramVO) {

        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        result.put("", "");
        result.put("data", "list");
        return result;
    }

    /**
     * 格式 时间list ['2024/5/5','2024/5/5','2024/5/5'];
     *
     * @param range 时间范围
     * @return ['2024/5/5','2024/5/5','2024/5/5']
     */
    private List<String> getTableHeader(LocalDate[] range, Integer dateType) {

        List<String> headerList = new ArrayList<>();

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
        StatisticsResultVO dateData = getDateData(vo, range, dateType,queryType);

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

                if (statisticsDateData.getConsumption()!=null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }else{
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

                if (statisticsDateData.getConsumption()!=null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }else{
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                startDate = startDate.plusYears(1);
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

                if (statisticsDateData.getConsumption()!=null) {
                    sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }else{
                    sumEnergyConsumption = null;
                    sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());
                }

                startDate = startDate.plusDays(1);
            }
        }

        vo.setStatisticsDateDataList(statisticsDateDataList);
        vo.setSumEnergyConsumption(sumEnergyConsumption);
        vo.setSumEnergyMoney(sumEnergyMoney);


        return vo;
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