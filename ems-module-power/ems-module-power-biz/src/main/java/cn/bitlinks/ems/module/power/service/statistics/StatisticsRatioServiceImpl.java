package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration.EnergyConfigurationDO;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.service.energyconfiguration.EnergyConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.hutool.core.util.RandomUtil;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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