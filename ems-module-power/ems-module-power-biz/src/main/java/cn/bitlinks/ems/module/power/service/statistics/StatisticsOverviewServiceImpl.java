package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计总览 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
public class StatisticsOverviewServiceImpl implements StatisticsOverviewService {

    @Resource
    private LabelConfigService labelConfigService;

    @Resource
    private EnergyConfigurationService energyConfigurationService;

    @Resource
    private StandingbookService standingbookService;

    @Resource
    private StatisticsService statisticsService;

    @Override
    public StatisticsOverviewResultVO overview(StatisticsParamVO paramVO) {

        StatisticsOverviewResultVO statisticsOverviewResultVO = new StatisticsOverviewResultVO();

        // 计量器具
        Long measurementInstrumentNum = standingbookService.count(CommonConstants.MEASUREMENT_INSTRUMENT_ID);
        statisticsOverviewResultVO.setMeasurementInstrumentNum(measurementInstrumentNum);
        // 重点设备
        Long keyEquipmentNum = standingbookService.count(CommonConstants.KEY_EQUIPMENT_ID);
        statisticsOverviewResultVO.setKeyEquipmentNum(keyEquipmentNum);
        // 其他设备
        Long otherEquipmentNum = standingbookService.count(CommonConstants.OTHER_EQUIPMENT_ID);
        statisticsOverviewResultVO.setOtherEquipmentNum(otherEquipmentNum);

        statisticsOverviewResultVO.setOutputValueEnergyConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(100000000L)).setScale(2, RoundingMode.HALF_UP));
        statisticsOverviewResultVO.setProductEnergyConsumption8(RandomUtil.randomBigDecimal(BigDecimal.valueOf(100000000L)).setScale(2, RoundingMode.HALF_UP));
        statisticsOverviewResultVO.setProductEnergyConsumption12(RandomUtil.randomBigDecimal(BigDecimal.valueOf(100000000L)).setScale(2, RoundingMode.HALF_UP));
        statisticsOverviewResultVO.setOutsourceEnergyUtilizationRate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(100L)).setScale(2, RoundingMode.HALF_UP));
        statisticsOverviewResultVO.setParkEnergyUtilizationRate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(100L)).setScale(2, RoundingMode.HALF_UP));
        statisticsOverviewResultVO.setEnergyConversionRate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(100L)).setScale(2, RoundingMode.HALF_UP));


        List<StatisticsOverviewEnergyData> list = new ArrayList<>();
        Integer energyClassify = paramVO.getEnergyClassify();

        List<EnergyConfigurationDO> energyList = energyConfigurationService.selectByCondition(null, energyClassify.toString(), null);

        List<Long> energyIdList = new ArrayList<>();

        energyList.forEach(e -> {
            StatisticsOverviewEnergyData data = new StatisticsOverviewEnergyData();

            data.setName(e.getEnergyName());

            data.setConsumption(RandomUtil.randomBigDecimal(BigDecimal.valueOf(100000000L)).setScale(2, RoundingMode.HALF_UP));
            data.setStandardCoal(RandomUtil.randomBigDecimal(BigDecimal.valueOf(10000000L)).setScale(2, RoundingMode.HALF_UP));
            data.setMoney(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000000L)).setScale(2, RoundingMode.HALF_UP));
            list.add(data);

            energyIdList.add(e.getId());
        });

        // 综合能耗
        BigDecimal standardCoal = BigDecimal.ZERO;
        BigDecimal money = BigDecimal.ZERO;
        for (StatisticsOverviewEnergyData l : list) {
            standardCoal = standardCoal.add(l.getStandardCoal());
            money = money.add(l.getMoney());
        }
        StatisticsOverviewEnergyData data = new StatisticsOverviewEnergyData();
        data.setName("综合能耗");
        data.setStandardCoal(standardCoal);
        data.setMoney(money);
        list.add(0, data);

        statisticsOverviewResultVO.setStatisticsOverviewEnergyDataList(list);

        // 1.折标煤用量统计
        StatisticsOverviewData standardCoalStatistics = new StatisticsOverviewData();

        // 1.1 今日/本周/本季/本年
        StatisticsOverviewStatisticsData standardCoalNow = new StatisticsOverviewStatisticsData();
        standardCoalNow.setAccumulate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(8000L), BigDecimal.valueOf(10000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalNow.setMax(RandomUtil.randomBigDecimal(BigDecimal.valueOf(4000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalNow.setMin(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalNow.setAverage(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L), BigDecimal.valueOf(3000L)).setScale(2, RoundingMode.HALF_UP));

        // 1.2 昨日/上周/上季/去年
        StatisticsOverviewStatisticsData standardCoalPrevious = new StatisticsOverviewStatisticsData();
        standardCoalPrevious.setAccumulate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(8000L), BigDecimal.valueOf(10000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalPrevious.setMax(RandomUtil.randomBigDecimal(BigDecimal.valueOf(4000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalPrevious.setMin(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalPrevious.setAverage(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L), BigDecimal.valueOf(3000L)).setScale(2, RoundingMode.HALF_UP));


        // 1.3 昨日/上周/上季/去年（去年同期）
        StatisticsOverviewStatisticsData standardCoalPreviousLastYear = new StatisticsOverviewStatisticsData();
        standardCoalPreviousLastYear.setAccumulate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(8000L), BigDecimal.valueOf(10000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalPreviousLastYear.setMax(RandomUtil.randomBigDecimal(BigDecimal.valueOf(4000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalPreviousLastYear.setMin(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
        standardCoalPreviousLastYear.setAverage(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L), BigDecimal.valueOf(3000L)).setScale(2, RoundingMode.HALF_UP));


        // 1.4 同比  和去年同期比
        StatisticsOverviewStatisticsData standardCoalYOY = new StatisticsOverviewStatisticsData();
        standardCoalYOY.setAccumulate(getMOMOrYOY(standardCoalNow.getAccumulate(), standardCoalPreviousLastYear.getAccumulate()));
        standardCoalYOY.setMax(getMOMOrYOY(standardCoalNow.getMax(), standardCoalPreviousLastYear.getMax()));
        standardCoalYOY.setMin(getMOMOrYOY(standardCoalNow.getMin(), standardCoalPreviousLastYear.getMin()));
        standardCoalYOY.setAverage(getMOMOrYOY(standardCoalNow.getAverage(), standardCoalPreviousLastYear.getAverage()));

        // 1.5 环比 和上一个期限比
        StatisticsOverviewStatisticsData standardCoalMOM = new StatisticsOverviewStatisticsData();
        standardCoalMOM.setAccumulate(getMOMOrYOY(standardCoalNow.getAccumulate(), standardCoalPrevious.getAccumulate()));
        standardCoalMOM.setMax(getMOMOrYOY(standardCoalNow.getMax(), standardCoalPrevious.getMax()));
        standardCoalMOM.setMin(getMOMOrYOY(standardCoalNow.getMin(), standardCoalPrevious.getMin()));
        standardCoalMOM.setAverage(getMOMOrYOY(standardCoalNow.getAverage(), standardCoalPrevious.getAverage()));

        // 1.6 图处理
        paramVO.setEnergyIds(energyIdList);
        StatisticsBarVO standardCoalBar = statisticsService.getOverallViewBar(paramVO);

        // 1.7 填充数据
        standardCoalStatistics.setNow(standardCoalNow);
        standardCoalStatistics.setPrevious(standardCoalPrevious);
        standardCoalStatistics.setYOY(standardCoalYOY);
        standardCoalStatistics.setMOM(standardCoalMOM);
        standardCoalStatistics.setBar(standardCoalBar);

        statisticsOverviewResultVO.setStandardCoalStatistics(standardCoalStatistics);

        // : 2024/12/31  改成 { list  bar}
        List<JSONObject> standardCoalJsonList = entity2Json(standardCoalNow,
                standardCoalPrevious,
                standardCoalYOY,
                standardCoalMOM);

        JSONObject standardCoalStatisticsJson = new JSONObject();

        standardCoalStatisticsJson.put("list", standardCoalJsonList);
        standardCoalStatisticsJson.put("bar", standardCoalBar);

        statisticsOverviewResultVO.setStandardCoalStatisticsJson(standardCoalStatisticsJson);

        // 2.折价统计
        StatisticsOverviewData moneyStatistics = new StatisticsOverviewData();

        // 2.1 今日/本周/本季/本年
        StatisticsOverviewStatisticsData moneyNow = new StatisticsOverviewStatisticsData();
        moneyNow.setAccumulate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(8000L), BigDecimal.valueOf(10000L)).setScale(2, RoundingMode.HALF_UP));
        moneyNow.setMax(RandomUtil.randomBigDecimal(BigDecimal.valueOf(4000L)).setScale(2, RoundingMode.HALF_UP));
        moneyNow.setMin(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
        moneyNow.setAverage(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L), BigDecimal.valueOf(3000L)).setScale(2, RoundingMode.HALF_UP));
        // 2.2 昨日/上周/上季/去年
        StatisticsOverviewStatisticsData moneyPrevious = new StatisticsOverviewStatisticsData();
        moneyPrevious.setAccumulate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(8000L), BigDecimal.valueOf(10000L)).setScale(2, RoundingMode.HALF_UP));
        moneyPrevious.setMax(RandomUtil.randomBigDecimal(BigDecimal.valueOf(4000L)).setScale(2, RoundingMode.HALF_UP));
        moneyPrevious.setMin(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
        moneyPrevious.setAverage(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L), BigDecimal.valueOf(3000L)).setScale(2, RoundingMode.HALF_UP));

        // 2.3 昨日/上周/上季/去年（去年同期）
        StatisticsOverviewStatisticsData moneyPreviousLastYear = new StatisticsOverviewStatisticsData();
        moneyPreviousLastYear.setAccumulate(RandomUtil.randomBigDecimal(BigDecimal.valueOf(8000L), BigDecimal.valueOf(10000L)).setScale(2, RoundingMode.HALF_UP));
        moneyPreviousLastYear.setMax(RandomUtil.randomBigDecimal(BigDecimal.valueOf(4000L)).setScale(2, RoundingMode.HALF_UP));
        moneyPreviousLastYear.setMin(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L)).setScale(2, RoundingMode.HALF_UP));
        moneyPreviousLastYear.setAverage(RandomUtil.randomBigDecimal(BigDecimal.valueOf(1000L), BigDecimal.valueOf(3000L)).setScale(2, RoundingMode.HALF_UP));

        // 2.4 同比  和去年同期比
        StatisticsOverviewStatisticsData moneyYOY = new StatisticsOverviewStatisticsData();
        moneyYOY.setAccumulate(getMOMOrYOY(moneyNow.getAccumulate(), moneyPreviousLastYear.getAccumulate()));
        moneyYOY.setMax(getMOMOrYOY(moneyNow.getMax(), moneyPreviousLastYear.getMax()));
        moneyYOY.setMin(getMOMOrYOY(moneyNow.getMin(), moneyPreviousLastYear.getMin()));
        moneyYOY.setAverage(getMOMOrYOY(moneyNow.getAverage(), moneyPreviousLastYear.getAverage()));
        // 2.5 环比 和上一个期限比
        StatisticsOverviewStatisticsData moneyMOM = new StatisticsOverviewStatisticsData();
        moneyMOM.setAccumulate(getMOMOrYOY(moneyNow.getAccumulate(), moneyPrevious.getAccumulate()));
        moneyMOM.setMax(getMOMOrYOY(moneyNow.getMax(), moneyPrevious.getMax()));
        moneyMOM.setMin(getMOMOrYOY(moneyNow.getMin(), moneyPrevious.getMin()));
        moneyMOM.setAverage(getMOMOrYOY(moneyNow.getAverage(), moneyPrevious.getAverage()));

        // 图处理
//        paramVO.setEnergyIds(energyIdList);
        StatisticsBarVO moneyBar = statisticsService.getOverallViewBar(paramVO);


        moneyStatistics.setNow(moneyNow);
        moneyStatistics.setPrevious(moneyPrevious);
        moneyStatistics.setYOY(moneyYOY);
        moneyStatistics.setMOM(moneyMOM);
        moneyStatistics.setBar(moneyBar);
        statisticsOverviewResultVO.setMoneyStatistics(moneyStatistics);

        // : 2024/12/31  改成 { list  bar}
        List<JSONObject> moneyStatisticsJsonList = entity2Json(moneyNow,
                moneyPrevious,
                moneyYOY,
                moneyMOM);

        JSONObject moneyStatisticsJson = new JSONObject();

        moneyStatisticsJson.put("list", moneyStatisticsJsonList);
        moneyStatisticsJson.put("bar", moneyBar);

        statisticsOverviewResultVO.setMoneyStatisticsJson(moneyStatisticsJson);

        statisticsOverviewResultVO.setDataUpdateTime(LocalDateTime.now());
        return statisticsOverviewResultVO;
    }

    private List<JSONObject> entity2Json(StatisticsOverviewStatisticsData now,
                                         StatisticsOverviewStatisticsData previous,
                                         StatisticsOverviewStatisticsData YOY,
                                         StatisticsOverviewStatisticsData MOM) {
        List<JSONObject> standardCoalJsonList = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            JSONObject json = new JSONObject();
            switch (i) {
                case 0:
                    json.put("item", "累计");
                    json.put("now", now.getAccumulate());
                    json.put("previous", previous.getAccumulate());
                    json.put("YOY", YOY.getAccumulate());
                    json.put("MOM", MOM.getAccumulate());
                    break;
                case 1:
                    json.put("item", "最高(Max)");
                    json.put("now", now.getMax());
                    json.put("previous", previous.getMax());
                    json.put("YOY", YOY.getMax());
                    json.put("MOM", MOM.getMax());
                    break;
                case 2:
                    json.put("item", "最低(Min)");
                    json.put("now", now.getMin());
                    json.put("previous", previous.getMin());
                    json.put("YOY", YOY.getMin());
                    json.put("MOM", MOM.getMin());
                    break;
                case 3:
                    json.put("item", "平均(Avg)");
                    json.put("now", now.getAverage());
                    json.put("previous", previous.getAverage());
                    json.put("YOY", YOY.getAverage());
                    json.put("MOM", MOM.getAverage());
                    break;
                default:
            }
            standardCoalJsonList.add(json);
        }

        return standardCoalJsonList;
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