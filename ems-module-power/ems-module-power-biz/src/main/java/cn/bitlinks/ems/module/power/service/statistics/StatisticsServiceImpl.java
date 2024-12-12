package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.energyconfiguration.vo.EnergyConfigurationSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsDateData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsResultVO;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
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
    public Map<String, Object> standardCoalAnalysis(StatisticsParamVO paramVO) {

        // 校验时间范围是否存在
        LocalDate[] range = paramVO.getRange();
        if (ArrayUtil.isEmpty(range)) {
            throw exception(DATE_RANGE_NOT_EXISTS);
        }

        // 返回结果map
        Map<String, Object> result = new HashMap<>(2);

        // 表头处理
        List<String> tableHeader = getTableHeader(range);
        List<Tree<Long>> labelTree = labelConfigService.getLabelTree(false, null, null);

        // 外购能源list
        EnergyConfigurationSaveReqVO queryVO = new EnergyConfigurationSaveReqVO();
        queryVO.setEnergyClassify(1);
        List<EnergyConfigurationDO> energyList = energyConfigurationService.getEnergyConfigurationList(queryVO);

        // TODO: 2024/12/11 多线程处理 labelTree for循环
        // 统计结果list
        List<StatisticsResultVO> list = new ArrayList<>();
        for (Tree<Long> tree : labelTree) {
            List<StatisticsResultVO> statisticsResultVOList = getStatisticsResultVO(tree, energyList, range);
            list.addAll(statisticsResultVOList);
        }

        result.put("header", tableHeader);
        result.put("data", list);
        return result;
    }

    /**
     * 格式 时间list ['2024/5/5','2024/5/5','2024/5/5'];
     *
     * @param range 时间范围
     * @return ['2024/5/5','2024/5/5','2024/5/5']
     */
    private List<String> getTableHeader(LocalDate[] range) {

        List<String> headerList = new ArrayList<>();

        LocalDate startDate = range[0];
        LocalDate endDate = range[1];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN);

        while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

            String formattedDate = formatter.format(startDate);
            headerList.add(formattedDate);

            startDate = startDate.plusDays(1);
        }

        return headerList;
    }


    private List<StatisticsResultVO> getStatisticsResultVO(Tree<Long> labelTree, List<EnergyConfigurationDO> energyList, LocalDate[] range) {

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
            List<StatisticsResultVO> statisticsResultVOList = getEnergyList(statisticsResultVO, energyList, range);
            list.addAll(statisticsResultVOList);
            return list;
        }

        // 还有孩子节点的数据
        for (Tree<Long> longTree : labelTreeList) {

            List<StatisticsResultVO> statisticsResultList = getStatisticsResultVO(longTree, energyList, range);
            list.addAll(statisticsResultList);
        }

        return list;
    }

    private List<StatisticsResultVO> getEnergyList(StatisticsResultVO statisticsResultVO, List<EnergyConfigurationDO> energyList, LocalDate[] range) {

        List<StatisticsResultVO> list = new ArrayList<>();
        BigDecimal sumLabelConsumption = BigDecimal.ZERO;
        BigDecimal sumLabelMoney = BigDecimal.ZERO;
        for (EnergyConfigurationDO energy : energyList) {
            StatisticsResultVO vo = BeanUtils.toBean(statisticsResultVO, StatisticsResultVO.class);
            vo.setEnergyName(energy.getEnergyName());
            vo.setEnergyId(energy.getId());

            // 获取对应标签下对应能源的每日用能数据和折价数据
            StatisticsResultVO dateData = getDateData(vo, range);
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


    /**
     * @param range 时间范围
     * @return
     */
    private StatisticsResultVO getDateData(StatisticsResultVO vo, LocalDate[] range) {

        List<StatisticsDateData> statisticsDateDataList = new ArrayList<>();

        //时间预处理
        LocalDate startDate = range[0];
        LocalDate endDate = range[1];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN);

        // 标签、能源id预处理
        Long energyId = vo.getEnergyId();
        Long labelId = vo.getLabelId();


        BigDecimal sumEnergyConsumption = BigDecimal.ZERO;
        BigDecimal sumEnergyMoney = BigDecimal.ZERO;


        while (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {

            // 时间处理
            String formattedDate = formatter.format(startDate);

            // 用量、折价处理  根据标签 能源 获取对应日期的统计数据。
            StatisticsDateData statisticsDateData = getData(labelId, energyId, startDate);

            statisticsDateData.setDate(formattedDate);
            statisticsDateDataList.add(statisticsDateData);

            sumEnergyConsumption = sumEnergyConsumption.add(statisticsDateData.getConsumption());
            sumEnergyMoney = sumEnergyMoney.add(statisticsDateData.getMoney());

            startDate = startDate.plusDays(1);
        }

        vo.setStatisticsDateDataList(statisticsDateDataList);
        vo.setSumEnergyConsumption(sumEnergyConsumption);
        vo.setSumEnergyMoney(sumEnergyMoney);


        return vo;
    }

    private StatisticsDateData getData(Long labelId, Long energyId, LocalDate date) {
        StatisticsDateData statisticsDateData = new StatisticsDateData();

        // TODO: 2024/12/12 调用starrocks数据库获取对应 标签、能源、日期下的数据。 （待完善）


        statisticsDateData.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));
        statisticsDateData.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10L)).setScale(2, RoundingMode.HALF_UP));

        return statisticsDateData;
    }

}