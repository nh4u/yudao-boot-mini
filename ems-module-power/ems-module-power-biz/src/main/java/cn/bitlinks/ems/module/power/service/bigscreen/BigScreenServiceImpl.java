package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenRespVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.CopChartResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.vo.ReportParamVO;
import cn.bitlinks.ems.module.power.dal.mysql.bigscreen.PowerPureWasteWaterGasSettingsMapper;
import cn.bitlinks.ems.module.power.service.cophouraggdata.CopHourAggDataService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class BigScreenServiceImpl implements BigScreenService {

    @Resource
    private CopHourAggDataService copHourAggDataService;

    @Resource
    private PowerPureWasteWaterGasSettingsMapper powerPureWasteWaterGasSettingsMapper;

    @Override
    public BigScreenRespVO getBigScreenDetails(BigScreenParamReqVO paramVO) {

        BigScreenRespVO resultVO = new BigScreenRespVO();

        // 1. 中部
        // 中1 4#宿舍楼
        // 中2 2#生产厂房
        // 中3 3#办公楼
        // 中4 5#CUB
        // 中5 1#生产厂房

        // 2. 右部
        // 2.1. 右1 室外工况

        // 2.2. 右2 获取cop数据
        ReportParamVO reportParamVO = BeanUtils.toBean(paramVO, ReportParamVO.class);
        CopChartResultVO copChart = copHourAggDataService.copChartForBigScreen(reportParamVO);
        resultVO.setCopChart(copChart);

        // 2.3. 右3 纯废水单价


        // 2.4. 右4 压缩空气单价


        // 3. 底部
        // 3.1. 单位产品综合能耗


        return resultVO;
    }
}
