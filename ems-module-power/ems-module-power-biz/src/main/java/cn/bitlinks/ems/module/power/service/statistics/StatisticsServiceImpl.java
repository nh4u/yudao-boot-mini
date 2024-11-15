package cn.bitlinks.ems.module.power.service.statistics;

import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.StatisticsParamVO;
import cn.bitlinks.ems.module.power.dal.mysql.voucher.VoucherMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 用能分析 Service 实现类
 *
 * @author hero
 */
@Service
@Validated
public class StatisticsServiceImpl implements StatisticsService {

    @Resource
    private VoucherMapper voucherMapper;

    @Override
    public JSONObject energyFlowAnalysis(StatisticsParamVO paramVO) {
        return null;
    }
}